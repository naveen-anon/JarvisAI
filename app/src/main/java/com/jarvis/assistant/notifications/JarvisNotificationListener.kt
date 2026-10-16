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

    fun summarizeActive(limit: Int = 8, appFilter: String? = null): String {
        val active = try {
            activeNotifications ?: emptyArray()
        } catch (e: Exception) {
            return "I couldn't read notifications, sir."
        }
        if (active.isEmpty()) return "No active notifications, sir."
        val filter = appFilter?.lowercase()?.trim()
        val lines = active.mapNotNull { sbn ->
            if (sbn.packageName == packageName) return@mapNotNull null
            val pkg = sbn.packageName.lowercase()
            if (filter != null) {
                val ok = when {
                    "whatsapp" in filter -> "whatsapp" in pkg
                    "telegram" in filter -> "telegram" in pkg
                    "instagram" in filter || "insta" in filter -> "instagram" in pkg
                    "gmail" in filter || "mail" in filter || "email" in filter ->
                        "gm" in pkg || "mail" in pkg || "email" in pkg
                    "sms" in filter || "message" in filter ->
                        "messaging" in pkg || "mms" in pkg || "sms" in pkg || pkg.contains("message")
                    else -> filter in pkg || pkg.contains(filter)
                }
                if (!ok) return@mapNotNull null
            }
            val extras = sbn.notification?.extras ?: return@mapNotNull null
            val title = extras.getCharSequence("android.title")?.toString()?.trim().orEmpty()
            val text = extras.getCharSequence("android.text")?.toString()?.trim().orEmpty()
            val big = extras.getCharSequence("android.bigText")?.toString()?.trim().orEmpty()
            val bodyText = when {
                big.isNotBlank() -> big
                text.isNotBlank() -> text
                else -> ""
            }
            if (title.isBlank() && bodyText.isBlank()) return@mapNotNull null
            val appLabel = friendlyApp(sbn.packageName)
            val body = listOf(title, bodyText).filter { it.isNotBlank() }.joinToString(": ")
            val short = if (body.length > 160) body.take(160) + "…" else body
            "$appLabel — $short"
        }.distinct().take(limit)
        if (lines.isEmpty()) {
            return if (filter != null) "No $filter notifications right now, sir."
            else "Nothing important in the notification shade, sir."
        }
        return "Here's what you missed, sir: " + lines.joinToString("; ")
    }

    private fun friendlyApp(pkg: String): String {
        val p = pkg.lowercase()
        return when {
            "whatsapp" in p && "w4b" in p -> "WhatsApp Business"
            "whatsapp" in p -> "WhatsApp"
            "telegram" in p -> "Telegram"
            "instagram" in p -> "Instagram"
            "gm" in p || p.endsWith(".gm") -> "Gmail"
            "youtube" in p -> "YouTube"
            else -> pkg.substringAfterLast('.')
        }
    }

    companion object {
        private const val TAG = "JarvisNotif"
        @Volatile var instance: JarvisNotificationListener? = null

        fun summaryOrHelp(limit: Int = 8, appFilter: String? = null): String {
            val i = instance
            return if (i == null) {
                "Notification access is off, sir. Enable Jarvis under Settings → Notification access."
            } else i.summarizeActive(limit, appFilter)
        }
    }
}
