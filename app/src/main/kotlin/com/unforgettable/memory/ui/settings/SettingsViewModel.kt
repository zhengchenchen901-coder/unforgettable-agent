package com.unforgettable.memory.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.unforgettable.memory.UnforgettableApp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

data class SettingsUiState(
    val apiKeyDraft: String = "",
    val hasSavedKey: Boolean = false,
    val message: String? = null,
)

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as UnforgettableApp
    private val _uiState = MutableStateFlow(SettingsUiState(hasSavedKey = app.container.apiKeyStore.hasApiKey()))
    val uiState: StateFlow<SettingsUiState> = _uiState

    fun updateApiKey(value: String) {
        _uiState.update { it.copy(apiKeyDraft = value, message = null) }
    }

    fun saveApiKey() {
        val key = _uiState.value.apiKeyDraft.trim()
        if (key.isBlank()) {
            _uiState.update { it.copy(message = "请输入 OpenAI API Key") }
            return
        }
        app.container.apiKeyStore.saveApiKey(key)
        _uiState.update {
            it.copy(
                apiKeyDraft = "",
                hasSavedKey = true,
                message = "OpenAI Key 已保存",
            )
        }
    }

    fun clearApiKey() {
        app.container.apiKeyStore.clear()
        _uiState.update {
            it.copy(
                apiKeyDraft = "",
                hasSavedKey = false,
                message = "OpenAI Key 已清除",
            )
        }
    }
}

