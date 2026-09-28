package com.jarvis.assistant.util

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar

/**
 * Tony-style status briefing — offline-first, business + security aware.
 * No armor/suit references.
 */
class BriefingHelper(private val context: Context) {

    data class Briefing(
        val text: String,
        val parts: List<String>
    )

    suspend fun build(includeWeather: Boolean = true): Briefing = withContext(Dispatchers.IO) {
        val parts = mutableListOf<String>()
        parts += greeting()

        timeLine()?.let { parts += it }
        batteryLine()?.let { parts += it }
        networkLine()?.let { parts += it }
        calendarLine()?.let { parts += it }

        if (includeWeather) {
            weatherLine()?.let { parts += it }
        }

        memoryLine()?.let { parts += it }
        securityLine()?.let { parts += it }

        if (parts.size <= 1) {
            parts += "All systems nominal. No major updates on the board."
        } else {
            parts += "Ready for your orders."
        }

        Briefing(text = parts.joinToString(" "), parts = parts)
    }

    private fun greeting(): String {
        val who = try {
            com.jarvis.assistant.memory.JarvisMemory(context).getAddressAs()
        } catch (_: Exception) {
            try {
                val n = SettingsManager(context).getUserName().trim()
                if (n.isBlank() || n.equals("User", true)) "sir" else n
            } catch (_: Exception) {
                "sir"
            }
        }

        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return when (hour) {
            in 5..11 -> "Good morning, $who."
            in 12..16 -> "Good afternoon, $who."
            in 17..21 -> "Good evening, $who."
            else -> "Hello, $who."
        }
    }

    private fun timeLine(): String? {
        return try {
            val fmt = java.text.SimpleDateFormat("h:mm a", java.util.Locale.getDefault())
            "The time is ${fmt.format(java.util.Date())}."
        } catch (_: Exception) {
            null
        }
    }

    private fun batteryLine(): String? {
        return try {
            val ifilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            val status = context.registerReceiver(null, ifilter) ?: return null
            val level = status.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = status.getIntExtra(BatteryManager.EXTRA_SCALE, 100).coerceAtLeast(1)
            val pct = (level * 100) / scale
            val st = status.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
            val charging = st == BatteryManager.BATTERY_STATUS_CHARGING ||
                st == BatteryManager.BATTERY_STATUS_FULL
            when {
                pct <= 15 && !charging -> "Battery critical at $pct percent — I recommend charging soon."
                pct <= 25 && !charging -> "Battery low at $pct percent."
                charging -> "Battery $pct percent, charging."
                else -> "Battery $pct percent."
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun networkLine(): String? {
        return try {
            val net = NetworkStatusManager(context)
            if (net.isOnline()) "Network online." else "You are offline — local systems only."
        } catch (_: Exception) {
            null
        }
    }

    private fun calendarLine(): String? {
        return try {
            val helper = CalendarHelper(context)
            val brief = try {
                helper.formatBrief(2)
            } catch (_: Exception) {
                null
            }
            if (brief.isNullOrBlank()) return null
            if (brief.contains("no", ignoreCase = true) &&
                brief.contains("event", ignoreCase = true)
            ) {
                return "No upcoming events on your calendar."
            }
            val cleaned = brief.trim().removePrefix("Next:").trim()
            "Next on calendar: $cleaned"
        } catch (_: Exception) {
            null
        }
    }

    private suspend fun weatherLine(): String? {
        return try {
            val net = NetworkStatusManager(context)
            if (!net.isOnline()) return null
            val loc = LocationHelper(context).getCurrentLocation() ?: return null
            val key = try {
                com.jarvis.assistant.BuildConfig.OPENWEATHER_API_KEY
            } catch (_: Exception) {
                return null
            }
            if (key.isBlank() || key == "null") return null
            val weather = WeatherClient(key).getWeather(loc.lat, loc.lon) ?: return null
            val city = loc.cityName?.takeIf { it.isNotBlank() } ?: "your area"
            "Weather in $city: ${weather.tempCelsius} degrees, ${weather.condition}."
        } catch (_: Exception) {
            null
        }
    }

    private fun memoryLine(): String? {
        return try {
            val mem = PersistentMemory(context)
            val note = mem.recall("last_note")?.trim().orEmpty()
            if (note.isBlank()) return null
            val short = if (note.length > 80) note.take(77) + "…" else note
            "Pinned note: $short"
        } catch (_: Exception) {
            null
        }
    }

    /** Defensive security snapshot only — no offensive tooling. */
    private fun securityLine(): String? {
        return try {
            val lock = try {
                val km = context.getSystemService(Context.KEYGUARD_SERVICE) as? android.app.KeyguardManager
                if (km?.isDeviceSecure == true) "device lock is on" else "device lock is not configured"
            } catch (_: Exception) {
                null
            }
            lock?.let { "Security: $it." }
        } catch (_: Exception) {
            null
        }
    }
}
