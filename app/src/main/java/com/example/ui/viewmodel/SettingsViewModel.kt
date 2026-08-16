package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

sealed interface ValidationState {
    object Idle : ValidationState
    object Testing : ValidationState
    data class Success(val message: String) : ValidationState
    data class Error(val error: String) : ValidationState
}

sealed interface SaveStatus {
    object Idle : SaveStatus
    data class Success(val message: String) : SaveStatus
    data class Error(val error: String) : SaveStatus
}

class SettingsViewModel(private val repository: SettingsRepository) : ViewModel() {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()

    val apiKey: StateFlow<String> = repository.apiKeyFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ""
        )

    private val _validationState = MutableStateFlow<ValidationState>(ValidationState.Idle)
    val validationState: StateFlow<ValidationState> = _validationState.asStateFlow()

    private val _saveStatus = MutableStateFlow<SaveStatus>(SaveStatus.Idle)
    val saveStatus: StateFlow<SaveStatus> = _saveStatus.asStateFlow()

    fun testApiKey(key: String) {
        val cleanKey = key.trim()
        if (cleanKey.isEmpty()) {
            _validationState.value = ValidationState.Error("Please enter an API Key first.")
            return
        }

        _validationState.value = ValidationState.Testing
        viewModelScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    val url = "https://generativelanguage.googleapis.com/v1beta/models?key=$cleanKey"
                    val request = Request.Builder().url(url).get().build()
                    val response = httpClient.newCall(request).execute()

                    if (response.isSuccessful) {
                        ValidationState.Success("Key Validated! Gemini 2.5 API connection successful.")
                    } else if (response.code == 400 || response.code == 403) {
                        ValidationState.Error("Invalid API Key (HTTP ${response.code}). Check key in Google AI Studio.")
                    } else {
                        ValidationState.Error("API Test Failed (HTTP ${response.code}).")
                    }
                }
                _validationState.value = result
            } catch (e: Exception) {
                _validationState.value = ValidationState.Error("Network Error: ${e.localizedMessage ?: "Failed to reach Gemini API"}")
            }
        }
    }

    fun saveApiKey(key: String) {
        val cleanKey = key.trim()
        viewModelScope.launch {
            repository.saveApiKey(cleanKey)
            if (cleanKey.isNotEmpty()) {
                val timeStr = SimpleDateFormat("hh:mm:ss a", Locale.getDefault()).format(Date())
                _saveStatus.value = SaveStatus.Success("Key saved securely on device at $timeStr")
            } else {
                _saveStatus.value = SaveStatus.Success("API Key cleared from device")
            }
        }
    }

    fun clearValidationState() {
        _validationState.value = ValidationState.Idle
    }

    class Factory(private val repository: SettingsRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return SettingsViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
