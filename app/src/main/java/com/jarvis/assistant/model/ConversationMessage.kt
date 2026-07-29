package com.jarvis.assistant.model

data class ConversationMessage(
    val sender: String,
    val message: String,
    val timestamp: Long = System.currentTimeMillis()
)
