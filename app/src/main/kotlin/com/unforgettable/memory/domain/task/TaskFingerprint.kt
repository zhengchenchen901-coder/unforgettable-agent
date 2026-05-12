package com.unforgettable.memory.domain.task

object TaskFingerprint {
    fun from(text: String): String {
        return text
            .lowercase()
            .replace(Regex("[\\p{Punct}\\s]+"), "")
            .replace(Regex("(今天|明天|后天|今晚|上午|下午|晚上|之前|前|记得|别忘了|别忘|please|pls|asap|urgent)"), "")
            .take(80)
    }

    fun isDuplicate(candidate: String, existing: String): Boolean {
        val normalizedCandidate = from(candidate)
        val normalizedExisting = from(existing)
        if (normalizedCandidate.isBlank() || normalizedExisting.isBlank()) return false
        val overlap = normalizedCandidate.toSet()
            .intersect(normalizedExisting.toSet())
            .size
            .toDouble() / maxOf(normalizedCandidate.toSet().size, normalizedExisting.toSet().size)
        return normalizedCandidate == normalizedExisting ||
            normalizedCandidate.contains(normalizedExisting) ||
            normalizedExisting.contains(normalizedCandidate) ||
            overlap >= 0.75
    }
}
