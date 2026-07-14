package com.jarvis.assistant.brain

import com.jarvis.assistant.model.AssistantCommand

class IntentClassifier {

    fun classify(input: String): AssistantCommand? {

        val text = input.trim().lowercase()

        return when {

            // ---------- OPEN APPS ----------
            text.contains("open camera") ||
            text.contains("camera kholo") ->
                AssistantCommand("open_app", "Camera")

            text.contains("open gallery") ||
            text.contains("gallery kholo") ->
                AssistantCommand("open_app", "Gallery")

            text.contains("open chrome") ||
            text.contains("chrome kholo") ->
                AssistantCommand("open_app", "Chrome")

            text.contains("open youtube") ||
            text.contains("youtube kholo") ->
                AssistantCommand("open_app", "YouTube")

            text.contains("open whatsapp") ||
            text.contains("whatsapp kholo") ->
                AssistantCommand("open_app", "WhatsApp")

            text.contains("open telegram") ||
            text.contains("telegram kholo") ->
                AssistantCommand("open_app", "Telegram")

            // ---------- SETTINGS ----------
            text.contains("flash on") ||
            text.contains("torch on") ||
            text.contains("flashlight") ->
                AssistantCommand("toggle_setting", "flashlight")

            text.contains("wifi") ->
                AssistantCommand("toggle_setting", "wifi")

            text.contains("bluetooth") ->
                AssistantCommand("toggle_setting", "bluetooth")

            text.contains("airplane mode") ->
                AssistantCommand("toggle_setting", "airplane_mode")

            // ---------- CALL ----------
            text.startsWith("call ") -> {

                val name = input.substringAfter("call").trim()

                AssistantCommand(
                    action = "call",
                    target = name
                )
            }

            // ---------- SMS ----------
            text.startsWith("message ") -> {

                val name = input.substringAfter("message").trim()

                AssistantCommand(
                    action = "send_sms",
                    target = name,
                    message = ""
                )
            }

            // ---------- GREETINGS ----------
            text == "hi" ||
            text == "hello" ||
            text == "hey jarvis" ->
                AssistantCommand(
                    action = "reply",
                    message = "Hello! How can I help you?"
                )

            // ---------- TIME ----------
            text.contains("time") ->
                AssistantCommand(
                    action = "reply",
                    message = "Time query detected."
                )

            else -> null
        }
    }
}
