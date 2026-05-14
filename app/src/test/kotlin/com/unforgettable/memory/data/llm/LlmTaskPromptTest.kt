package com.unforgettable.memory.data.llm

import com.unforgettable.memory.domain.memory.MemoryContextItem
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LlmTaskPromptTest {
    @Test
    fun limitsMemoryContextInPrompt() {
        val memories = (1..10).map { index ->
            MemoryContextItem(
                id = index.toLong(),
                type = "pattern",
                title = "Memory #$index",
                content = "Content for memory $index",
                keywords = listOf("keyword$index"),
                sourcePackage = "com.tencent.mm",
                confidence = 0.8,
                importance = 0.6,
                evidenceCount = 1,
                lastSeenAt = 1_800_000_000_000L,
                score = 1.0,
            )
        }

        val prompt = LlmTaskPrompt.buildUserPrompt(
            eventPackageName = "com.tencent.mm",
            eventTitle = "Boss",
            eventContent = "Please send the PPT",
            eventTimestamp = 1_800_000_000_000L,
            activeTasks = emptyList(),
            memoryContext = memories,
            maxMemories = 3,
            maxMemoryChars = 1000,
        )

        assertTrue(prompt.contains("Relevant memories"))
        assertTrue(prompt.contains("Memory #3"))
        assertFalse(prompt.contains("Memory #4"))
    }
}
