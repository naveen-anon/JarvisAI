package com.jarvis.assistant.util

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import com.jarvis.ai.controller.ArmorController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar

/**
 * On-demand status briefing — offline-first.
 * Battery + calendar always; weather only when online + key works.
 */
class BriefingHelper(private val context: Context) {

    data class Briefing(
        val text: String,
        val parts: List<String>
    )

    suspend fun build(includeWeather: Boolean = true): Briefing = withContext(Dispatchers.IO) {
        val parts = mutableListOf<String>()
        parts += greeting()

        batteryLine()?.let { parts += it }
        calendarLine()?.let { parts += it }

        if (includeWeather) {
            weatherLine()?.let { parts += it }
        }

        suitLine()?.let { parts += it }

        if (parts.size <= 1) {
            parts += "All systems nominal. No major updates."
        }

        Briefing(text = parts.joinToString(" "), parts = parts)
    }

    private fun greeting(): String {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return when (hour) {
            in 5..11 -> "Good morning."
            in 12..16 -> "Good afternoon."
            in 17..21 -> "Good evening."
            else -> "Hello."
        }
    }

    private fun batteryLine(): String? {
        return try {
            val ifilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            val status = context.registerReceiver(null, ifilter) ?: return null
            val level = status.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = status.getIntExtra(BatteryManager.EXTRA_SCALE, 100).coerceAtLeast(1)
            val pct = (level * 100) / scale
            val charging = status.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ==
                BatteryManager.BATTERY_STATUS_CHARGING ||
                status.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ==
                BatteryManager.BATTERY_STATUS_FULL
            when {
                pct <= 15 && !charging -> "Battery low at $pct percent."
                charging -> "Battery $pct percent, charging."
                else -> "Battery $pct percent."
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun calendarLine(): String? {
        return try {
            val helper = CalendarHelper(context)
            // Prefer a short "next events" API if present; else formatBrief
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
            // Avoid duplicating helper prefixes
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

    private fun suitLine(): String? {
        return try {
            val suit = ArmorController.currentSuit.value
            val short = suit.name.substringBefore("—").substringBefore("-").trim()
            "$short online, mode ${suit.systemMode}."
        } catch (_: Exception) {
            try {
                val id = SettingsManager(context).getActiveSuitId()
                "Active suit $id."
            } catch (_: Exception) {
                null
            }
        }
    }
}
