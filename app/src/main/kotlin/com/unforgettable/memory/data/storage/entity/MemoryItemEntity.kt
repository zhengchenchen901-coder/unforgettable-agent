package com.unforgettable.memory.data.storage.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.unforgettable.memory.domain.memory.MemoryStatus

@Entity(
    tableName = "memory_items",
    indices = [
        Index(value = ["fingerprint"], unique = true),
        Index("status"),
        Index("type"),
        Index("sourcePackage"),
        Index("source"),
        Index("lastSeenAt"),
    ],
)
data class MemoryItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val type: String,
    val title: String,
    val content: String,
    val keywords: List<String>,
    val sourcePackage: String?,
    val source: String,
    val confidence: Double,
    val importance: Double,
    val status: String = MemoryStatus.ACTIVE,
    val fingerprint: String,
    val evidenceCount: Int = 0,
    val lastSeenAt: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)
