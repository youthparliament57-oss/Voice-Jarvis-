package com.example

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class AppState {
    IDLE,
    WAKE_LISTENING,
    ACTIVE_CONVERSATION
}

object AssistantStateManager {
    private val _appState = MutableStateFlow(AppState.IDLE)
    val appState: StateFlow<AppState> = _appState.asStateFlow()

    fun updateState(newState: AppState) {
        _appState.value = newState
    }
}
