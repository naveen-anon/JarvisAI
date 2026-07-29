package com.jarvis.assistant.model

data class AssistantCommand(
    val action: String? = null,
    val rawCommand: String? = null,
    val parameters: Map<String, String> = emptyMap()
)
