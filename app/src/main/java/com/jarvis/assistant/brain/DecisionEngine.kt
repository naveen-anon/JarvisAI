package com.jarvis.assistant.brain

import com.jarvis.assistant.ai.GeminiClient
import com.jarvis.assistant.executor.CommandExecutor

class DecisionEngine(
    private val brain: JarvisBrain,
    private val gemini: GeminiClient,
    private val executor: CommandExecutor
) {

    suspend fun execute(userInput: String): String {

        val input = userInput.trim()

        if (input.isBlank()) {
            return "I didn't hear anything."
        }

        return try {

            // Local Brain First
            brain.process(input)

        } catch (e: Exception) {

            try {

                // AI Fallback
                val command = gemini.getCommand(input)
                executor.execute(command)

            } catch (err: Exception) {

                "Sorry, I couldn't process your request."

            }

        }

    }

    fun shouldUseOffline(text: String): Boolean {

        val cmd = text.lowercase()

        val keywords = listOf(

            "open",
            "launch",
            "start",
            "camera",
            "gallery",
            "chrome",
            "youtube",
            "whatsapp",
            "telegram",
            "wifi",
            "bluetooth",
            "flash",
            "call",
            "message",
            "sms",
            "settings"

        )

        return keywords.any {
            cmd.contains(it)
        }

    }

}
