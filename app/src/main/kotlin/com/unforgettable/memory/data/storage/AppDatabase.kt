package com.unforgettable.memory.data.storage

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.unforgettable.memory.data.storage.dao.AiExtractionLogDao
import com.unforgettable.memory.data.storage.dao.MemoryAccessLogDao
import com.unforgettable.memory.data.storage.dao.MemoryEvidenceDao
import com.unforgettable.memory.data.storage.dao.MemoryItemDao
import com.unforgettable.memory.data.storage.dao.RawNotificationDao
import com.unforgettable.memory.data.storage.dao.ReminderLogDao
import com.unforgettable.memory.data.storage.dao.TaskDao
import com.unforgettable.memory.data.storage.converter.RoomTypeConverters
import com.unforgettable.memory.data.storage.entity.AiExtractionLogEntity
import com.unforgettable.memory.data.storage.entity.MemoryAccessLogEntity
import com.unforgettable.memory.data.storage.entity.MemoryEvidenceEntity
import com.unforgettable.memory.data.storage.entity.MemoryItemEntity
import com.unforgettable.memory.data.storage.entity.RawNotificationEntity
import com.unforgettable.memory.data.storage.entity.ReminderLogEntity
import com.unforgettable.memory.data.storage.entity.TaskEntity

@Database(
    entities = [
        RawNotificationEntity::class,
        TaskEntity::class,
        AiExtractionLogEntity::class,
        ReminderLogEntity::class,
        MemoryItemEntity::class,
        MemoryEvidenceEntity::class,
        MemoryAccessLogEntity::class,
    ],
    version = 3,
    exportSchema = false,
)
@TypeConverters(RoomTypeConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun rawNotificationDao(): RawNotificationDao
    abstract fun taskDao(): TaskDao
    abstract fun aiExtractionLogDao(): AiExtractionLogDao
    abstract fun reminderLogDao(): ReminderLogDao
    abstract fun memoryItemDao(): MemoryItemDao
    abstract fun memoryEvidenceDao(): MemoryEvidenceDao
    abstract fun memoryAccessLogDao(): MemoryAccessLogDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "unforgettable.db",
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build()
                    .also { instance = it }
            }
        }
    }
}
