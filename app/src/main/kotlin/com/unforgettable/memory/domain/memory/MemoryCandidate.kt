package com.unforgettable.memory.domain.memory

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MemoryCandidate(
    val type: String = "pattern",
    val title: String = "",
    val content: String = "",
    val keywords: List<String> = emptyList(),
    val confidence: Double = 0.0,
    val importance: Double = 0.5,
    val reason: String? = null,
)

data class MemoryContextItem(
    val id: Long,
    val type: String,
    val title: String,
    val content: String,
    val keywords: List<String>,
    val sourcePackage: String?,
    val source: String = MemorySource.NOTIFICATION_INFERRED,
    val confidence: Double,
    val importance: Double,
    val evidenceCount: Int,
    val lastSeenAt: Long,
    val score: Double = 0.0,
)

data class SanitizedMemoryCandidate(
    val type: String,
    val title: String,
    val content: String,
    val keywords: List<String>,
    val confidence: Double,
    val importance: Double,
    val reason: String?,
)
