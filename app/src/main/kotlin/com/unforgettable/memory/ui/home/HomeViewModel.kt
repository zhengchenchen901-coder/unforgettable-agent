package com.unforgettable.memory.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.unforgettable.memory.UnforgettableApp
import com.unforgettable.memory.util.PermissionUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class HomeUiState(
    val notificationAccess: Boolean = false,
    val aiServiceReady: Boolean = false,
    val aiServiceLabel: String = "未配置",
    val reminderPermission: Boolean = false,
    val discoveredTaskCount: Int = 0,
    val completedTaskCount: Int = 0,
    val avoidedForgetCount: Int = 0,
)

private data class PermissionState(
    val notificationAccess: Boolean = false,
    val aiServiceReady: Boolean = false,
    val aiServiceLabel: String = "未配置",
    val reminderPermission: Boolean = false,
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as UnforgettableApp
    private val permissions = MutableStateFlow(PermissionState())

    val uiState = combine(
        permissions,
        app.container.database.taskDao().observeTaskCount(),
        app.container.database.taskDao().observeCompletedCount(),
        app.container.database.taskDao().observeReminderCount(),
    ) { permissionState, discovered, completed, reminders ->
        HomeUiState(
            notificationAccess = permissionState.notificationAccess,
            aiServiceReady = permissionState.aiServiceReady,
            aiServiceLabel = permissionState.aiServiceLabel,
            reminderPermission = permissionState.reminderPermission,
            discoveredTaskCount = discovered,
            completedTaskCount = completed,
            avoidedForgetCount = completed + reminders,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    fun refresh() {
        val provider = app.container.llmConfigStore.getSelectedProvider()
        val model = app.container.llmConfigStore.getSelectedModel(provider)
        val hasApiKey = app.container.llmConfigStore.hasApiKey(provider.id)
        permissions.value = PermissionState(
            notificationAccess = PermissionUtils.isNotificationListenerEnabled(app),
            aiServiceReady = hasApiKey,
            aiServiceLabel = if (hasApiKey) {
                "${provider.displayName} / ${model.displayName}"
            } else {
                "到 Settings 配置大模型和 API Key"
            },
            reminderPermission = PermissionUtils.hasPostNotificationsPermission(app),
        )
    }
}
