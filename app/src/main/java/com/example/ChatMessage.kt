package com.example

// Data class for chat history
data class ChatMessage(
    val id: Int,
    val text: String,
    val isUser: Boolean,
    val isSystemMessage: Boolean = false
)
