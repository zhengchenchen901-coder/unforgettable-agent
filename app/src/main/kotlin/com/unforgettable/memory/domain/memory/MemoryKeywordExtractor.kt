package com.unforgettable.memory.domain.memory

object MemoryKeywordExtractor {
    private val stopWords = setOf(
        "the",
        "and",
        "for",
        "with",
        "from",
        "that",
        "this",
        "task",
        "please",
        "need",
        "needs",
        "remember",
    )

    fun fromText(text: String, limit: Int = 8): List<String> {
        val asciiTokens = text
            .lowercase()
            .replace(Regex("[^a-z0-9\\s_-]+"), " ")
            .split(Regex("\\s+"))
            .map { it.trim('-', '_') }
            .filter { it.length >= 3 && it !in stopWords }

        val compactText = text.trim()
            .takeIf { it.length in 2..24 && !it.any(Char::isWhitespace) }

        return (asciiTokens + listOfNotNull(compactText))
            .distinct()
            .take(limit)
    }
}
