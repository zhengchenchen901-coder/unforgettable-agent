package com.unforgettable.memory.domain.memory

import com.unforgettable.memory.domain.notification.NotificationEvent
import java.util.concurrent.TimeUnit

object MemoryRanker {
    fun score(
        memory: MemoryContextItem,
        event: NotificationEvent,
        now: Long = System.currentTimeMillis(),
    ): Double {
        val text = event.combinedText.lowercase()
        val keywordHits = memory.keywords.count { keyword ->
            keyword.isNotBlank() && text.contains(keyword.lowercase())
        }
        val keywordScore = if (memory.keywords.isEmpty()) {
            0.0
        } else {
            (keywordHits.toDouble() / memory.keywords.size).coerceAtMost(1.0)
        }
        val sourcePackageScore = if (memory.sourcePackage == event.packageName) 1.0 else 0.0
        val recencyDays = ((now - memory.lastSeenAt).coerceAtLeast(0L)).toDouble() /
            TimeUnit.DAYS.toMillis(1).toDouble()
        val recencyScore = 1.0 / (1.0 + recencyDays / 14.0)
        val sourceReliabilityScore = when (memory.source) {
            MemorySource.USER_INPUT -> 1.0
            MemorySource.USER_CONFIRMED -> 0.9
            MemorySource.TASK_BEHAVIOR -> 0.75
            else -> 0.45
        }

        return (keywordScore * 0.45) +
            (sourcePackageScore * 0.15) +
            (sourceReliabilityScore * 0.12) +
            (memory.importance.coerceIn(0.0, 1.0) * 0.18) +
            (memory.confidence.coerceIn(0.0, 1.0) * 0.1) +
            (recencyScore * 0.05)
    }
}
