package com.unforgettable.memory.data.storage.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.unforgettable.memory.data.storage.entity.MemoryEvidenceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MemoryEvidenceDao {
    @Insert
    suspend fun insert(evidence: MemoryEvidenceEntity): Long

    @Query("SELECT * FROM memory_evidence WHERE memoryItemId = :memoryItemId ORDER BY createdAt DESC LIMIT :limit")
    fun observeForMemory(memoryItemId: Long, limit: Int = 20): Flow<List<MemoryEvidenceEntity>>

    @Query("DELETE FROM memory_evidence WHERE memoryItemId = :memoryItemId")
    suspend fun deleteForMemory(memoryItemId: Long)
}
