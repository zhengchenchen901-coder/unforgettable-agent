package com.unforgettable.memory.data.storage.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "ai_extraction_logs",
    indices = [Index("rawNotificationId")],
)
data class AiExtractionLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val rawNotificationId: Long,
    val requestText: String,
    val responseJson: String?,
    val finalDecision: String,
    val error: String?,
    val createdAt: Long = System.currentTimeMillis(),
)

