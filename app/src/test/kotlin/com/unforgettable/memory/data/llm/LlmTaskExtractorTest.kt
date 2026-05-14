package com.unforgettable.memory.data.llm

import com.unforgettable.memory.data.storage.entity.TaskEntity
import com.unforgettable.memory.domain.llm.TaskExtractionResult
import com.unforgettable.memory.domain.llm.TaskExtractor
import com.unforgettable.memory.domain.memory.MemoryContextItem
import com.unforgettable.memory.domain.notification.NotificationEvent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class LlmTaskExtractorTest {
    @Test
    fun missingApiKeySkipsExtractionWithoutFallback() = runTest {
        var fallbackCalls = 0
        val extractor = LlmTaskExtractor(
            configStore = object : LlmRuntimeConfigProvider {
                override fun getRuntimeConfig(): LlmRuntimeConfig? = null
            },
            service = object : LlmChatService {
                override suspend fun createChatCompletion(
                    url: String,
                    authorization: String,
                    request: ChatCompletionRequest,
                ): ChatCompletionResponse {
                    error("service should not be called without an API key")
                }
            },
            fallback = object : TaskExtractor {
                override suspend fun extract(
                    event: NotificationEvent,
                    activeTasks: List<TaskEntity>,
                    memoryContext: List<MemoryContextItem>,
                ): TaskExtractionResult {
                    fallbackCalls += 1
                    return TaskExtractionResult(
                        isTask = true,
                        task = "fallback task",
                        deadlineLocal = null,
                        deadlineText = null,
                        urgency = "medium",
                        confidence = 0.99,
                        duplicateOfTaskId = null,
                        reason = "fallback",
                    )
                }
            },
        )

        val result = extractor.extract(
            event = NotificationEvent(
                packageName = "com.tencent.mm",
                title = "老板",
                content = "明天开会前把PPT发我",
                timestamp = 1_800_000_000_000L,
            ),
            activeTasks = emptyList(),
            memoryContext = emptyList(),
        )

        assertFalse(result.isTask)
        assertNull(result.task)
        assertEquals(LlmTaskExtractor.ERROR_MISSING_API_KEY, result.error)
        assertEquals(0, fallbackCalls)
    }
}
