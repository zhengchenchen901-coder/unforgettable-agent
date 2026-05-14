package com.unforgettable.memory.ui.debug

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.unforgettable.memory.UnforgettableApp
import com.unforgettable.memory.service.NotificationListenerHealth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DebugSeedUiState(
    val isRunning: Boolean = false,
    val message: String? = null,
)

class DebugViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as UnforgettableApp
    private val notificationSeeder = DebugNotificationSeeder(app)
    private val _listenerHealth = MutableStateFlow(NotificationListenerHealth.snapshot(app))
    private val _seedState = MutableStateFlow(DebugSeedUiState())

    val rawNotifications = app.container.database.rawNotificationDao()
        .observeRecent()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val extractionLogs = app.container.database.aiExtractionLogDao()
        .observeRecent()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val memoryItems = app.container.database.memoryItemDao()
        .observeRecent()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val memoryAccessLogs = app.container.database.memoryAccessLogDao()
        .observeRecent()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val listenerHealth = _listenerHealth.asStateFlow()
    val seedState = _seedState.asStateFlow()

    fun refreshListenerHealth() {
        _listenerHealth.value = NotificationListenerHealth.snapshot(app)
    }

    fun injectDemoNotification() {
        seedNotifications(count = 1, insertRaw = true, postVisible = false)
    }

    fun seedRawNotifications(count: Int) {
        seedNotifications(count = count, insertRaw = true, postVisible = false)
    }

    fun showVisibleNotifications(count: Int) {
        seedNotifications(count = count, insertRaw = false, postVisible = true)
    }

    fun seedRawAndVisibleNotifications(count: Int) {
        seedNotifications(count = count, insertRaw = true, postVisible = true)
    }

    private fun seedNotifications(
        count: Int,
        insertRaw: Boolean,
        postVisible: Boolean,
    ) {
        if (_seedState.value.isRunning) return

        viewModelScope.launch {
            _seedState.value = DebugSeedUiState(isRunning = true, message = "Generating $count notifications...")
            val result = notificationSeeder.seed(
                count = count,
                insertRaw = insertRaw,
                postVisible = postVisible,
            )
            val visibleText = result.visibleSkippedReason?.let { "visible skipped: $it" }
                ?: "visible posted: ${result.visiblePosted}"
            _seedState.value = DebugSeedUiState(
                isRunning = false,
                message = "raw inserted: ${result.rawInserted}, extraction queued: ${result.extractionEnqueued}, $visibleText",
            )
        }
    }

    fun archiveMemory(id: Long) {
        viewModelScope.launch {
            app.container.memoryRepository.archiveMemory(id)
        }
    }

    fun confirmMemory(id: Long) {
        viewModelScope.launch {
            app.container.memoryRepository.confirmMemory(id)
        }
    }

    fun ignoreMemory(id: Long) {
        viewModelScope.launch {
            app.container.memoryRepository.ignoreMemory(id)
        }
    }

    fun deleteMemory(id: Long) {
        viewModelScope.launch {
            app.container.memoryRepository.deleteMemory(id)
        }
    }

    fun createUserMemory(
        type: String,
        title: String,
        content: String,
        keywordsText: String,
    ) {
        viewModelScope.launch {
            val keywords = keywordsText
                .split(",", "，", "\n")
                .map { it.trim() }
                .filter { it.isNotBlank() }
            app.container.memoryRepository.createUserMemory(
                type = type,
                title = title,
                content = content,
                keywords = keywords,
            )
        }
    }
}
