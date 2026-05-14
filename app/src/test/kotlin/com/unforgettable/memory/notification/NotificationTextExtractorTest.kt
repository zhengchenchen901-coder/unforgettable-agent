package com.unforgettable.memory.notification

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NotificationTextExtractorTest {
    @Test
    fun usesMessagingStyleMessagesWhenTextIsMissing() {
        val parsed = NotificationTextExtractor.parse(
            title = "WeChat",
            titleBig = null,
            conversationTitle = null,
            text = null,
            bigText = null,
            textLines = null,
            messages = listOf("Confirm the release plan before 8 tonight"),
            summaryText = null,
            subText = null,
            infoText = null,
        )

        assertEquals("WeChat", parsed?.title)
        assertEquals("Confirm the release plan before 8 tonight", parsed?.content)
    }

    @Test
    fun joinsInboxTextLinesWhenSingleTextIsMissing() {
        val parsed = NotificationTextExtractor.parse(
            title = "Wechat group",
            titleBig = null,
            conversationTitle = null,
            text = null,
            bigText = null,
            textLines = arrayOf("Alex: send the deck tonight", "Maya: confirm before 8"),
            messages = emptyList(),
            summaryText = null,
            subText = null,
            infoText = null,
        )

        assertEquals("Wechat group", parsed?.title)
        assertEquals("Alex: send the deck tonight\nMaya: confirm before 8", parsed?.content)
    }

    @Test
    fun returnsNullWhenEveryFieldIsBlank() {
        val parsed = NotificationTextExtractor.parse(
            title = " ",
            titleBig = null,
            conversationTitle = null,
            text = null,
            bigText = " ",
            textLines = emptyArray(),
            messages = emptyList(),
            summaryText = null,
            subText = null,
            infoText = null,
        )

        assertNull(parsed)
    }
}
