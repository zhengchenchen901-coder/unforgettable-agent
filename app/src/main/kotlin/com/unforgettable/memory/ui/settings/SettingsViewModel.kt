package com.unforgettable.memory.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.unforgettable.memory.UnforgettableApp
import com.unforgettable.memory.data.llm.LlmModel
import com.unforgettable.memory.data.llm.LlmProvider
import com.unforgettable.memory.data.llm.LlmProviders
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

data class SettingsUiState(
    val providers: List<LlmProvider> = LlmProviders.all,
    val selectedProviderId: String = LlmProviders.defaultProvider.id,
    val selectedModelId: String = LlmProviders.defaultProvider.defaultModelId,
    val apiKeyDraft: String = "",
    val hasSavedKey: Boolean = false,
    val message: String? = null,
) {
    val selectedProvider: LlmProvider = LlmProviders.find(selectedProviderId)
    val selectedModel: LlmModel = LlmProviders.findModel(selectedProvider, selectedModelId)
}

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as UnforgettableApp
    private val store = app.container.llmConfigStore
    private val _uiState = MutableStateFlow(loadState())
    val uiState: StateFlow<SettingsUiState> = _uiState

    fun selectProvider(providerId: String) {
        store.saveSelectedProvider(providerId)
        val provider = store.getSelectedProvider()
        val model = store.getSelectedModel(provider)
        _uiState.update {
            it.copy(
                selectedProviderId = provider.id,
                selectedModelId = model.id,
                apiKeyDraft = "",
                hasSavedKey = store.hasApiKey(provider.id),
                message = null,
            )
        }
    }

    fun selectModel(modelId: String) {
        val provider = _uiState.value.selectedProvider
        store.saveSelectedModel(provider.id, modelId)
        val model = store.getSelectedModel(provider)
        _uiState.update {
            it.copy(
                selectedModelId = model.id,
                message = null,
            )
        }
    }

    fun updateApiKey(value: String) {
        _uiState.update { it.copy(apiKeyDraft = value, message = null) }
    }

    fun saveApiKey() {
        val provider = _uiState.value.selectedProvider
        val key = normalizeApiKey(_uiState.value.apiKeyDraft)
        if (key.isBlank()) {
            _uiState.update { it.copy(message = "请输入 ${provider.apiKeyLabel}") }
            return
        }
        val requiredPrefix = provider.apiKeyRequiredPrefix
        if (requiredPrefix != null && !key.startsWith(requiredPrefix)) {
            _uiState.update { it.copy(message = "请粘贴完整 ${provider.apiKeyLabel}，包括 $requiredPrefix 前缀") }
            return
        }
        store.saveApiKey(provider.id, key)
        _uiState.update {
            it.copy(
                apiKeyDraft = "",
                hasSavedKey = true,
                message = "${provider.displayName} Key 已保存",
            )
        }
    }

    private fun normalizeApiKey(value: String): String {
        val key = value.trim()
        return if (key.startsWith("Bearer ", ignoreCase = true)) {
            key.substringAfter(' ').trim()
        } else {
            key
        }
    }

    fun clearApiKey() {
        val provider = _uiState.value.selectedProvider
        store.clearApiKey(provider.id)
        _uiState.update {
            it.copy(
                apiKeyDraft = "",
                hasSavedKey = false,
                message = "${provider.displayName} Key 已清除",
            )
        }
    }

    private fun loadState(): SettingsUiState {
        val provider = store.getSelectedProvider()
        val model = store.getSelectedModel(provider)
        return SettingsUiState(
            selectedProviderId = provider.id,
            selectedModelId = model.id,
            hasSavedKey = store.hasApiKey(provider.id),
        )
    }
}
