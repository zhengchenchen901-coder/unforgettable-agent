package com.unforgettable.memory.data.storage.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.unforgettable.memory.data.storage.entity.MemoryItemEntity
import com.unforgettable.memory.domain.memory.MemoryStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface MemoryItemDao {
    @Insert
    suspend fun insert(item: MemoryItemEntity): Long

    @Update
    suspend fun update(item: MemoryItemEntity)

    @Query("SELECT * FROM memory_items WHERE id = :id")
    suspend fun getById(id: Long): MemoryItemEntity?

    @Query("SELECT * FROM memory_items WHERE fingerprint = :fingerprint LIMIT 1")
    suspend fun findByFingerprint(fingerprint: String): MemoryItemEntity?

    @Query("SELECT * FROM memory_items WHERE status = '${MemoryStatus.ACTIVE}' ORDER BY updatedAt DESC LIMIT :limit")
    suspend fun getActiveSnapshot(limit: Int = 200): List<MemoryItemEntity>

    @Query("SELECT * FROM memory_items ORDER BY updatedAt DESC LIMIT :limit")
    fun observeRecent(limit: Int = 100): Flow<List<MemoryItemEntity>>

    @Query("UPDATE memory_items SET status = :status, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateStatus(id: Long, status: String, updatedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM memory_items WHERE id = :id")
    suspend fun deleteById(id: Long)
}
