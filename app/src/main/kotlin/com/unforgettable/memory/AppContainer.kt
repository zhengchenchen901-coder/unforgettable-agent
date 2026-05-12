package com.unforgettable.memory

import android.content.Context
import com.unforgettable.memory.data.llm.ApiKeyStore
import com.unforgettable.memory.data.llm.OpenAIService
import com.unforgettable.memory.data.llm.OpenAiTaskExtractor
import com.unforgettable.memory.data.repository.TaskRepository
import com.unforgettable.memory.data.storage.AppDatabase
import com.unforgettable.memory.domain.llm.HeuristicTaskExtractor
import com.unforgettable.memory.domain.llm.TaskExtractor
import com.unforgettable.memory.reminder.WorkManagerReminderScheduler

class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val database: AppDatabase = AppDatabase.get(appContext)
    val apiKeyStore: ApiKeyStore = ApiKeyStore(appContext)
    val reminderScheduler = WorkManagerReminderScheduler(appContext)

    val taskRepository = TaskRepository(
        taskDao = database.taskDao(),
        reminderLogDao = database.reminderLogDao(),
        reminderScheduler = reminderScheduler,
    )

    val taskExtractor: TaskExtractor = OpenAiTaskExtractor(
        apiKeyStore = apiKeyStore,
        service = OpenAIService.create(),
        fallback = HeuristicTaskExtractor(),
    )
}

