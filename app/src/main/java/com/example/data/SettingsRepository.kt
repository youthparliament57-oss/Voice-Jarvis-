package com.example.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsRepository(private val context: Context) {
    companion object {
        private const val PREFS_FILENAME = "jarvis_secure_prefs"
        private const val GEMINI_API_KEY = "gemini_api_key"
    }

    private val sharedPreferences: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            context,
            PREFS_FILENAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    private val _apiKeyFlow = MutableStateFlow(getApiKey() ?: "")
    val apiKeyFlow: Flow<String> = _apiKeyFlow.asStateFlow()

    fun getApiKey(): String? {
        return sharedPreferences.getString(GEMINI_API_KEY, null)
    }

    suspend fun saveApiKey(apiKey: String) {
        sharedPreferences.edit().putString(GEMINI_API_KEY, apiKey).apply()
        _apiKeyFlow.value = apiKey
    }
}
