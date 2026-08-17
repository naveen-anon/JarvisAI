package com.jarvis.assistant.util

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * On-device long-term memory for J.A.R.V.I.S.
 * Survives process death. Never leaves the device.
 *
 * Stores:
 *  - free-form facts ("my car is black", "I work at X")
 *  - named keys (last_note, favourite_*, etc.)
 *  - a rolling list of remembered facts for cloud context
 */
class PersistentMemory(context: Context) {

    private val prefs = context.getSharedPreferences("jarvis_memory", Context.MODE_PRIVATE)
    private val factsKey = "__facts_list__"
    private val maxFacts = 80

    fun remember(key: String, value: String) {
        prefs.edit().putString(key.lowercase().trim(), value.trim()).apply()
        // Also append to facts timeline when it's a real note
        if (key != factsKey && value.isNotBlank()) {
            appendFact("$key: $value")
        }
    }

    fun recall(key: String): String? =
        prefs.getString(key.lowercase().trim(), null)

    fun forget(key: String) {
        prefs.edit().remove(key.lowercase().trim()).apply()
    }

    fun clear() {
        prefs.edit().clear().apply()
    }

    fun getAll(): Map<String, String> {
        @Suppress("UNCHECKED_CAST")
        return prefs.all
            .filterKeys { it != factsKey }
            .filterValues { it is String } as Map<String, String>
    }

    /** Remember a free-form fact the user stated ("remember that I like tea"). */
    fun rememberFact(fact: String) {
        val clean = fact.trim()
        if (clean.isBlank()) return
        appendFact(clean)
        // Index under a stable key for quick recall of "last fact"
        remember("last_fact", clean)
    }

    fun getFacts(limit: Int = 30): List<String> {
        val raw = prefs.getString(factsKey, null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { arr.getString(it) }.takeLast(limit)
        } catch (_: Exception) {
            emptyList()
        }
    }

    /** Compact block injected into cloud LLM system context. */
    fun buildMemoryContext(userName: String): String {
        val facts = getFacts(25)
        val named = getAll().filterKeys { it != "last_note" && it != "last_fact" }
        if (facts.isEmpty() && named.isEmpty()) {
            return "No long-term memories stored yet about the user."
        }
        return buildString {
            append("User name: ").append(userName.ifBlank { "sir" }).append('\n')
            if (named.isNotEmpty()) {
                append("Named memories:\n")
                named.entries.take(20).forEach { (k, v) ->
                    append("- ").append(k).append(": ").append(v).append('\n')
                }
            }
            if (facts.isNotEmpty()) {
                append("Remembered facts (oldest → newest):\n")
                facts.forEach { append("- ").append(it).append('\n') }
            }
        }.trim()
    }

    private fun appendFact(fact: String) {
        val list = getFacts(maxFacts).toMutableList()
        // Avoid exact duplicates
        if (list.any { it.equals(fact, ignoreCase = true) }) return
        list.add(fact)
        while (list.size > maxFacts) list.removeAt(0)
        val arr = JSONArray()
        list.forEach { arr.put(it) }
        prefs.edit().putString(factsKey, arr.toString()).apply()
    }
}
