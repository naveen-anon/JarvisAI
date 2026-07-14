package com.jarvis.assistant.brain

import android.util.Log
import com.jarvis.assistant.executor.CommandExecutor
import com.jarvis.assistant.model.AssistantCommand

class ActionDispatcher(
    private val executor: CommandExecutor
) {

    companion object {
        private const val TAG = "ActionDispatcher"
    }

    fun dispatch(command: AssistantCommand): String {
        return try {
            Log.d(TAG, "Executing: ${command.action} -> ${command.target}")

            executor.execute(command)

        } catch (e: Exception) {

            Log.e(TAG, "Execution failed", e)

            "Failed to execute command."

        }
    }

    fun dispatchAll(commands: List<AssistantCommand>): List<String> {

        val results = mutableListOf<String>()

        commands.forEach { cmd ->
            results.add(dispatch(cmd))
        }

        return results
    }

    fun hasCommands(commands: List<AssistantCommand>): Boolean {
        return commands.isNotEmpty()
    }
}
