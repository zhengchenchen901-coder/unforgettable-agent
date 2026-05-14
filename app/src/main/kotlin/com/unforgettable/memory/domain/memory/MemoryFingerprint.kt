package com.unforgettable.memory.domain.memory

object MemoryFingerprint {
    fun from(type: String, title: String, content: String): String {
        val normalized = "$type|$title|$content"
            .lowercase()
            .replace(Regex("[\\p{Punct}\\s]+"), "")
            .take(120)
        return normalized.ifBlank { "memory:${System.currentTimeMillis()}" }
    }
}
