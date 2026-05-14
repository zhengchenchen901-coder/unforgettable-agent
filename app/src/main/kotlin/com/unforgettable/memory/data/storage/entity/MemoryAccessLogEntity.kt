package com.unforgettable.memory.data.storage.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "memory_access_logs",
    indices = [
        Index("rawNotificationId"),
        Index("createdAt"),
    ],
)
data class MemoryAccessLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val rawNotificationId: Long,
    val requestText: String,
    val retrievedMemoryIds: List<Long>,
    val createdAt: Long = System.currentTimeMillis(),
)
