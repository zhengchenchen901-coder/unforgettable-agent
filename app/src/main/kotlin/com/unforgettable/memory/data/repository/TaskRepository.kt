package com.unforgettable.memory.data.repository

import com.unforgettable.memory.data.storage.dao.ReminderLogDao
import com.unforgettable.memory.data.storage.dao.TaskDao
import com.unforgettable.memory.data.storage.entity.TaskEntity
import com.unforgettable.memory.domain.task.TaskStatus
import com.unforgettable.memory.reminder.ReminderScheduler
import java.time.LocalDate
import java.time.ZoneId

class TaskRepository(
    private val taskDao: TaskDao,
    private val reminderLogDao: ReminderLogDao,
    private val reminderScheduler: ReminderScheduler,
) {
    suspend fun createTask(task: TaskEntity): Long {
        val id = taskDao.insert(task)
        taskDao.getById(id)?.let { reminderScheduler.schedule(it) }
        return id
    }

    suspend fun completeTask(taskId: Long) {
        val task = taskDao.getById(taskId) ?: return
        taskDao.update(
            task.copy(
                status = TaskStatus.COMPLETED,
                updatedAt = System.currentTimeMillis(),
            ),
        )
        reminderScheduler.cancel(taskId)
    }

    suspend fun delayTask(taskId: Long, delayMillis: Long) {
        val nextReminderAt = System.currentTimeMillis() + delayMillis
        delayTaskUntil(taskId, nextReminderAt)
    }

    suspend fun delayTaskUntil(taskId: Long, reminderAt: Long) {
        val task = taskDao.getById(taskId) ?: return
        val updated = task.copy(
            reminderAt = reminderAt,
            updatedAt = System.currentTimeMillis(),
        )
        taskDao.update(updated)
        reminderScheduler.reschedule(updated)
    }

    suspend fun getActiveTasks(): List<TaskEntity> = taskDao.getActiveTasksSnapshot()

    suspend fun getTasksByStatus(status: String): List<TaskEntity> = taskDao.getTasksByStatus(status)

    suspend fun countTodayReminderRuns(taskId: Long): Int {
        val startOfToday = LocalDate.now()
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
        return reminderLogDao.countTaskRemindersSince(taskId, startOfToday)
    }
}
