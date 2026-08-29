package com.jarvis.assistant.util

import android.content.Context
import org.json.JSONArray

class CommandHistoryStore(context: Context) {
    private val prefs = context.getSharedPreferences("jarvis_cmd_history", Context.MODE_PRIVATE)
    private val key = "items"
    private val max = 20

    fun add(command: String) {
        val c = command.trim()
        if (c.isBlank()) return
        val list = list().toMutableList()
        list.removeAll { it.equals(c, true) }
        list.add(0, c)
        while (list.size > max) list.removeAt(list.lastIndex)
        val arr = JSONArray()
        list.forEach { arr.put(it) }
        prefs.edit().putString(key, arr.toString()).apply()
    }

    fun list(): List<String> {
        val raw = prefs.getString(key, "[]") ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { arr.getString(it) }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun last(): String? = list().firstOrNull()
}
