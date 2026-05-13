package com.unforgettable.memory.data.storage.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.unforgettable.memory.data.storage.entity.RawNotificationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RawNotificationDao {
    @Insert
    suspend fun insert(notification: RawNotificationEntity): Long

    @Query(
        """
        SELECT id FROM raw_notifications
        WHERE packageName = :packageName
            AND title = :title
            AND content = :content
            AND timestamp = :timestamp
        LIMIT 1
        """,
    )
    suspend fun findExistingId(
        packageName: String,
        title: String,
        content: String,
        timestamp: Long,
    ): Long?

    @Query("SELECT * FROM raw_notifications WHERE id = :id")
    suspend fun getById(id: Long): RawNotificationEntity?

    @Query("SELECT * FROM raw_notifications ORDER BY timestamp DESC LIMIT :limit")
    fun observeRecent(limit: Int = 100): Flow<List<RawNotificationEntity>>
}
