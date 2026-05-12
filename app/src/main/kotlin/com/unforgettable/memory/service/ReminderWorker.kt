package com.unforgettable.memory.service

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.unforgettable.memory.MainActivity
import com.unforgettable.memory.R
import com.unforgettable.memory.UnforgettableApp
import com.unforgettable.memory.data.storage.entity.ReminderLogEntity
import com.unforgettable.memory.domain.task.TaskStatus
import com.unforgettable.memory.reminder.NotificationChannels
import com.unforgettable.memory.ui.formatDateTime
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

class ReminderWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val taskId = inputData.getLong(KEY_TASK_ID, -1L)
        if (taskId <= 0L) return Result.failure()

        val container = (applicationContext as UnforgettableApp).container
        val database = container.database
        val task = database.taskDao().getById(taskId) ?: return Result.success()
        if (task.status != TaskStatus.PENDING) {
            database.reminderLogDao().insert(
                ReminderLogEntity(taskId = taskId, fireAt = System.currentTimeMillis(), action = "skip", result = "completed"),
            )
            return Result.success()
        }

        if (!canPostNotifications()) {
            database.reminderLogDao().insert(
                ReminderLogEntity(taskId = taskId, fireAt = System.currentTimeMillis(), action = "skip", result = "permission_missing"),
            )
            return Result.success()
        }

        showNotification(task.id, task.content, task.deadlineAt)
        database.reminderLogDao().insert(
            ReminderLogEntity(taskId = taskId, fireAt = System.currentTimeMillis(), action = "fire", result = "shown"),
        )

        val runCount = container.taskRepository.countTodayReminderRuns(taskId)
        val nextReminderAt = nextReminderAt(runCount)
        val updated = task.copy(reminderAt = nextReminderAt, updatedAt = System.currentTimeMillis())
        database.taskDao().update(updated)
        container.reminderScheduler.schedule(updated)
        return Result.success()
    }

    private fun canPostNotifications(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
    }

    private fun showNotification(taskId: Long, content: String, deadlineAt: Long?) {
        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("task_id", taskId)
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            taskId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val deadlineText = deadlineAt?.let { formatDateTime(it) } ?: "未设置"
        val body = "你还没有：\n$content\n\n截止时间：$deadlineText"

        val notification = NotificationCompat.Builder(applicationContext, NotificationChannels.REMINDER_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("你还没有：")
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        NotificationManagerCompat.from(applicationContext).notify(taskId.toInt(), notification)
    }

    private fun nextReminderAt(runCountToday: Int): Long {
        val now = System.currentTimeMillis()
        return when (runCountToday) {
            0, 1 -> now + ONE_HOUR
            2 -> now + THREE_HOURS
            else -> LocalDateTime.of(LocalDate.now().plusDays(1), LocalTime.of(9, 0))
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
        }
    }

    companion object {
        const val KEY_TASK_ID = "task_id"
        private const val ONE_HOUR = 60 * 60 * 1000L
        private const val THREE_HOURS = 3 * 60 * 60 * 1000L
    }
}

