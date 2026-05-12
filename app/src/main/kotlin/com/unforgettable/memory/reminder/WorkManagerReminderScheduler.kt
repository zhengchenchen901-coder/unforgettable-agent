package com.unforgettable.memory.reminder

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.unforgettable.memory.service.ReminderWorker
import java.util.concurrent.TimeUnit

class WorkManagerReminderScheduler(context: Context) : ReminderScheduler {
    private val appContext = context.applicationContext
    private val workManager = WorkManager.getInstance(appContext)

    override fun schedule(task: com.unforgettable.memory.data.storage.entity.TaskEntity) {
        val reminderAt = task.reminderAt ?: return
        val delay = (reminderAt - System.currentTimeMillis()).coerceAtLeast(0L)
        val work = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(workDataOf(ReminderWorker.KEY_TASK_ID to task.id))
            .addTag(tagFor(task.id))
            .build()

        workManager.enqueueUniqueWork(workNameFor(task.id), ExistingWorkPolicy.REPLACE, work)
    }

    override fun reschedule(task: com.unforgettable.memory.data.storage.entity.TaskEntity) {
        cancel(task.id)
        schedule(task)
    }

    override fun cancel(taskId: Long) {
        workManager.cancelUniqueWork(workNameFor(taskId))
    }

    private fun workNameFor(taskId: Long) = "reminder_$taskId"
    private fun tagFor(taskId: Long) = "task_reminder_$taskId"
}

