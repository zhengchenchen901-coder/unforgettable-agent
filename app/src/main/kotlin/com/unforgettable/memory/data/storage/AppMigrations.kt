package com.unforgettable.memory.data.storage

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `memory_items` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `type` TEXT NOT NULL,
                `title` TEXT NOT NULL,
                `content` TEXT NOT NULL,
                `keywords` TEXT NOT NULL,
                `sourcePackage` TEXT,
                `confidence` REAL NOT NULL,
                `importance` REAL NOT NULL,
                `status` TEXT NOT NULL,
                `fingerprint` TEXT NOT NULL,
                `evidenceCount` INTEGER NOT NULL,
                `lastSeenAt` INTEGER NOT NULL,
                `createdAt` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL
            )
            """.trimIndent(),
        )
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_memory_items_fingerprint` ON `memory_items` (`fingerprint`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_memory_items_status` ON `memory_items` (`status`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_memory_items_type` ON `memory_items` (`type`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_memory_items_sourcePackage` ON `memory_items` (`sourcePackage`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_memory_items_lastSeenAt` ON `memory_items` (`lastSeenAt`)")

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `memory_evidence` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `memoryItemId` INTEGER NOT NULL,
                `rawNotificationId` INTEGER,
                `taskId` INTEGER,
                `sourceText` TEXT NOT NULL,
                `reason` TEXT,
                `createdAt` INTEGER NOT NULL,
                FOREIGN KEY(`memoryItemId`) REFERENCES `memory_items`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent(),
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_memory_evidence_memoryItemId` ON `memory_evidence` (`memoryItemId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_memory_evidence_rawNotificationId` ON `memory_evidence` (`rawNotificationId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_memory_evidence_taskId` ON `memory_evidence` (`taskId`)")

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `memory_access_logs` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `rawNotificationId` INTEGER NOT NULL,
                `requestText` TEXT NOT NULL,
                `retrievedMemoryIds` TEXT NOT NULL,
                `createdAt` INTEGER NOT NULL
            )
            """.trimIndent(),
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_memory_access_logs_rawNotificationId` ON `memory_access_logs` (`rawNotificationId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_memory_access_logs_createdAt` ON `memory_access_logs` (`createdAt`)")
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `memory_items` ADD COLUMN `source` TEXT NOT NULL DEFAULT 'notification_inferred'")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_memory_items_source` ON `memory_items` (`source`)")
        db.execSQL(
            """
            UPDATE `memory_items`
            SET `status` = 'pending', `updatedAt` = strftime('%s','now') * 1000
            WHERE `status` = 'active' AND `source` = 'notification_inferred'
            """.trimIndent(),
        )
    }
}
