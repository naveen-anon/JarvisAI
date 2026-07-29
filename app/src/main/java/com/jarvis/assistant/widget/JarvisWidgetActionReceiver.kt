package com.jarvis.assistant.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.jarvis.assistant.service.AssistantForegroundService

/**
 * Triggered by either the home screen widget (JarvisWidgetProvider) or the lock-screen
 * notification's "Listen" action. Both need to start listening without any Activity ever
 * being opened — this starts (or reuses) the foreground service and kicks off a listening
 * cycle directly, headless.
 */
class JarvisWidgetActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val serviceIntent = Intent(context, AssistantForegroundService::class.java).apply {
            action = ACTION_START_LISTENING
        }
        context.startForegroundService(serviceIntent)
    }

    companion object {
        const val ACTION_START_LISTENING = "com.jarvis.assistant.ACTION_START_LISTENING"
    }
}
