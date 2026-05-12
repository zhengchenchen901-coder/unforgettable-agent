package com.unforgettable.memory.data.storage.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.unforgettable.memory.data.storage.entity.TaskEntity
import com.unforgettable.memory.domain.task.TaskStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Insert
    suspend fun insert(task: TaskEntity): Long

    @Update
    suspend fun update(task: TaskEntity)

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getById(id: Long): TaskEntity?

    @Query("SELECT * FROM tasks ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE status = '${TaskStatus.PENDING}' ORDER BY COALESCE(deadlineAt, 32503680000000), createdAt DESC")
    fun observePending(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE status = '${TaskStatus.COMPLETED}' ORDER BY updatedAt DESC")
    fun observeCompleted(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE status = '${TaskStatus.PENDING}' ORDER BY createdAt DESC LIMIT :limit")
    suspend fun getActiveTasksSnapshot(limit: Int = 30): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE status = :status ORDER BY updatedAt DESC")
    suspend fun getTasksByStatus(status: String): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE status = '${TaskStatus.PENDING}' AND fingerprint = :fingerprint LIMIT 1")
    suspend fun findPendingByFingerprint(fingerprint: String): TaskEntity?

    @Query("SELECT COUNT(*) FROM tasks")
    fun observeTaskCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM tasks WHERE status = '${TaskStatus.COMPLETED}'")
    fun observeCompletedCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM tasks WHERE reminderAt IS NOT NULL")
    fun observeReminderCount(): Flow<Int>
}
