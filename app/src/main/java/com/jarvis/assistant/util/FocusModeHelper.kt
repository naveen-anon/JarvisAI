package com.jarvis.assistant.util

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings

/** DND / Focus helpers. Full DND needs policy access on Android. */
class FocusModeHelper(private val context: Context) {

    fun status(): String {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!nm.isNotificationPolicyAccessGranted) {
                return "Focus policy access is off, sir. I can open settings so you can allow Jarvis."
            }
            return when (nm.currentInterruptionFilter) {
                NotificationManager.INTERRUPTION_FILTER_NONE -> "Do Not Disturb is fully on, sir."
                NotificationManager.INTERRUPTION_FILTER_PRIORITY -> "Priority only mode is on, sir."
                NotificationManager.INTERRUPTION_FILTER_ALARMS -> "Alarms-only mode is on, sir."
                else -> "Do Not Disturb is off, sir. Focus mode is clear."
            }
        }
        return "Focus controls need Android 6 or newer, sir."
    }

    fun enableDnd(): String {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!nm.isNotificationPolicyAccessGranted) {
                openPolicySettings()
                return "Please allow notification policy access for Jarvis, then ask again, sir."
            }
            nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY)
            return "Focus mode enabled, sir. Priority interruptions only."
        }
        return "Not supported on this Android version, sir."
    }

    fun disableDnd(): String {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!nm.isNotificationPolicyAccessGranted) {
                openPolicySettings()
                return "Please allow notification policy access for Jarvis, sir."
            }
            nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
            return "Focus mode disabled, sir. All notifications allowed."
        }
        return "Not supported on this Android version, sir."
    }

    private fun openPolicySettings() {
        try {
            val i = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(i)
        } catch (_: Exception) {
        }
    }
}
