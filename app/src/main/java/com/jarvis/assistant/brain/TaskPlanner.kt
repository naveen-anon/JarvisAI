package com.jarvis.assistant.brain

import com.jarvis.assistant.model.AssistantCommand

class TaskPlanner {

    fun plan(input: String): List<AssistantCommand> {

        val commands = mutableListOf<AssistantCommand>()

        val text = input.lowercase()

        if (text.contains("camera")) {
            commands.add(
                AssistantCommand(
                    action = "open_app",
                    target = "Camera"
                )
            )
        }

        if (text.contains("gallery")) {
            commands.add(
                AssistantCommand(
                    action = "open_app",
                    target = "Gallery"
                )
            )
        }

        if (text.contains("whatsapp")) {
            commands.add(
                AssistantCommand(
                    action = "open_app",
                    target = "WhatsApp"
                )
            )
        }

        if (text.contains("flash")) {
            commands.add(
                AssistantCommand(
                    action = "toggle_setting",
                    target = "flashlight"
                )
            )
        }

        if (text.contains("wifi")) {
            commands.add(
                AssistantCommand(
                    action = "toggle_setting",
                    target = "wifi"
                )
            )
        }

        return commands

    }

}
