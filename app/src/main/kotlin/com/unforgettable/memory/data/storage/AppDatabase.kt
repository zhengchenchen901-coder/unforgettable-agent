package com.unforgettable.memory.data.storage

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.unforgettable.memory.data.storage.dao.AiExtractionLogDao
import com.unforgettable.memory.data.storage.dao.RawNotificationDao
import com.unforgettable.memory.data.storage.dao.ReminderLogDao
import com.unforgettable.memory.data.storage.dao.TaskDao
import com.unforgettable.memory.data.storage.entity.AiExtractionLogEntity
import com.unforgettable.memory.data.storage.entity.RawNotificationEntity
import com.unforgettable.memory.data.storage.entity.ReminderLogEntity
import com.unforgettable.memory.data.storage.entity.TaskEntity

@Database(
    entities = [
        RawNotificationEntity::class,
        TaskEntity::class,
        AiExtractionLogEntity::class,
        ReminderLogEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun rawNotificationDao(): RawNotificationDao
    abstract fun taskDao(): TaskDao
    abstract fun aiExtractionLogDao(): AiExtractionLogDao
    abstract fun reminderLogDao(): ReminderLogDao

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
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { instance = it }
            }
        }
    }
}
