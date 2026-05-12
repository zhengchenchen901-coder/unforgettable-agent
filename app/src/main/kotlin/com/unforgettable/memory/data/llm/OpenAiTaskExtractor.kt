package com.unforgettable.memory.data.llm

import com.unforgettable.memory.data.storage.entity.TaskEntity
import com.unforgettable.memory.domain.llm.TaskExtractionResult
import com.unforgettable.memory.domain.llm.TaskExtractor
import com.unforgettable.memory.domain.notification.NotificationEvent
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

class OpenAiTaskExtractor(
    private val apiKeyStore: ApiKeyStore,
    private val service: OpenAIService,
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
        val apiKey = apiKeyStore.getApiKey()
        if (apiKey == null) {
            return fallback.extract(event, activeTasks)
        }

        val request = OpenAIResponseRequest(
            model = MODEL,
            input = listOf(
                OpenAIInputMessage(
                    role = "system",
                    content = listOf(OpenAIInputContent(text = SYSTEM_PROMPT)),
                ),
                OpenAIInputMessage(
                    role = "user",
                    content = listOf(OpenAIInputContent(text = buildUserPrompt(event, activeTasks))),
                ),
            ),
            text = OpenAITextConfig(
                format = OpenAIResponseFormat(
                    name = "task_extraction",
                    description = "A structured task extraction result for one Android notification.",
                    schema = extractionSchema(),
                ),
            ),
        )

        return runCatching {
            val response = service.createResponse("Bearer $apiKey", request)
            val outputText = response.output
                .asSequence()
                .flatMap { it.content.asSequence() }
                .mapNotNull { it.text }
                .firstOrNull()
                ?: error("OpenAI response did not include text output")

            json.decodeFromString<TaskExtractionResult>(outputText)
                .copy(rawJson = outputText)
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

    private fun extractionSchema(): JsonObject {
        fun nullableString(): JsonObject = buildJsonObject {
            put("type", buildJsonArray {
                add(JsonPrimitive("string"))
                add(JsonPrimitive("null"))
            })
        }

        fun nullableInteger(): JsonObject = buildJsonObject {
            put("type", buildJsonArray {
                add(JsonPrimitive("integer"))
                add(JsonPrimitive("null"))
            })
        }

        return buildJsonObject {
            put("type", "object")
            putJsonObject("properties") {
                putJsonObject("is_task") { put("type", "boolean") }
                put("task", nullableString())
                put("deadline_local", nullableString())
                put("deadline_text", nullableString())
                putJsonObject("urgency") {
                    put("type", "string")
                    put("enum", JsonArray(listOf(JsonPrimitive("low"), JsonPrimitive("medium"), JsonPrimitive("high"))))
                }
                putJsonObject("confidence") {
                    put("type", "number")
                    put("minimum", 0)
                    put("maximum", 1)
                }
                put("duplicate_of_task_id", nullableInteger())
                putJsonObject("reason") { put("type", "string") }
            }
            putJsonArray("required") {
                listOf(
                    "is_task",
                    "task",
                    "deadline_local",
                    "deadline_text",
                    "urgency",
                    "confidence",
                    "duplicate_of_task_id",
                    "reason",
                ).forEach { add(JsonPrimitive(it)) }
            }
            put("additionalProperties", false)
        }
    }

    companion object {
        private const val MODEL = "gpt-4.1-mini"

        private val SYSTEM_PROMPT = """
            You are an AI task extraction engine.

            Analyze one Android notification and decide whether it contains an actionable task for the user.
            Extract tasks conservatively. Ignore ads, verification codes, marketing, newsletters, and casual chat.
            Use the provided active task list to detect duplicates. If it duplicates an active task, set duplicate_of_task_id.

            Return valid JSON only.
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
