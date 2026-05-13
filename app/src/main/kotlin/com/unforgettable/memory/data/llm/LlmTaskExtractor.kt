package com.unforgettable.memory.data.llm

import com.unforgettable.memory.data.storage.entity.TaskEntity
import com.unforgettable.memory.domain.llm.TaskExtractionResult
import com.unforgettable.memory.domain.llm.TaskExtractor
import com.unforgettable.memory.domain.notification.NotificationEvent
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

class LlmTaskExtractor(
    private val configStore: LlmConfigStore,
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
    ): TaskExtractionResult {
        val config = configStore.getRuntimeConfig() ?: return fallback.extract(event, activeTasks)

        val request = ChatCompletionRequest(
            model = config.modelId,
            messages = listOf(
                ChatMessage(
                    role = "system",
                    content = SYSTEM_PROMPT,
                ),
                ChatMessage(
                    role = "user",
                    content = buildUserPrompt(event, activeTasks),
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
            fallback.extract(event, activeTasks).copy(error = error.message ?: error::class.java.simpleName)
        }
    }

    private fun buildUserPrompt(
        event: NotificationEvent,
        activeTasks: List<TaskEntity>,
    ): String {
        val activeTaskSummary = activeTasks.joinToString(separator = "\n") {
            "- id=${it.id}, content=${it.content}, deadlineAt=${it.deadlineAt}, urgency=${it.urgency}"
        }.ifBlank { "none" }

        return """
            Notification:
            packageName: ${event.packageName}
            title: ${event.title}
            content: ${event.content}
            timestampMillis: ${event.timestamp}

            Active tasks for duplicate detection:
            $activeTaskSummary
        """.trimIndent()
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

    companion object {
        private val SYSTEM_PROMPT = """
            You are an AI task extraction engine.

            Analyze one Android notification and decide whether it contains an actionable task for the user.
            Extract tasks conservatively. Ignore ads, verification codes, marketing, newsletters, and casual chat.
            Use the provided active task list to detect duplicates. If it duplicates an active task, set duplicate_of_task_id.

            Return one valid JSON object only. Do not wrap it in Markdown.
            JSON schema:
            {
              "is_task": true,
              "task": "concise task content or null",
              "deadline_local": "yyyy-MM-dd HH:mm, yyyy-MM-dd, or null",
              "deadline_text": "original natural language deadline phrase or null",
              "urgency": "low | medium | high",
              "confidence": 0.0,
              "duplicate_of_task_id": null,
              "reason": "short reason"
            }

            Fields:
            - is_task: whether this notification contains an actionable task
            - task: concise task content, or null
            - deadline_local: local deadline as "yyyy-MM-dd HH:mm" when clear, "yyyy-MM-dd" when only date is clear, or null
            - deadline_text: original natural language deadline phrase, or null
            - urgency: low, medium, or high
            - confidence: 0 to 1
            - duplicate_of_task_id: active task id when duplicate, otherwise null
            - reason: short reason
        """.trimIndent()
    }
}
