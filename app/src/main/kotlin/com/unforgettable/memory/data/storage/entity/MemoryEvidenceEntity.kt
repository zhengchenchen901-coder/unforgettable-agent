package com.unforgettable.memory.data.storage.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "memory_evidence",
    foreignKeys = [
        ForeignKey(
            entity = MemoryItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["memoryItemId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index("memoryItemId"),
        Index("rawNotificationId"),
        Index("taskId"),
    ],
)
data class MemoryEvidenceEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val memoryItemId: Long,
    val rawNotificationId: Long?,
    val taskId: Long?,
    val sourceText: String,
    val reason: String?,
    val createdAt: Long = System.currentTimeMillis(),
)
