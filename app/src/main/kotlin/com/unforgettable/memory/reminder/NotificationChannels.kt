package com.unforgettable.memory.reminder

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

object NotificationChannels {
    const val REMINDER_CHANNEL_ID = "task_reminders"

    fun create(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val channel = NotificationChannel(
            REMINDER_CHANNEL_ID,
            "Task reminders",
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = "Local reminders for AI-discovered tasks"
        }

        context.getSystemService(NotificationManager::class.java)
            .createNotificationChannel(channel)
    }
}

