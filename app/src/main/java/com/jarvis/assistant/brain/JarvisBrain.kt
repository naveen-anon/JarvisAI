package com.jarvis.assistant.brain

import android.util.Log
import com.jarvis.assistant.ai.GeminiClient
import com.jarvis.assistant.executor.CommandExecutor
import com.jarvis.assistant.model.AssistantCommand

class JarvisBrain(
    private val gemini: GeminiClient,
    private val executor: CommandExecutor
) {

    companion object {
        private const val TAG = "JarvisBrain"
    }

    private val memory = mutableMapOf<String, String>()

    suspend fun process(text: String): String {

        val speech = text.trim()

        Log.d(TAG, "User: $speech")

        val local = processOffline(speech)

        if (local != null) {
            Log.d(TAG, "Offline command executed.")
            return executor.execute(local)
        }

        Log.d(TAG, "Sending to Gemini...")

        return try {

            val aiCommand = gemini.getCommand(speech)

            executor.execute(aiCommand)

        } catch (e: Exception) {

            Log.e(TAG, e.message ?: "Unknown Error")

            "Sorry, something went wrong."

        }

    }

    private fun processOffline(text: String): AssistantCommand? {

        val cmd = text.lowercase()

        return when {

            cmd.contains("camera") ->
                AssistantCommand(
                    action = "open_app",
                    target = "Camera"
                )

            cmd.contains("gallery") ->
                AssistantCommand(
                    action = "open_app",
                    target = "Gallery"
                )

            cmd.contains("youtube") ->
                AssistantCommand(
                    action = "open_app",
                    target = "YouTube"
                )

            cmd.contains("chrome") ->
                AssistantCommand(
                    action = "open_app",
                    target = "Chrome"
                )

            cmd.contains("settings") ->
                AssistantCommand(
                    action = "open_app",
                    target = "Settings"
                )

            cmd.contains("whatsapp") ->
                AssistantCommand(
                    action = "open_app",
                    target = "WhatsApp"
                )

            cmd.contains("telegram") ->
                AssistantCommand(
                    action = "open_app",
                    target = "Telegram"
                )

            cmd.contains("flash") ->
                AssistantCommand(
                    action = "toggle_setting",
                    target = "flashlight"
                )

            cmd.contains("wifi") ->
                AssistantCommand(
                    action = "toggle_setting",
                    target = "wifi"
                )

            cmd.contains("bluetooth") ->
                AssistantCommand(
                    action = "toggle_setting",
                    target = "bluetooth"
                )

            cmd.startsWith("call ") -> {

                val person = text.substringAfter("call").trim()

                AssistantCommand(
                    action = "call",
                    target = person
                )

            }

            cmd.startsWith("open ") -> {

                val app = text.substringAfter("open").trim()

                AssistantCommand(
                    action = "open_app",
                    target = app
                )

            }

            cmd.startsWith("launch ") -> {

                val app = text.substringAfter("launch").trim()

                AssistantCommand(
                    action = "open_app",
                    target = app
                )

            }

            cmd.startsWith("start ") -> {

                val app = text.substringAfter("start").trim()

                AssistantCommand(
                    action = "open_app",
                    target = app
                )

            }

            cmd.contains("remember") -> {

                val data = text.substringAfter("remember").trim()

                memory["last_note"] = data

                AssistantCommand(
                    action = "reply",
                    message = "I will remember that."
                )

            }

            cmd.contains("what did i tell you") -> {

                AssistantCommand(
                    action = "reply",
                    message = memory["last_note"] ?: "You haven't told me anything yet."
                )

            }

            else -> null

        }

    }

}
