package com.jarvis.assistant.brain

data class Intent(
    val action: String,
    val confidence: Float,
    val rawText: String
)
