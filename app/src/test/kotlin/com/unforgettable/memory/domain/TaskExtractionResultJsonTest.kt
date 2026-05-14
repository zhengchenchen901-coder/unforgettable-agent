package com.unforgettable.memory.domain

import com.unforgettable.memory.domain.llm.TaskExtractionResult
import kotlinx.serialization.json.Json
import org.junit.Assert.assertTrue
import org.junit.Test

class TaskExtractionResultJsonTest {
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    @Test
    fun decodesLegacyJsonWithoutMemoryCandidates() {
        val result = json.decodeFromString<TaskExtractionResult>(
            """
            {
              "is_task": true,
              "task": "Send the deck",
              "deadline_local": null,
              "deadline_text": null,
              "urgency": "medium",
              "confidence": 0.91,
              "duplicate_of_task_id": null,
              "reason": "actionable request"
            }
            """.trimIndent(),
        )

        assertTrue(result.memoryCandidates.isEmpty())
    }
}
