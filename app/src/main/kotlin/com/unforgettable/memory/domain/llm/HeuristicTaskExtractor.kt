package com.unforgettable.memory.domain.llm

import com.unforgettable.memory.data.storage.entity.TaskEntity
import com.unforgettable.memory.domain.notification.NotificationEvent
import com.unforgettable.memory.domain.notification.NotificationRules
import com.unforgettable.memory.domain.notification.SupportedApps
import com.unforgettable.memory.domain.task.TaskFingerprint
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class HeuristicTaskExtractor : TaskExtractor {
    override suspend fun extract(
        event: NotificationEvent,
        activeTasks: List<TaskEntity>,
    ): TaskExtractionResult {
        val text = event.combinedText
        if (NotificationRules.shouldIgnore(event.title, event.content)) {
            return noTask("ignored by local noise rules")
        }

        val duplicate = activeTasks.firstOrNull {
            TaskFingerprint.isDuplicate(event.content, it.content)
        }
        if (duplicate != null) {
            return TaskExtractionResult(
                isTask = true,
                task = event.content,
                deadlineLocal = null,
                deadlineText = null,
                urgency = "low",
                confidence = 0.9,
                duplicateOfTaskId = duplicate.id,
                reason = "local fingerprint matched an active task",
            )
        }

        val hasAction = NotificationRules.hasActionVerb(text)
        val hasDeadline = NotificationRules.hasDeadlineSignal(text)
        val supportedSource = SupportedApps.shouldRunAi(event.packageName)
        val confidence = listOf(
            0.45,
            if (hasAction) 0.25 else 0.0,
            if (hasDeadline) 0.18 else 0.0,
            if (supportedSource) 0.07 else 0.0,
            if (text.contains("PPT", ignoreCase = true)) 0.05 else 0.0,
        ).sum().coerceAtMost(0.96)

        if (!hasAction && !hasDeadline) {
            return noTask("no action verb or deadline signal")
        }

        val urgency = NotificationRules.urgencyFor(text)
        val deadlineText = extractDeadlineText(text)
        return TaskExtractionResult(
            isTask = confidence >= 0.65,
            task = normalizeTask(event),
            deadlineLocal = inferDeadlineLocal(text, event.timestamp),
            deadlineText = deadlineText,
            urgency = urgency,
            confidence = confidence,
            duplicateOfTaskId = null,
            reason = "local heuristic extraction",
        )
    }

    private fun normalizeTask(event: NotificationEvent): String {
        val sender = event.title.takeIf { it.isNotBlank() }
        val content = event.content.trim()
        return when {
            content.contains("PPT", ignoreCase = true) && content.contains("发") -> {
                if (sender != null) "开会前把PPT发给$sender" else "开会前发送PPT"
            }
            sender != null && content.isNotBlank() -> "$sender：$content"
            content.isNotBlank() -> content
            else -> event.combinedText
        }
    }

    private fun inferDeadlineLocal(text: String, timestamp: Long): String? {
        val zone = ZoneId.systemDefault()
        val baseDate = Instant.ofEpochMilli(timestamp).atZone(zone).toLocalDate()
        val date = when {
            text.contains("后天") -> baseDate.plusDays(2)
            text.contains("明天") -> baseDate.plusDays(1)
            text.contains("今天") || text.contains("今晚") -> baseDate
            else -> return null
        }
        val time = when {
            text.contains("开会前") -> "09:00"
            text.contains("下午") -> "15:00"
            text.contains("晚上") || text.contains("今晚") -> "20:00"
            else -> "18:00"
        }
        return "${date.format(DateTimeFormatter.ISO_LOCAL_DATE)} $time"
    }

    private fun extractDeadlineText(text: String): String? {
        return listOf("后天", "明天", "今天", "今晚", "开会前", "下午", "上午", "tomorrow", "today", "tonight")
            .firstOrNull { text.contains(it, ignoreCase = true) }
    }

    private fun noTask(reason: String): TaskExtractionResult {
        return TaskExtractionResult(
            isTask = false,
            task = null,
            deadlineLocal = null,
            deadlineText = null,
            urgency = "low",
            confidence = 0.0,
            duplicateOfTaskId = null,
            reason = reason,
        )
    }
}

