package com.jarvis.assistant.accessibility

import android.accessibilityservice.AccessibilityService
import android.content.pm.PackageManager
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.jarvis.assistant.util.ConversationContext
import com.jarvis.assistant.voice.TextToSpeechHelper

class JarvisAccessibilityService : AccessibilityService() {

    private var lastAppPackage: String = ""
    private val TAG = "JarvisAccessibility"

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val packageName = event.packageName?.toString() ?: return

            // Same app repeat trigger ignore karo aur khud Jarvis app ko bhi
            if (packageName != lastAppPackage && packageName != this.packageName) {
                lastAppPackage = packageName
                val appName = getAppName(packageName) ?: return

                Log.d(TAG, "App Detected: $appName ($packageName)")

                // 1. Context Update
                ConversationContext.updateCurrentApp(packageName, appName)

                // 2. Real-time Reaction
                onAppDetected(appName, packageName)
            }
        }
    }

    private fun getAppName(packageName: String): String? {
        return try {
            val pm = packageManager
            val info = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(info).toString()
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }
    }

    private fun onAppDetected(appName: String, packageName: String) {
        // App ke hisaab se custom reactions
        when (packageName) {
            "com.whatsapp" -> {
                TextToSpeechHelper.speak(this, "WhatsApp open hua hai, Boss.")
            }
            "com.google.android.youtube" -> {
                TextToSpeechHelper.speak(this, "YouTube active ho gaya hai.")
            }
            else -> {
                // Kisi bhi doosre app ke liye general background alert (Optional)
                Log.i(TAG, "Jarvis is tracking: $appName")
            }
        }
    }

    override fun onInterrupt() {
        Log.w(TAG, "Accessibility Service Interrupted")
    }
}
