package com.unforgettable.memory.domain.task

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

data class DeadlineResolution(
    val deadlineAt: Long?,
    val reminderAt: Long?,
)

object DeadlineParser {
    private val dateTimeFormats = listOf(
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"),
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
        DateTimeFormatter.ISO_LOCAL_DATE_TIME,
    )

    fun resolve(
        deadlineLocal: String?,
        deadlineText: String?,
        urgency: String,
        nowMillis: Long = System.currentTimeMillis(),
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): DeadlineResolution {
        parseStructured(deadlineLocal, nowMillis, zoneId)?.let { return it }
        parseText(deadlineText.orEmpty(), nowMillis, zoneId)?.let { return it }

        return if (urgency == "high") {
            DeadlineResolution(
                deadlineAt = null,
                reminderAt = nowMillis + ONE_HOUR,
            )
        } else {
            DeadlineResolution(deadlineAt = null, reminderAt = null)
        }
    }

    private fun parseStructured(
        raw: String?,
        nowMillis: Long,
        zoneId: ZoneId,
    ): DeadlineResolution? {
        val value = raw?.trim().orEmpty()
        if (value.isBlank() || value.equals("null", ignoreCase = true)) return null

        dateTimeFormats.forEach { formatter ->
            try {
                val dateTime = LocalDateTime.parse(value, formatter)
                return withPreciseTime(dateTime, nowMillis, zoneId)
            } catch (_: DateTimeParseException) {
            }
        }

        return try {
            val date = LocalDate.parse(value, DateTimeFormatter.ISO_LOCAL_DATE)
            withDateOnly(date, nowMillis, zoneId)
        } catch (_: DateTimeParseException) {
            null
        }
    }

    private fun parseText(
        text: String,
        nowMillis: Long,
        zoneId: ZoneId,
    ): DeadlineResolution? {
        if (text.isBlank()) return null
        val now = Instant.ofEpochMilli(nowMillis).atZone(zoneId).toLocalDateTime()
        val lower = text.lowercase()
        val date = when {
            lower.contains("后天") -> now.toLocalDate().plusDays(2)
            lower.contains("明天") || lower.contains("tomorrow") -> now.toLocalDate().plusDays(1)
            lower.contains("今天") || lower.contains("今晚") || lower.contains("today") || lower.contains("tonight") -> now.toLocalDate()
            else -> null
        }

        val time = inferTime(lower)
        return when {
            date != null && time != null -> withPreciseTime(LocalDateTime.of(date, time), nowMillis, zoneId)
            date != null -> withDateOnly(date, nowMillis, zoneId)
            time != null -> {
                val dateTime = LocalDateTime.of(now.toLocalDate(), time)
                withPreciseTime(
                    if (dateTime.isBefore(now)) dateTime.plusDays(1) else dateTime,
                    nowMillis,
                    zoneId,
                )
            }
            else -> null
        }
    }

    private fun inferTime(text: String): LocalTime? {
        val hourMatch = Regex("(\\d{1,2})(?:[:点时])(\\d{1,2})?").find(text)
        if (hourMatch != null) {
            val hour = hourMatch.groupValues[1].toIntOrNull()
            val minute = hourMatch.groupValues.getOrNull(2)?.toIntOrNull() ?: 0
            if (hour != null && hour in 0..23 && minute in 0..59) return LocalTime.of(hour, minute)
        }

        return when {
            text.contains("开会前") -> LocalTime.of(9, 0)
            text.contains("早上") || text.contains("上午") -> LocalTime.of(9, 0)
            text.contains("中午") -> LocalTime.of(12, 0)
            text.contains("下午") -> LocalTime.of(15, 0)
            text.contains("晚上") || text.contains("今晚") || text.contains("tonight") -> LocalTime.of(20, 0)
            else -> null
        }
    }

    private fun withPreciseTime(
        dateTime: LocalDateTime,
        nowMillis: Long,
        zoneId: ZoneId,
    ): DeadlineResolution {
        val deadlineAt = dateTime.atZone(zoneId).toInstant().toEpochMilli()
        val defaultReminder = deadlineAt - THIRTY_MINUTES
        return DeadlineResolution(
            deadlineAt = deadlineAt,
            reminderAt = maxOf(defaultReminder, nowMillis + FIVE_MINUTES),
        )
    }

    private fun withDateOnly(
        date: LocalDate,
        nowMillis: Long,
        zoneId: ZoneId,
    ): DeadlineResolution {
        val deadlineAt = LocalDateTime.of(date, LocalTime.of(18, 0))
            .atZone(zoneId)
            .toInstant()
            .toEpochMilli()
        val reminderAt = LocalDateTime.of(date, LocalTime.of(9, 0))
            .atZone(zoneId)
            .toInstant()
            .toEpochMilli()
        return DeadlineResolution(
            deadlineAt = deadlineAt,
            reminderAt = maxOf(reminderAt, nowMillis + FIVE_MINUTES),
        )
    }

    private const val FIVE_MINUTES = 5 * 60 * 1000L
    private const val THIRTY_MINUTES = 30 * 60 * 1000L
    private const val ONE_HOUR = 60 * 60 * 1000L
}

