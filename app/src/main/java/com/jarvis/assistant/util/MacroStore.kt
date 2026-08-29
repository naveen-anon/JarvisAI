package com.jarvis.assistant.util

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * User macros: trigger → steps.
 * Step may include delayMs (wait after previous step).
 */
class MacroStore(context: Context) {

    private val prefs = context.getSharedPreferences("jarvis_macros", Context.MODE_PRIVATE)
    private val key = "macros_json"

    data class MacroStep(
        val action: String,
        val target: String? = null,
        val message: String? = null,
        val delayMs: Long = 0L
    )

    data class Macro(val name: String, val trigger: String, val steps: List<MacroStep>)

    fun list(): List<Macro> {
        val raw = prefs.getString(key, "[]") ?: "[]"
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).mapNotNull { i ->
                val o = arr.getJSONObject(i)
                val stepsArr = o.optJSONArray("steps") ?: JSONArray()
                val steps = (0 until stepsArr.length()).map { j ->
                    val s = stepsArr.getJSONObject(j)
                    MacroStep(
                        action = s.getString("action"),
                        target = s.optString("target", null).takeIf { !it.isNullOrBlank() && it != "null" },
                        message = s.optString("message", null).takeIf { !it.isNullOrBlank() && it != "null" },
                        delayMs = s.optLong("delayMs", 0L)
                    )
                }
                Macro(
                    name = o.optString("name", o.getString("trigger")),
                    trigger = o.getString("trigger").lowercase().trim(),
                    steps = steps
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun saveAll(macros: List<Macro>) {
        val arr = JSONArray()
        macros.forEach { m ->
            val steps = JSONArray()
            m.steps.forEach { s ->
                steps.put(JSONObject().apply {
                    put("action", s.action)
                    put("target", s.target)
                    put("message", s.message)
                    put("delayMs", s.delayMs)
                })
            }
            arr.put(JSONObject().apply {
                put("name", m.name)
                put("trigger", m.trigger.lowercase().trim())
                put("steps", steps)
            })
        }
        prefs.edit().putString(key, arr.toString()).apply()
    }

    fun upsert(macro: Macro) {
        val others = list().filter { it.trigger != macro.trigger.lowercase().trim() }
        saveAll(others + macro.copy(trigger = macro.trigger.lowercase().trim()))
    }

    fun delete(trigger: String) {
        saveAll(list().filter { it.trigger != trigger.lowercase().trim() })
    }

    fun findMatch(speech: String): Macro? {
        val s = speech.lowercase().trim()
        return list().sortedByDescending { it.trigger.length }
            .firstOrNull { s == it.trigger || s.startsWith(it.trigger + " ") || s.contains(it.trigger) }
    }
}
