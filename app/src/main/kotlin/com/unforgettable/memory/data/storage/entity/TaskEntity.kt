package com.unforgettable.memory.data.storage.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.unforgettable.memory.domain.task.TaskStatus

@Entity(
    tableName = "tasks",
    indices = [
        Index("fingerprint"),
        Index("status"),
        Index("sourceRawNotificationId"),
    ],
)
data class TaskEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val content: String,
    val deadlineAt: Long?,
    val reminderAt: Long?,
    val sourcePackage: String,
    val sourceRawNotificationId: Long?,
    val status: String = TaskStatus.PENDING,
    val confidence: Double,
    val urgency: String,
    val fingerprint: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)

