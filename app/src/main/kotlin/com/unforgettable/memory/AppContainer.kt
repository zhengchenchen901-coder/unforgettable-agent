package com.unforgettable.memory

import android.content.Context
import com.unforgettable.memory.data.llm.LlmChatService
import com.unforgettable.memory.data.llm.LlmConfigStore
import com.unforgettable.memory.data.llm.LlmTaskExtractor
import com.unforgettable.memory.data.repository.TaskRepository
import com.unforgettable.memory.data.storage.AppDatabase
import com.unforgettable.memory.domain.llm.HeuristicTaskExtractor
import com.unforgettable.memory.domain.llm.TaskExtractor
import com.unforgettable.memory.reminder.WorkManagerReminderScheduler

class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val database: AppDatabase = AppDatabase.get(appContext)
    val llmConfigStore: LlmConfigStore = LlmConfigStore(appContext)
    val reminderScheduler = WorkManagerReminderScheduler(appContext)

    val taskRepository = TaskRepository(
        taskDao = database.taskDao(),
        reminderLogDao = database.reminderLogDao(),
        reminderScheduler = reminderScheduler,
    )

    val taskExtractor: TaskExtractor = LlmTaskExtractor(
        configStore = llmConfigStore,
        service = LlmChatService.create(),
        fallback = HeuristicTaskExtractor(),
    )
}
