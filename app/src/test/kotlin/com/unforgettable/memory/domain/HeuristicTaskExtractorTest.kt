package com.unforgettable.memory.domain

import com.unforgettable.memory.domain.llm.HeuristicTaskExtractor
import com.unforgettable.memory.domain.notification.NotificationEvent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HeuristicTaskExtractorTest {
    @Test
    fun extractsDemoScenario() = runTest {
        val result = HeuristicTaskExtractor().extract(
            event = NotificationEvent(
                packageName = "com.tencent.mm",
                title = "老板",
                content = "明天开会前把PPT发我",
                timestamp = System.currentTimeMillis(),
            ),
            activeTasks = emptyList(),
            memoryContext = emptyList(),
        )

        assertTrue(result.isTask)
        assertTrue(result.confidence > 0.8)
        assertEquals("medium", result.urgency)
        assertTrue(result.task!!.contains("PPT"))
        assertTrue(result.memoryCandidates.isNotEmpty())
    }

    @Test
    fun rejectsVerificationCode() = runTest {
        val result = HeuristicTaskExtractor().extract(
            event = NotificationEvent(
                packageName = "com.google.android.apps.messaging",
                title = "银行",
                content = "您的验证码是 123456",
                timestamp = System.currentTimeMillis(),
            ),
            activeTasks = emptyList(),
            memoryContext = emptyList(),
        )

        assertEquals(false, result.isTask)
        assertNull(result.task)
    }
}
