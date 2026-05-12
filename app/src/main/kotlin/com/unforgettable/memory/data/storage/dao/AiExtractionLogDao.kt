package com.unforgettable.memory.data.storage.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.unforgettable.memory.data.storage.entity.AiExtractionLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AiExtractionLogDao {
    @Insert
    suspend fun insert(log: AiExtractionLogEntity): Long

    @Query("SELECT * FROM ai_extraction_logs ORDER BY createdAt DESC LIMIT :limit")
    fun observeRecent(limit: Int = 100): Flow<List<AiExtractionLogEntity>>
}

