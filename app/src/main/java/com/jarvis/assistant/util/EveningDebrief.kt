package com.jarvis.assistant.util

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import java.util.Calendar

/**
 * Stark L7 — end-of-day debrief + quiet hours.
 * Local only; pairs with morning BriefingHelper.
 */
class EveningDebrief(private val context: Context) {

    fun isQuietHours(now: Calendar = Calendar.getInstance()): Boolean {
        val settings = SettingsManager(context)
        val enabled = try {
            settings.getQuietHoursEnabled()
        } catch (_: Exception) {
            true // default on
        }
        if (!enabled) return false
        val start = try { settings.getQuietStartHour() } catch (_: Exception) { 22 }
        val end = try { settings.getQuietEndHour() } catch (_: Exception) { 7 }
        val hour = now.get(Calendar.HOUR_OF_DAY)
        return if (start > end) {
            // e.g. 22 → 7
            hour >= start || hour < end
        } else {
            hour in start until end
        }
    }

    /** True if proactive / non-urgent speech should be suppressed. */
    fun shouldSuppressProactive(): Boolean = isQuietHours()

    fun buildDebrief(): String {
        val parts = mutableListOf<String>()
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val who = try {
            com.jarvis.assistant.memory.JarvisMemory(context).getAddressAs()
        } catch (_: Exception) {
            "sir"
        }
        parts += when {
            hour >= 21 || hour < 5 -> "Good evening, $who."
            hour >= 17 -> "End of day summary, $who."
            else -> "Status debrief, $who."
        }

        batteryLine()?.let { parts += it }

        try {
            val cal = CalendarHelper(context).formatBrief(3)
            if (!cal.isNullOrBlank() &&
                !(cal.contains("no", true) && cal.contains("event", true))
            ) {
                parts += "Still on the calendar: $cal"
            } else {
                parts += "No remaining calendar events."
            }
        } catch (_: Exception) {}

        try {
            val suit = SettingsManager(context).getActiveSuitId()
            parts += "Active suit $suit."
        } catch (_: Exception) {}

        try {
            val notes = com.jarvis.assistant.memory.JarvisMemory(context).listNotes(3)
            if (notes.isNotEmpty()) {
                parts += "Open notes: " + notes.joinToString("; ")
            }
        } catch (_: Exception) {}

        if (isQuietHours()) {
            parts += "Quiet hours are active. I will keep non-urgent alerts down."
        } else {
            parts += "Systems standing by for tomorrow."
        }

        return parts.joinToString(" ")
    }

    private fun batteryLine(): String? {
        return try {
            val s = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
                ?: return null
            val level = s.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = s.getIntExtra(BatteryManager.EXTRA_SCALE, 100).coerceAtLeast(1)
            val pct = (level * 100) / scale
            val charging = s.getIntExtra(BatteryManager.EXTRA_STATUS, -1).let {
                it == BatteryManager.BATTERY_STATUS_CHARGING || it == BatteryManager.BATTERY_STATUS_FULL
            }
            when {
                pct <= 20 && !charging -> "Battery is low at $pct percent. Consider charging overnight."
                charging -> "Battery $pct percent, on charge."
                else -> "Battery $pct percent."
            }
        } catch (_: Exception) {
            null
        }
    }
}
