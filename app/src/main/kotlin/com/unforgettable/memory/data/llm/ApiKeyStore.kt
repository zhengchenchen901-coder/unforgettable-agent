package com.unforgettable.memory.data.llm

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class ApiKeyStore(context: Context) {
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

    fun saveApiKey(value: String) {
        preferences.edit()
            .putString(KEY_OPENAI_API_KEY, value.trim())
            .apply()
    }

    fun getApiKey(): String? = preferences.getString(KEY_OPENAI_API_KEY, null)
        ?.takeIf { it.isNotBlank() }

    fun clear() {
        preferences.edit().remove(KEY_OPENAI_API_KEY).apply()
    }

    fun hasApiKey(): Boolean = getApiKey() != null

    companion object {
        private const val SECRET_PREFS = "unforgettable_secrets"
        private const val FALLBACK_PREFS = "unforgettable_secrets_fallback"
        private const val KEY_OPENAI_API_KEY = "openai_api_key"
    }
}

