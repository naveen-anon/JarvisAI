package com.jarvis.assistant.util

object ConversationContext {
    var currentAppPackage: String = ""
    var currentAppName: String = ""
    var lastAppChangeTime: Long = 0L

    fun updateCurrentApp(packageName: String, appName: String) {
        currentAppPackage = packageName
        currentAppName = appName
        lastAppChangeTime = System.currentTimeMillis()
    }

    fun getAppUsageInfo(): String {
        if (currentAppName.isEmpty()) return "No app currently open"
        return "User is currently using $currentAppName ($currentAppPackage)"
    }
}
