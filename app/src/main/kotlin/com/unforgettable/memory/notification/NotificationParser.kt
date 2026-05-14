package com.unforgettable.memory.notification

import android.app.Notification
import android.os.Bundle
import android.service.notification.StatusBarNotification
import com.unforgettable.memory.domain.notification.NotificationEvent

class NotificationParser {
    fun parse(sbn: StatusBarNotification): NotificationEvent? {
        val extras = sbn.notification.extras ?: return null
        val parsed = NotificationTextExtractor.parse(
            title = extras.getCharSequence(Notification.EXTRA_TITLE),
            titleBig = extras.getCharSequence(Notification.EXTRA_TITLE_BIG),
            conversationTitle = extras.getCharSequence(Notification.EXTRA_CONVERSATION_TITLE),
            text = extras.getCharSequence(Notification.EXTRA_TEXT),
            bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT),
            textLines = extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES),
            messages = extras.messagingStyleTexts(),
            summaryText = extras.getCharSequence(Notification.EXTRA_SUMMARY_TEXT),
            subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT),
            infoText = extras.getCharSequence(Notification.EXTRA_INFO_TEXT),
        ) ?: return null

        return NotificationEvent(
            packageName = sbn.packageName,
            title = parsed.title,
            content = parsed.content,
            timestamp = sbn.postTime,
        )
    }

    @Suppress("DEPRECATION")
    private fun Bundle.messagingStyleTexts(): List<CharSequence> {
        return getParcelableArray(Notification.EXTRA_MESSAGES)
            ?.mapNotNull { item ->
                (item as? Bundle)?.getCharSequence(MESSAGING_STYLE_TEXT_KEY)
            }
            .orEmpty()
    }

    private companion object {
        private const val MESSAGING_STYLE_TEXT_KEY = "text"
    }
}

internal data class ParsedNotificationText(
    val title: String,
    val content: String,
)

internal object NotificationTextExtractor {
    fun parse(
        title: CharSequence?,
        titleBig: CharSequence?,
        conversationTitle: CharSequence?,
        text: CharSequence?,
        bigText: CharSequence?,
        textLines: Array<CharSequence>?,
        messages: List<CharSequence>,
        summaryText: CharSequence?,
        subText: CharSequence?,
        infoText: CharSequence?,
    ): ParsedNotificationText? {
        val cleanTitle = listOf(title, conversationTitle, titleBig, subText)
            .firstNonBlank()
        val cleanContent = listOf(
            bigText.clean(),
            messages.joinNonBlank(),
            textLines.orEmpty().toList().joinNonBlank(),
            text.clean(),
            summaryText.clean(),
            subText.clean(),
            infoText.clean(),
        ).firstOrNull { it.isNotBlank() }.orEmpty()

        if (cleanTitle.isBlank() && cleanContent.isBlank()) return null

        return ParsedNotificationText(
            title = cleanTitle,
            content = cleanContent,
        )
    }

    private fun List<CharSequence?>.firstNonBlank(): String {
        return firstNotNullOfOrNull { it.clean().takeIf(String::isNotBlank) }.orEmpty()
    }

    private fun List<CharSequence>.joinNonBlank(): String {
        return mapNotNull { it.clean().takeIf(String::isNotBlank) }
            .distinct()
            .joinToString(separator = "\n")
    }

    private fun CharSequence?.clean(): String {
        return this?.toString()?.trim().orEmpty()
    }
}
