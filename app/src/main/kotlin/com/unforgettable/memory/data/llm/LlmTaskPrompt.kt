package com.unforgettable.memory.data.llm

import com.unforgettable.memory.data.storage.entity.TaskEntity
import com.unforgettable.memory.domain.memory.MemoryContextItem

object LlmTaskPrompt {
    val SYSTEM_PROMPT = """
        You are an AI task extraction engine with local-first memory support.

        Analyze one Android notification and decide whether it contains an actionable task for the user.
        Extract tasks conservatively. Ignore ads, verification codes, marketing, newsletters, and casual chat.
        Use the provided active task list to detect duplicates. If it duplicates an active task, set duplicate_of_task_id.
        Use relevant memories only as context; never invent details that are not supported by the notification or memory.

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
          "reason": "short reason",
          "memory_candidates": [
            {
              "type": "person | project | preference | pattern",
              "title": "stable memory title",
              "content": "stable fact useful for future task extraction",
              "keywords": ["short", "retrieval", "terms"],
              "confidence": 0.0,
              "importance": 0.0,
              "reason": "why this should be remembered"
            }
          ]
        }

        Memory candidate rules:
        - Add candidates only for stable facts that can improve future task extraction, duplicate detection, or reminders.
        - Prefer people, project names, recurring deliverables, and user preferences.
        - Do not store verification codes, passwords, tokens, API keys, IDs, marketing, one-off small talk, or sensitive identifiers.
        - Keep memory_candidates empty when there is nothing stable to remember.
    """.trimIndent()

    fun buildUserPrompt(
        eventPackageName: String,
        eventTitle: String,
        eventContent: String,
        eventTimestamp: Long,
        activeTasks: List<TaskEntity>,
        memoryContext: List<MemoryContextItem>,
        maxMemories: Int = 8,
        maxMemoryChars: Int = 1600,
    ): String {
        val activeTaskSummary = activeTasks.joinToString(separator = "\n") {
            "- id=${it.id}, content=${it.content}, deadlineAt=${it.deadlineAt}, urgency=${it.urgency}"
        }.ifBlank { "none" }

        val memorySummary = buildMemorySummary(memoryContext, maxMemories, maxMemoryChars)

        return """
            Notification:
            packageName: $eventPackageName
            title: $eventTitle
            content: $eventContent
            timestampMillis: $eventTimestamp

            Active tasks for duplicate detection:
            $activeTaskSummary

            Relevant memories:
            $memorySummary
        """.trimIndent()
    }

    private fun buildMemorySummary(
        memoryContext: List<MemoryContextItem>,
        maxMemories: Int,
        maxMemoryChars: Int,
    ): String {
        val lines = memoryContext.take(maxMemories).map { memory ->
            "- id=${memory.id}, type=${memory.type}, title=${memory.title}, content=${memory.content}, " +
                "source=${memory.source}, keywords=${memory.keywords.joinToString("|")}, importance=${memory.importance}, " +
                "confidence=${memory.confidence}, score=${"%.3f".format(memory.score)}"
        }
        val summary = lines.joinToString(separator = "\n").ifBlank { "none" }
        return if (summary.length <= maxMemoryChars) summary else summary.take(maxMemoryChars)
    }
}
