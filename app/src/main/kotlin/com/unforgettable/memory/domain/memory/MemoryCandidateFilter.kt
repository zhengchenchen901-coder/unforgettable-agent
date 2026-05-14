package com.unforgettable.memory.domain.memory

object MemoryCandidateFilter {
    private const val MIN_CONFIDENCE = 0.7
    private val sensitiveTerms = listOf(
        "verification",
        "verify",
        "otp",
        "password",
        "passcode",
        "token",
        "secret",
        "api key",
        "apikey",
        "coupon",
        "discount",
        "promo",
        "promotion",
        "sale",
        "unsubscribe",
        "newsletter",
    )

    fun sanitize(candidate: MemoryCandidate): SanitizedMemoryCandidate? {
        val title = candidate.title.trim()
        val content = candidate.content.trim()
        if (candidate.confidence < MIN_CONFIDENCE) return null
        if (title.isBlank() || content.isBlank()) return null

        val combined = "$title\n$content".lowercase()
        if (sensitiveTerms.any { combined.contains(it) }) return null
        if (Regex("\\b\\d{4,8}\\b").containsMatchIn(combined) && combined.contains("code")) return null

        val keywords = (candidate.keywords + MemoryKeywordExtractor.fromText("$title $content"))
            .map { it.trim().lowercase() }
            .filter { it.length >= 2 }
            .distinct()
            .take(12)

        return SanitizedMemoryCandidate(
            type = candidate.type.trim().ifBlank { "pattern" }.lowercase(),
            title = title.take(120),
            content = content.take(500),
            keywords = keywords,
            confidence = candidate.confidence.coerceIn(0.0, 1.0),
            importance = candidate.importance.coerceIn(0.0, 1.0),
            reason = candidate.reason?.trim()?.take(240),
        )
    }
}
