package com.jarvis.assistant.notifications

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log

class JarvisNotificationListener : NotificationListenerService() {

    override fun onListenerConnected() {
        instance = this
        Log.d(TAG, "connected")
    }

    override fun onListenerDisconnected() {
        if (instance === this) instance = null
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {}
    override fun onNotificationRemoved(sbn: StatusBarNotification?) {}

    fun summarizeActive(limit: Int = 8): String {
        val active = try {
            activeNotifications ?: emptyArray()
        } catch (e: Exception) {
            return "I couldn't read notifications, sir."
        }
        if (active.isEmpty()) return "No active notifications, sir."
        val lines = active.mapNotNull { sbn ->
            if (sbn.packageName == packageName) return@mapNotNull null
            val extras = sbn.notification?.extras ?: return@mapNotNull null
            val title = extras.getCharSequence("android.title")?.toString()?.trim().orEmpty()
            val text = extras.getCharSequence("android.text")?.toString()?.trim().orEmpty()
            if (title.isBlank() && text.isBlank()) return@mapNotNull null
            val app = sbn.packageName.substringAfterLast('.')
            val body = listOf(title, text).filter { it.isNotBlank() }.joinToString(": ")
            "$app — $body"
        }.distinct().take(limit)
        if (lines.isEmpty()) return "Nothing important in the notification shade, sir."
        return "Recent notifications, sir: " + lines.joinToString("; ")
    }

    companion object {
        private const val TAG = "JarvisNotif"
        @Volatile var instance: JarvisNotificationListener? = null
        fun summaryOrHelp(limit: Int = 8): String {
            val i = instance
            return if (i == null) {
                "Notification access is off, sir. Enable Jarvis under Settings → Notification access."
            } else i.summarizeActive(limit)
        }
    }
}
