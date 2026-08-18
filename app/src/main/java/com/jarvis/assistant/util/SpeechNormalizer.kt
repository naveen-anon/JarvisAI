package com.jarvis.assistant.util

/**
 * Maps common SpeechRecognizer mistakes to phrases OfflineBrain already understands.
 */
object SpeechNormalizer {

    private val replacements = listOf(
        // Screen / analyze
        "and lies my screen" to "analyze my screen",
        "and lies the screen" to "analyze my screen",
        "anna lies screen" to "analyze my screen",
        "analysis screen" to "analyze my screen",
        "analyzer screen" to "analyze my screen",
        "analyse ice cream" to "analyze my screen",
        "on my screen what" to "what's on my screen",
        "what is on the screen" to "what's on my screen",
        // Wake / briefing
        "a jarvis" to "hey jarvis",
        "hey jarvis" to "hey jarvis",
        "hay jarvis" to "hey jarvis",
        "hey jarvis" to "hey jarvis",
        "good morning jarvis" to "good morning jarvis",
        // Weather
        "what is the weather" to "what's the weather",
        "weather like" to "what's the weather",
        "mausam kaisa" to "mausam",
        // Memory
        "remember that" to "remember that",
        "what do you remember" to "what do you remember",
        // Macros
        "create macro" to "create macro",
        "list macros" to "list macros",
    )

    fun normalize(raw: String): String {
        var s = raw.trim().lowercase()
            .replace(Regex("[\\s]+"), " ")
            .replace(".", "")
            .replace("?", "")
            .replace("!", "")
        for ((wrong, right) in replacements) {
            if (s.contains(wrong)) s = s.replace(wrong, right)
        }
        return s.trim()
    }
}
