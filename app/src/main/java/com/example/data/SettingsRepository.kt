package com.example.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsRepository private constructor(private val context: Context) {

    companion object {
        private const val PREFS_FILENAME = "jarvis_secure_prefs"
        private const val GEMINI_API_KEY = "gemini_api_key"
        private const val SYSTEM_PROMPT = "system_prompt"
        private const val MODEL_NAME = "model_name"
        private const val WAKE_THRESHOLD = "wake_threshold"
        private const val SESSION_TIMEOUT = "session_timeout"

        @Volatile
        private var INSTANCE: SettingsRepository? = null

        fun getInstance(context: Context): SettingsRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SettingsRepository(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    private val sharedPreferences: SharedPreferences by lazy {
        try {
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
        } catch (e: Exception) {
            android.util.Log.e("SettingsRepository", "EncryptedSharedPreferences error, recreating preferences file", e)
            try {
                context.deleteSharedPreferences(PREFS_FILENAME)
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
            } catch (fallbackEx: Exception) {
                android.util.Log.e("SettingsRepository", "Falling back to standard SharedPreferences", fallbackEx)
                context.getSharedPreferences(PREFS_FILENAME, Context.MODE_PRIVATE)
            }
        }
    }

    private val _apiKeyFlow = MutableStateFlow(getApiKey() ?: "")
    val apiKeyFlow: Flow<String> = _apiKeyFlow.asStateFlow()

    private val _systemPromptFlow = MutableStateFlow(getSystemPrompt())
    val systemPromptFlow: Flow<String> = _systemPromptFlow.asStateFlow()

    private val _modelNameFlow = MutableStateFlow(getModelName())
    val modelNameFlow: Flow<String> = _modelNameFlow.asStateFlow()

    private val _wakeThresholdFlow = MutableStateFlow(getWakeThreshold())
    val wakeThresholdFlow: Flow<Float> = _wakeThresholdFlow.asStateFlow()

    private val _sessionTimeoutFlow = MutableStateFlow(getSessionTimeout())
    val sessionTimeoutFlow: Flow<Long> = _sessionTimeoutFlow.asStateFlow()

    fun getApiKey(): String? {
        return sharedPreferences.getString(GEMINI_API_KEY, null)
    }

    suspend fun saveApiKey(apiKey: String) {
        sharedPreferences.edit().putString(GEMINI_API_KEY, apiKey).apply()
        _apiKeyFlow.value = apiKey
    }
    
    fun getSystemPrompt(): String {
        return sharedPreferences.getString(SYSTEM_PROMPT, "You are JARVIS, a highly advanced, witty AI assistant. Keep responses brief and conversational.") ?: "You are JARVIS, a highly advanced, witty AI assistant. Keep responses brief and conversational."
    }
    
    suspend fun saveSystemPrompt(prompt: String) {
        sharedPreferences.edit().putString(SYSTEM_PROMPT, prompt).apply()
        _systemPromptFlow.value = prompt
    }

    fun getModelName(): String {
        return sharedPreferences.getString(MODEL_NAME, "models/gemini-2.5-flash") ?: "models/gemini-2.5-flash"
    }

    suspend fun saveModelName(model: String) {
        sharedPreferences.edit().putString(MODEL_NAME, model).apply()
        _modelNameFlow.value = model
    }
    
    fun getWakeThreshold(): Float {
        return sharedPreferences.getFloat(WAKE_THRESHOLD, 0.50f)
    }
    
    suspend fun saveWakeThreshold(threshold: Float) {
        sharedPreferences.edit().putFloat(WAKE_THRESHOLD, threshold).apply()
        _wakeThresholdFlow.value = threshold
    }

    fun getSessionTimeout(): Long {
        return sharedPreferences.getLong(SESSION_TIMEOUT, 120_000L)
    }

    suspend fun saveSessionTimeout(timeout: Long) {
        sharedPreferences.edit().putLong(SESSION_TIMEOUT, timeout).apply()
        _sessionTimeoutFlow.value = timeout
    }
}
