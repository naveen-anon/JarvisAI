package com.jarvis.assistant.memory

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * Private on-device memory — no cloud, no API key.
 * Stark-style continuity: name, prefs, notes, routines.
 */
class JarvisMemory(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("jarvis_memory_v1", Context.MODE_PRIVATE)

    fun getUserName(): String =
        prefs.getString(KEY_NAME, "")?.trim().orEmpty()

    fun setUserName(name: String) {
        prefs.edit().putString(KEY_NAME, name.trim()).apply()
    }

    fun getPreferredChatApp(): String =
        prefs.getString(KEY_CHAT_APP, "whatsapp")?.trim()?.lowercase().orEmpty().ifBlank { "whatsapp" }

    fun setPreferredChatApp(app: String) {
        prefs.edit().putString(KEY_CHAT_APP, app.trim().lowercase()).apply()
    }

    fun getAddressAs(): String {
        val n = getUserName()
        return if (n.isBlank()) "sir" else n
    }

    /** Free-form facts Jarvis should remember. */
    fun addNote(text: String) {
        val t = text.trim()
        if (t.isBlank()) return
        val arr = notesArray()
        arr.put(
            JSONObject()
                .put("t", t)
                .put("at", System.currentTimeMillis())
        )
        // keep last 40
        while (arr.length() > 40) arr.remove(0)
        prefs.edit().putString(KEY_NOTES, arr.toString()).apply()
    }

    fun listNotes(limit: Int = 10): List<String> {
        val arr = notesArray()
        val out = mutableListOf<String>()
        val start = (arr.length() - limit).coerceAtLeast(0)
        for (i in start until arr.length()) {
            out += arr.getJSONObject(i).optString("t")
        }
        return out.reversed()
    }

    fun clearNotes() {
        prefs.edit().remove(KEY_NOTES).apply()
    }

    fun setRoutine(key: String, value: String) {
        val obj = routinesObject()
        obj.put(key, value)
        prefs.edit().putString(KEY_ROUTINES, obj.toString()).apply()
    }

    fun getRoutine(key: String): String? =
        routinesObject().optString(key, null)?.takeIf { it.isNotBlank() }

    fun summaryForPrompt(): String {
        val parts = mutableListOf<String>()
        val name = getUserName()
        if (name.isNotBlank()) parts += "User prefers to be called $name."
        parts += "Preferred chat app: ${getPreferredChatApp()}."
        getRoutine("morning")?.let { parts += "Morning routine: $it" }
        getRoutine("evening")?.let { parts += "Evening routine: $it" }
        val notes = listNotes(5)
        if (notes.isNotEmpty()) {
            parts += "Recent notes: " + notes.joinToString("; ")
        }
        return parts.joinToString(" ")
    }

    private fun notesArray(): JSONArray {
        val raw = prefs.getString(KEY_NOTES, "[]") ?: "[]"
        return try {
            JSONArray(raw)
        } catch (_: Exception) {
            JSONArray()
        }
    }

    private fun routinesObject(): JSONObject {
        val raw = prefs.getString(KEY_ROUTINES, "{}") ?: "{}"
        return try {
            JSONObject(raw)
        } catch (_: Exception) {
            JSONObject()
        }
    }

    companion object {
        private const val KEY_NAME = "user_name"
        private const val KEY_CHAT_APP = "preferred_chat"
        private const val KEY_NOTES = "notes_json"
        private const val KEY_ROUTINES = "routines_json"
    }
}
