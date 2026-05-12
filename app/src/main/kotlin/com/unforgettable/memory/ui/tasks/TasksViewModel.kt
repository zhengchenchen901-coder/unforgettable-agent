package com.unforgettable.memory.ui.tasks

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.unforgettable.memory.UnforgettableApp
import com.unforgettable.memory.data.storage.entity.TaskEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

data class TasksUiState(
    val pending: List<TaskEntity> = emptyList(),
    val completed: List<TaskEntity> = emptyList(),
)

class TasksViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as UnforgettableApp
    private val repository = app.container.taskRepository

    val uiState = combine(
        app.container.database.taskDao().observePending(),
        app.container.database.taskDao().observeCompleted(),
    ) { pending, completed ->
        TasksUiState(pending = pending, completed = completed)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TasksUiState())

    fun complete(taskId: Long) {
        viewModelScope.launch { repository.completeTask(taskId) }
    }

    fun delayOneHour(taskId: Long) {
        viewModelScope.launch { repository.delayTask(taskId, ONE_HOUR) }
    }

    fun remindTomorrow(taskId: Long) {
        val tomorrowAtNine = LocalDateTime.of(LocalDate.now().plusDays(1), LocalTime.of(9, 0))
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
        viewModelScope.launch { repository.delayTaskUntil(taskId, tomorrowAtNine) }
    }

    companion object {
        private const val ONE_HOUR = 60 * 60 * 1000L
    }
}

