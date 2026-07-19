package com.jarvis.assistant.brain

data class AssistantIntent(
    val type: IntentType,
    val target: String = "",
    val message: String = ""
)
