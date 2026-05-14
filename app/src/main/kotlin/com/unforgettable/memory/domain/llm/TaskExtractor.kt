package com.unforgettable.memory.domain.llm

import com.unforgettable.memory.data.storage.entity.TaskEntity
import com.unforgettable.memory.domain.memory.MemoryContextItem
import com.unforgettable.memory.domain.notification.NotificationEvent

interface TaskExtractor {
    suspend fun extract(
        event: NotificationEvent,
        activeTasks: List<TaskEntity>,
        memoryContext: List<MemoryContextItem>,
    ): TaskExtractionResult
}
