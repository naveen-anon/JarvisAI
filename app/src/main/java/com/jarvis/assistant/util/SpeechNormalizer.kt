package com.jarvis.assistant.util

/**
 * Maps common SpeechRecognizer mistakes + synonyms to phrases OfflineBrain understands.
 */
object SpeechNormalizer {

    private val replacements = listOf(
        "and lies my screen" to "analyze my screen",
        "and lies the screen" to "analyze my screen",
        "anna lies screen" to "analyze my screen",
        "analysis screen" to "analyze my screen",
        "analyzer screen" to "analyze my screen",
        "analyse ice cream" to "analyze my screen",
        "on my screen what" to "what's on my screen",
        "what is on the screen" to "what's on my screen",
        "read the screen" to "read this page",
        "screen padho" to "read this page",
        "a jarvis" to "hey jarvis",
        "hay jarvis" to "hey jarvis",
        "hey jarwis" to "hey jarvis",
        "morning briefing" to "briefing",
        "daily briefing" to "briefing",
        "what is the weather" to "what's the weather",
        "weather like" to "what's the weather",
        "mausam kaisa" to "mausam",
        "serch for" to "search for",
        "search fro" to "search for",
        "google for" to "search for",
        "look up" to "search for",
        "play the song" to "play song",
        "put on youtube" to "play on youtube",
        "youtube pe chalao" to "play on youtube",
        "kholo" to "open",
        "khol do" to "open",
        "band karo" to "close",
        "band kar do" to "close",
        "awaaz badhao" to "volume up",
        "awaaz kam" to "volume down",
        "silent mode" to "mute",
        "torch on" to "flashlight on",
        "torch off" to "flashlight off",
        "torch jalao" to "flashlight on",
        "torch bujhao" to "flashlight off",
        "whats app" to "whatsapp",
        "what's app" to "whatsapp",
        "wake me at" to "set alarm for",
        "alarm at" to "set alarm for",
        "timer of" to "timer for",
        "missed notifications" to "what did i miss",
        "go home" to "go to home screen",
        "show home" to "go to home screen",
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
