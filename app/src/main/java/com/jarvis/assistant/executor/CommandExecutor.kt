package com.jarvis.assistant.executor

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import com.jarvis.assistant.brain.OfflineBrain
import com.jarvis.assistant.model.AssistantCommand
import com.jarvis.assistant.voice.TextToSpeechHelper

object CommandExecutor {

    // Overload 1: Handles AssistantCommand object from Foreground Service
    fun execute(context: Context, command: AssistantCommand): String {
        val query = command.rawCommand ?: command.action ?: ""
        return execute(context, query)
    }

    // Overload 2: Handles direct String query
    fun execute(context: Context, userQuery: String): String {
        val cleanQuery = userQuery.lowercase().trim()

        if (cleanQuery.startsWith("open ") || cleanQuery.startsWith("kholo ")) {
            val targetApp = cleanQuery
                .replace("open ", "")
                .replace("kholo ", "")
                .trim()

            val launched = launchAppByName(context, targetApp)
            val response = if (launched) {
                "$targetApp open kar raha hoon, Boss."
            } else {
                "$targetApp device mein nahi mila."
            }

            TextToSpeechHelper.speak(context, response)
            return response
        }

        val response = OfflineBrain.processQuery(userQuery)
        TextToSpeechHelper.speak(context, response)
        return response
    }

    private fun launchAppByName(context: Context, appName: String): Boolean {
        val pm = context.packageManager
        val installedApps = pm.getInstalledApplications(PackageManager.GET_META_DATA)

        for (app in installedApps) {
            val label = pm.getApplicationLabel(app).toString().lowercase()
            if (label.contains(appName)) {
                val launchIntent = pm.getLaunchIntentForPackage(app.packageName)
                if (launchIntent != null) {
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(launchIntent)
                    return true
                }
            }
        }
        return false
    }
}
