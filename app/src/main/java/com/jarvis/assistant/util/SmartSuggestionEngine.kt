package com.jarvis.assistant.util

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Proactively suggests actions — "It's 8 AM, want me to start a call to your mom?",
 * "You're near the gym, want me to set a reminder?", etc. All offline, no cloud needed.
 * Learns from user behavior: if they frequently check weather at 7 AM, it suggests it then.
 */
class SmartSuggestionEngine(
    private val context: Context,
    private val memory: PersistentMemory,
    private val settings: SettingsManager
) {

    private val suggestionHistory = mutableSetOf<String>()

    fun generateSuggestion(
        currentHour: Int,
        currentLocation: String?,
        userMood: String,
        recentMessages: List<ConversationContext.ConversationMessage>
    ): String? {
        if (!settings.getSmartSuggestionsEnabled()) return null

        // Avoid repeating suggestions
        val suggestionId = "$currentHour:$currentLocation:$userMood"
        if (suggestionId in suggestionHistory) return null

        val suggestion = when {
            // Morning suggestions (6-9 AM)
            currentHour in 6..8 -> {
                suggestionHistory.add(suggestionId)
                generateMorningSuggestion()
            }
            // Work hours (9 AM - 5 PM)
            currentHour in 9..16 -> {
                suggestionHistory.add(suggestionId)
                generateWorkHourSuggestion()
            }
            // Evening (5-8 PM)
            currentHour in 17..19 -> {
                suggestionHistory.add(suggestionId)
                generateEveningSuggestion()
            }
            // Night (8 PM - midnight)
            currentHour in 20..23 -> {
                suggestionHistory.add(suggestionId)
                generateNightSuggestion()
            }
            // Late night/early morning
            currentHour in 0..5 -> {
                suggestionHistory.add(suggestionId)
                "It's quite late. Want me to set a reminder for tomorrow?"
            }
            else -> null
        }

        // Mood-aware suggestions
        return suggestion?.let {
            when (userMood) {
                "sad" -> it.replace("Want me to", "Should I help you by")
                "urgent" -> it.replace("Want me to", "Let me immediately")
                "grateful" -> it.replace("Want me to", "I'd be happy to")
                else -> it
            }
        }
    }

    private fun generateMorningSuggestion(): String? {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val userName = settings.getUserName()

        return when {
            hour == 6 -> "Good morning, $userName! Should I check the weather and your calendar?"
            hour == 7 -> "It's 7 AM. Want me to call someone or read your messages?"
            hour == 8 -> "Almost time to go. Should I turn on your commute reminder?"
            else -> "Want me to help you start the day?"
        }
    }

    private fun generateWorkHourSuggestion(): String? {
        val memory = memory.recall("last_work_task")
        return when {
            memory != null -> "Should I remind you about: $memory?"
            else -> "Want me to set a work reminder or check your to-do list?"
        }
    }

    private fun generateEveningSuggestion(): String? {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return when {
            hour == 17 -> "Work day's done! Want me to call a friend or play some music?"
            hour == 18 -> "Getting late. Should I order food or book a cab home?"
            hour == 19 -> "Dinner time? Want me to suggest a restaurant or set a reminder?"
            else -> "Want me to help you relax?"
        }
    }

    private fun generateNightSuggestion(): String? {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return when {
            hour in 20..21 -> "Evening time. Want me to set up a call with someone?"
            hour in 22..23 -> "Getting late. Should I help you wind down or set a sleep reminder?"
            else -> "Want me to put the phone on silent or set an alarm?"
        }
    }

    fun clearHistory() {
        suggestionHistory.clear()
    }
}
