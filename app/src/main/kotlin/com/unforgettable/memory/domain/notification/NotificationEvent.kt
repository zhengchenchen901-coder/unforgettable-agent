package com.unforgettable.memory.domain.notification

data class NotificationEvent(
    val packageName: String,
    val title: String,
    val content: String,
    val timestamp: Long,
) {
    val combinedText: String
        get() = listOf(title, content)
            .filter { it.isNotBlank() }
            .joinToString(separator = "\n")
}

