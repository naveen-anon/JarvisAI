package com.jarvis.assistant.util

import com.jarvis.assistant.model.ConversationMessage

object SmartSuggestionEngine {
    private val history = mutableListOf<ConversationMessage>()

    fun recordInteraction(query: String, response: String) {
        history.add(ConversationMessage("user", query))
        history.add(ConversationMessage("jarvis", response))
    }

    fun getSuggestions(): List<String> {
        return listOf("Open WhatsApp", "What is the time?", "Jarvis Status")
    }
}
