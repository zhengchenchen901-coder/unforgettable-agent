package com.unforgettable.memory.ui

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val dateTimeFormatter = DateTimeFormatter.ofPattern("MM-dd HH:mm")
private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

fun formatDateTime(epochMillis: Long): String {
    val dateTime = Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).toLocalDateTime()
    return dateTime.format(dateTimeFormatter)
}

fun formatReminder(epochMillis: Long?): String {
    if (epochMillis == null) return "未安排"
    val dateTime = Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).toLocalDateTime()
    val today = LocalDate.now()
    return when (dateTime.toLocalDate()) {
        today -> "今天 ${dateTime.format(timeFormatter)}"
        today.plusDays(1) -> "明天 ${dateTime.format(timeFormatter)}"
        else -> dateTime.format(dateTimeFormatter)
    }
}

