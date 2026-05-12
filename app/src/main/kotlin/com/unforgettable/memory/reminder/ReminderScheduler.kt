package com.unforgettable.memory.reminder

import com.unforgettable.memory.data.storage.entity.TaskEntity

interface ReminderScheduler {
    fun schedule(task: TaskEntity)
    fun reschedule(task: TaskEntity)
    fun cancel(taskId: Long)
}

