package com.unforgettable.memory.reminder

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

object NotificationChannels {
    const val REMINDER_CHANNEL_ID = "task_reminders"
    const val DEBUG_SEED_CHANNEL_ID = "debug_notification_seeds"

    fun create(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val reminderChannel = NotificationChannel(
            REMINDER_CHANNEL_ID,
            "Task reminders",
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = "Local reminders for AI-discovered tasks"
        }

        val debugSeedChannel = NotificationChannel(
            DEBUG_SEED_CHANNEL_ID,
            "Debug notification seeds",
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = "Visible realistic notifications generated from the Debug screen"
        }

        val notificationManager = context.getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(reminderChannel)
        notificationManager.createNotificationChannel(debugSeedChannel)
    }
}
