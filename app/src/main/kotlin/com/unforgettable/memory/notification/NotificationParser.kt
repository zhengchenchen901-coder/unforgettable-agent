package com.unforgettable.memory.notification

import android.app.Notification
import android.service.notification.StatusBarNotification
import com.unforgettable.memory.domain.notification.NotificationEvent

class NotificationParser {
    fun parse(sbn: StatusBarNotification): NotificationEvent? {
        val extras = sbn.notification.extras ?: return null
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.trim().orEmpty()
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()?.trim().orEmpty()
        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()?.trim()
        val subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString()?.trim()
        val content = listOfNotNull(bigText, text, subText)
            .firstOrNull { it.isNotBlank() }
            .orEmpty()

        if (title.isBlank() && content.isBlank()) return null

        return NotificationEvent(
            packageName = sbn.packageName,
            title = title,
            content = content,
            timestamp = sbn.postTime,
        )
    }
}

