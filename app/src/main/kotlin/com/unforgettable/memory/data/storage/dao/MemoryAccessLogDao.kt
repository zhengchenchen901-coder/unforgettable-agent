package com.unforgettable.memory.data.storage.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.unforgettable.memory.data.storage.entity.MemoryAccessLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MemoryAccessLogDao {
    @Insert
    suspend fun insert(log: MemoryAccessLogEntity): Long

    @Query("SELECT * FROM memory_access_logs ORDER BY createdAt DESC LIMIT :limit")
    fun observeRecent(limit: Int = 50): Flow<List<MemoryAccessLogEntity>>
}
