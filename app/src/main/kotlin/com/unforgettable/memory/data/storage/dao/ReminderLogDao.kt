package com.unforgettable.memory.data.storage.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.unforgettable.memory.data.storage.entity.ReminderLogEntity

@Dao
interface ReminderLogDao {
    @Insert
    suspend fun insert(log: ReminderLogEntity): Long

    @Query("SELECT COUNT(*) FROM reminder_logs WHERE taskId = :taskId AND createdAt >= :since")
    suspend fun countTaskRemindersSince(taskId: Long, since: Long): Int
}

