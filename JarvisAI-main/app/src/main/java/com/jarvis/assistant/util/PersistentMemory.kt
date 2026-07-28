package com.jarvis.assistant.util

import android.content.Context

/**
 * Small on-device key/value memory for the offline brain ("remember X" / "what did I tell you").
 * Backed by SharedPreferences instead of an in-memory Map so notes survive process death —
 * the old JarvisBrain implementation kept a mutableMapOf() that was wiped every time Android
 * killed the foreground service, which is exactly the kind of thing Doze/battery optimization
 * does regularly. Nothing here ever touches the network.
 */
class PersistentMemory(context: Context) {

    private val prefs = context.getSharedPreferences("jarvis_memory", Context.MODE_PRIVATE)

    fun remember(key: String, value: String) {
        prefs.edit().putString(key.lowercase(), value).apply()
    }

    fun recall(key: String): String? = prefs.getString(key.lowercase(), null)

    fun forget(key: String) {
        prefs.edit().remove(key.lowercase()).apply()
    }

    fun clear() {
        prefs.edit().clear().apply()
    }

    fun getAll(): Map<String, String> {
        @Suppress("UNCHECKED_CAST")
        return prefs.all.filterValues { it is String } as Map<String, String>
    }
}
