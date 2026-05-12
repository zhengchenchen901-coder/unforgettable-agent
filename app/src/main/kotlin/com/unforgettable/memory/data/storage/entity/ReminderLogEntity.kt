package com.unforgettable.memory.data.storage.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "reminder_logs",
    indices = [Index("taskId")],
)
data class ReminderLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val taskId: Long,
    val fireAt: Long,
    val action: String,
    val result: String,
    val createdAt: Long = System.currentTimeMillis(),
)

