package com.jarvis.assistant.util

import java.util.concurrent.ConcurrentHashMap

/**
 * Tracks conversation context — recent messages, user mood, time patterns, location —
 * so the offline brain can give smarter, more contextual responses without needing
 * to hit the cloud for every reply. Survives within a single session (not persistent
 * across app restarts — use PersistentMemory for that).
 */
class ConversationContext {

    private val messageHistory = mutableListOf<ConversationMessage>()
    private val userPreferences = ConcurrentHashMap<String, String>()
    private var lastUserMood = "neutral"
    private var conversationStartTime = System.currentTimeMillis()
    private var recentLocations = mutableListOf<String>()

    data class ConversationMessage(
        val text: String,
        val isUserInput: Boolean,
        val timestamp: Long = System.currentTimeMillis()
    )

    fun addMessage(text: String, isUserInput: Boolean) {
        messageHistory.add(ConversationMessage(text, isUserInput))
        if (messageHistory.size > 50) {
            messageHistory.removeAt(0)  // Keep last 50 messages
        }
        // Auto-detect mood from user messages
        if (isUserInput) {
            lastUserMood = detectMood(text)
        }
    }

    fun getRecentMessages(count: Int = 5): List<ConversationMessage> {
        return messageHistory.takeLast(count)
    }

    fun getConversationDuration(): Long = System.currentTimeMillis() - conversationStartTime

    fun getUserMood(): String = lastUserMood

    fun addUserPreference(key: String, value: String) {
        userPreferences[key] = value
    }

    fun getUserPreference(key: String): String? = userPreferences[key]

    fun updateLocation(location: String) {
        if (recentLocations.isEmpty() || recentLocations.last() != location) {
            recentLocations.add(location)
            if (recentLocations.size > 10) recentLocations.removeAt(0)
        }
    }

    fun getRecentLocations(): List<String> = recentLocations.toList()

    fun shouldSuggestAction(): Boolean {
        // Suggest an action every 5+ interactions
        return messageHistory.size % 5 == 0
    }

    fun clear() {
        messageHistory.clear()
        userPreferences.clear()
        recentLocations.clear()
        lastUserMood = "neutral"
        conversationStartTime = System.currentTimeMillis()
    }

    private fun detectMood(text: String): String = when {
        text.contains(Regex("\\b(good|great|amazing|awesome|love|happy|excited)\\b", RegexOption.IGNORE_CASE)) -> "happy"
        text.contains(Regex("\\b(bad|terrible|hate|angry|frustrated|upset)\\b", RegexOption.IGNORE_CASE)) -> "sad"
        text.contains(Regex("\\b(please|help|need|urgent|asap)\\b", RegexOption.IGNORE_CASE)) -> "urgent"
        text.contains(Regex("\\b(thank|thanks|appreciate)\\b", RegexOption.IGNORE_CASE)) -> "grateful"
        else -> "neutral"
    }
}
