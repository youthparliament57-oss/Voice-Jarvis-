package com.example.ui.viewmodels

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SettingsViewModel(private val context: Context) : ViewModel() {

    private val PREFS_NAME = "secret_prefs"
    private val API_KEY_KEY = "gemini_api_key"

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPreferences = EncryptedSharedPreferences.create(
        context,
        PREFS_NAME,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    private val _apiKey = MutableStateFlow<String?>(null)
    val apiKey: StateFlow<String?> = _apiKey

    init {
        loadApiKey()
    }

    private fun loadApiKey() {
        val key = sharedPreferences.getString(API_KEY_KEY, null)
        _apiKey.value = key
    }

    fun saveApiKey(key: String) {
        viewModelScope.launch {
            try {
                sharedPreferences.edit().putString(API_KEY_KEY, key).apply()
                _apiKey.value = key
                Log.d("SettingsViewModel", "API key saved securely.")
            } catch (e: Exception) {
                Log.e("SettingsViewModel", "Error saving API key", e)
            }
        }
    }
}

class SettingsViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SettingsViewModel(context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
