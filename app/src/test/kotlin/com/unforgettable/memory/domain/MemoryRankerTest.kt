package com.unforgettable.memory.domain

import com.unforgettable.memory.domain.memory.MemoryContextItem
import com.unforgettable.memory.domain.memory.MemoryRanker
import com.unforgettable.memory.domain.notification.NotificationEvent
import org.junit.Assert.assertTrue
import org.junit.Test

class MemoryRankerTest {
    @Test
    fun ranksSourceAndKeywordMatchesHigher() {
        val now = 1_800_000_000_000L
        val event = NotificationEvent(
            packageName = "com.tencent.mm",
            title = "Boss",
            content = "Please send the Alpha PPT before the meeting",
            timestamp = now,
        )
        val matching = MemoryContextItem(
            id = 1,
            type = "project",
            title = "Alpha launch",
            content = "Alpha PPT requests are usually deliverable tasks.",
            keywords = listOf("alpha", "ppt"),
            sourcePackage = "com.tencent.mm",
            confidence = 0.9,
            importance = 0.8,
            evidenceCount = 3,
            lastSeenAt = now,
        )
        val unrelated = matching.copy(
            id = 2,
            title = "Billing",
            content = "Invoices are sometimes tasks.",
            keywords = listOf("invoice"),
            sourcePackage = "com.google.android.gm",
            importance = 0.2,
        )

        assertTrue(
            MemoryRanker.score(matching, event, now) >
                MemoryRanker.score(unrelated, event, now),
        )
    }
}
