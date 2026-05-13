package com.unforgettable.memory.data.llm

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

data class LlmRuntimeConfig(
    val provider: LlmProvider,
    val modelId: String,
    val apiKey: String,
)

class LlmConfigStore(context: Context) {
    private val appContext = context.applicationContext

    private val preferences: SharedPreferences by lazy {
        runCatching {
            val masterKey = MasterKey.Builder(appContext)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            EncryptedSharedPreferences.create(
                appContext,
                SECRET_PREFS,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
            )
        }.getOrElse {
            appContext.getSharedPreferences(FALLBACK_PREFS, Context.MODE_PRIVATE)
        }
    }

    fun getSelectedProvider(): LlmProvider {
        return LlmProviders.find(preferences.getString(KEY_SELECTED_PROVIDER, null))
    }

    fun saveSelectedProvider(providerId: String) {
        val provider = LlmProviders.find(providerId)
        preferences.edit()
            .putString(KEY_SELECTED_PROVIDER, provider.id)
            .apply()
    }

    fun getSelectedModel(provider: LlmProvider = getSelectedProvider()): LlmModel {
        return LlmProviders.findModel(provider, preferences.getString(modelKey(provider.id), null))
    }

    fun saveSelectedModel(providerId: String, modelId: String) {
        val provider = LlmProviders.find(providerId)
        val model = LlmProviders.findModel(provider, modelId)
        preferences.edit()
            .putString(modelKey(provider.id), model.id)
            .apply()
    }

    fun saveApiKey(providerId: String, value: String) {
        val provider = LlmProviders.find(providerId)
        val editor = preferences.edit()
            .putString(apiKey(provider.id), value.trim())
        if (provider.id == LlmProviders.OPENAI_ID) {
            editor.remove(KEY_LEGACY_OPENAI_API_KEY)
        }
        editor.apply()
    }

    fun getApiKey(providerId: String = getSelectedProvider().id): String? {
        val provider = LlmProviders.find(providerId)
        val key = preferences.getString(apiKey(provider.id), null)
            ?: if (provider.id == LlmProviders.OPENAI_ID) {
                preferences.getString(KEY_LEGACY_OPENAI_API_KEY, null)
            } else {
                null
            }
        return key?.takeIf { it.isNotBlank() }
    }

    fun clearApiKey(providerId: String = getSelectedProvider().id) {
        val provider = LlmProviders.find(providerId)
        val editor = preferences.edit().remove(apiKey(provider.id))
        if (provider.id == LlmProviders.OPENAI_ID) {
            editor.remove(KEY_LEGACY_OPENAI_API_KEY)
        }
        editor.apply()
    }

    fun hasApiKey(providerId: String = getSelectedProvider().id): Boolean {
        return getApiKey(providerId) != null
    }

    fun getRuntimeConfig(): LlmRuntimeConfig? {
        val provider = getSelectedProvider()
        val apiKey = getApiKey(provider.id) ?: return null
        return LlmRuntimeConfig(
            provider = provider,
            modelId = getSelectedModel(provider).id,
            apiKey = apiKey,
        )
    }

    companion object {
        private const val SECRET_PREFS = "unforgettable_secrets"
        private const val FALLBACK_PREFS = "unforgettable_secrets_fallback"
        private const val KEY_SELECTED_PROVIDER = "llm_selected_provider"
        private const val KEY_LEGACY_OPENAI_API_KEY = "openai_api_key"

        private fun apiKey(providerId: String): String = "llm_api_key_$providerId"
        private fun modelKey(providerId: String): String = "llm_model_$providerId"
    }
}
