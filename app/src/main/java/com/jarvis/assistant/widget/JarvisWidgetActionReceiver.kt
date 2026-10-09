package com.jarvis.assistant.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.jarvis.assistant.service.AssistantForegroundService

class JarvisWidgetActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != ACTION_START_LISTENING) return
        val serviceIntent = Intent(context, AssistantForegroundService::class.java).apply {
            action = ACTION_START_LISTENING
        }
        try {
            context.startForegroundService(serviceIntent)
        } catch (_: Exception) {
            try { context.startService(serviceIntent) } catch (_: Exception) {}
        }
    }

    companion object {
        const val ACTION_START_LISTENING = "com.jarvis.assistant.ACTION_START_LISTENING"
    }
}
