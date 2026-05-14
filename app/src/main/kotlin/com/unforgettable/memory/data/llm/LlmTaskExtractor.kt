package com.unforgettable.memory.data.llm

import com.unforgettable.memory.data.storage.entity.TaskEntity
import com.unforgettable.memory.domain.memory.MemoryContextItem
import com.unforgettable.memory.domain.llm.TaskExtractionResult
import com.unforgettable.memory.domain.llm.TaskExtractor
import com.unforgettable.memory.domain.notification.NotificationEvent
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

class LlmTaskExtractor(
    private val configStore: LlmRuntimeConfigProvider,
    private val service: LlmChatService,
    private val fallback: TaskExtractor,
) : TaskExtractor {
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    override suspend fun extract(
        event: NotificationEvent,
        activeTasks: List<TaskEntity>,
        memoryContext: List<MemoryContextItem>,
    ): TaskExtractionResult {
        val config = configStore.getRuntimeConfig() ?: return missingApiKeyResult()

        val request = ChatCompletionRequest(
            model = config.modelId,
            messages = listOf(
                ChatMessage(
                    role = "system",
                    content = LlmTaskPrompt.SYSTEM_PROMPT,
                ),
                ChatMessage(
                    role = "user",
                    content = LlmTaskPrompt.buildUserPrompt(
                        eventPackageName = event.packageName,
                        eventTitle = event.title,
                        eventContent = event.content,
                        eventTimestamp = event.timestamp,
                        activeTasks = activeTasks,
                        memoryContext = memoryContext,
                    ),
                ),
            ),
            responseFormat = if (config.provider.supportsJsonMode) ChatResponseFormat() else null,
        )

        return runCatching {
            val response = service.createChatCompletion(
                url = config.provider.chatCompletionsUrl,
                authorization = "Bearer ${config.apiKey}",
                request = request,
            )
            val outputText = response.choices
                .asSequence()
                .mapNotNull { it.message?.content }
                .firstOrNull()
                ?: error("${config.provider.displayName} response did not include text output")
            val jsonText = normalizeJsonOutput(outputText)

            json.decodeFromString<TaskExtractionResult>(jsonText)
                .copy(rawJson = jsonText)
        }.getOrElse { error ->
            fallback.extract(event, activeTasks, memoryContext).copy(error = error.message ?: error::class.java.simpleName)
        }
    }

    private fun normalizeJsonOutput(outputText: String): String {
        val trimmed = outputText.trim()
        if (runCatching { json.parseToJsonElement(trimmed) is JsonObject }.getOrDefault(false)) {
            return trimmed
        }

        val start = trimmed.indexOf('{')
        val end = trimmed.lastIndexOf('}')
        if (start >= 0 && end > start) {
            val candidate = trimmed.substring(start, end + 1)
            if (runCatching { json.parseToJsonElement(candidate) is JsonObject }.getOrDefault(false)) {
                return candidate
            }
        }

        return trimmed
    }

    private fun missingApiKeyResult(): TaskExtractionResult {
        return TaskExtractionResult(
            isTask = false,
            task = null,
            deadlineLocal = null,
            deadlineText = null,
            urgency = "low",
            confidence = 0.0,
            duplicateOfTaskId = null,
            reason = "missing LLM API key",
            error = ERROR_MISSING_API_KEY,
        )
    }

    companion object {
        const val ERROR_MISSING_API_KEY = "missing_api_key"
    }
}
