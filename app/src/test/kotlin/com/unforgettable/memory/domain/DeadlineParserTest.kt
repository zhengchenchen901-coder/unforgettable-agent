package com.unforgettable.memory.domain

import com.unforgettable.memory.domain.task.DeadlineParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class DeadlineParserTest {
    private val zone = ZoneId.of("Asia/Shanghai")
    private val now = LocalDateTime.of(2026, 5, 12, 10, 0)
        .atZone(zone)
        .toInstant()
        .toEpochMilli()

    @Test
    fun preciseDeadlineRemindsThirtyMinutesBefore() {
        val result = DeadlineParser.resolve("2026-05-12 14:00", null, "high", now, zone)
        val expectedReminder = LocalDateTime.of(2026, 5, 12, 13, 30)
            .atZone(zone)
            .toInstant()
            .toEpochMilli()
        assertEquals(expectedReminder, result.reminderAt)
    }

    @Test
    fun dateOnlyDefaultsToEveningDeadline() {
        val result = DeadlineParser.resolve("2026-05-13", null, "medium", now, zone)
        val expectedDeadline = LocalDateTime.of(2026, 5, 13, 18, 0)
            .atZone(zone)
            .toInstant()
            .toEpochMilli()
        assertEquals(expectedDeadline, result.deadlineAt)
    }

    @Test
    fun highUrgencyWithoutDeadlineStillGetsReminder() {
        val result = DeadlineParser.resolve(null, null, "high", now, zone)
        assertNotNull(result.reminderAt)
    }
}

