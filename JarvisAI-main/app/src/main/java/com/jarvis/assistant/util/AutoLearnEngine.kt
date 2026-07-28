package com.jarvis.assistant.util

import android.content.Context
import java.util.Calendar

/**
 * Auto-Learn Mode: Silently tracks user behavior (app usage, call patterns, alarm times, etc.)
 * and learns daily routines. After a week of data, automatically makes smart suggestions
 * without being asked.
 *
 * Examples:
 * - "I notice you check Instagram every Monday at 9 AM"
 * - "You usually code between 10 AM - 1 PM. Want me to set a focus timer?"
 * - "You call your mom every Sunday. Should I remind you?"
 */
class AutoLearnEngine(context: Context) {

    private val prefs = context.getSharedPreferences("jarvis_auto_learn", Context.MODE_PRIVATE)

    // Track app usage patterns: "app_name:hour" -> count
    fun recordAppUsage(appName: String, packageName: String) {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val dayOfWeek = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
        val key = "app_usage:${appName}:${dayOfWeek}:${hour}"
        val count = prefs.getInt(key, 0)
        prefs.edit().putInt(key, count + 1).apply()

        // Also track overall app usage
        val appKey = "app_total:${appName}"
        val appCount = prefs.getInt(appKey, 0)
        prefs.edit().putInt(appKey, appCount + 1).apply()
    }

    // Track call/SMS patterns: "contact_name" -> count
    fun recordCallOrSMS(contactName: String, type: String) {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val dayOfWeek = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
        val key = "contact:${contactName}:${type}:${dayOfWeek}:${hour}"
        val count = prefs.getInt(key, 0)
        prefs.edit().putInt(key, count + 1).apply()

        // Overall contact frequency
        val contactKey = "contact_total:${contactName}"
        val contactCount = prefs.getInt(contactKey, 0)
        prefs.edit().putInt(contactKey, contactCount + 1).apply()
    }

    // Track alarm/timer patterns: "alarm_time" -> count (e.g., "7:30 AM")
    fun recordAlarm(timeStr: String) {
        val key = "alarm:${timeStr}"
        val count = prefs.getInt(key, 0)
        prefs.edit().putInt(key, count + 1).apply()
    }

    // Analyze and suggest based on learned patterns
    fun suggestBasedOnLearning(): String? {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val dayOfWeek = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
        val dayName = getDayName(dayOfWeek)

        // Check if there's a frequent app at this time on this day
        val frequentApp = findFrequentAppAtTime(hour, dayOfWeek)
        if (frequentApp != null && prefs.getInt("app_usage:${frequentApp}:${dayOfWeek}:${hour}", 0) >= 3) {
            return "I notice you usually open $frequentApp on $dayName mornings at this time. Want me to launch it?"
        }

        // Check if there's a frequent contact at this time
        val frequentContact = findFrequentContactAtTime(hour, dayOfWeek)
        if (frequentContact != null) {
            val callCount = prefs.getInt("contact:${frequentContact}:call:${dayOfWeek}:${hour}", 0)
            if (callCount >= 2) {
                return "You usually call $frequentContact on $dayName at this time. Should I dial them?"
            }
        }

        // Check if there's a regular alarm time
        val frequentAlarm = findFrequentAlarmTime()
        if (frequentAlarm != null && hour.toString() in frequentAlarm) {
            return "It's around your usual alarm time. Want me to set one?"
        }

        return null
    }

    // Get most frequent app used at a specific time
    private fun findFrequentAppAtTime(hour: Int, dayOfWeek: Int): String? {
        var maxCount = 0
        var maxApp: String? = null

        prefs.all.forEach { (key, value) ->
            if (key.startsWith("app_usage:") && ":${dayOfWeek}:${hour}" in key && value is Int) {
                if (value > maxCount) {
                    maxCount = value
                    maxApp = key.substringAfter("app_usage:").substringBefore(":${dayOfWeek}")
                }
            }
        }

        return if (maxCount >= 2) maxApp else null  // Need at least 2 occurrences
    }

    // Get most frequent contact contacted at a specific time
    private fun findFrequentContactAtTime(hour: Int, dayOfWeek: Int): String? {
        var maxCount = 0
        var maxContact: String? = null

        prefs.all.forEach { (key, value) ->
            if (key.startsWith("contact:") && ":${dayOfWeek}:${hour}" in key && value is Int) {
                if (value > maxCount) {
                    maxCount = value
                    maxContact = key.substringAfter("contact:").substringBefore(":call").substringBefore(":sms")
                }
            }
        }

        return if (maxCount >= 1) maxContact else null
    }

    // Get most frequent alarm time
    private fun findFrequentAlarmTime(): String? {
        var maxCount = 0
        var maxTime: String? = null

        prefs.all.forEach { (key, value) ->
            if (key.startsWith("alarm:") && value is Int) {
                if (value > maxCount) {
                    maxCount = value
                    maxTime = key.substringAfter("alarm:")
                }
            }
        }

        return if (maxCount >= 3) maxTime else null  // Need at least 3 alarms at same time
    }

    // Get top N apps used overall
    fun getTopApps(limit: Int = 5): List<Pair<String, Int>> {
        return prefs.all
            .filter { (key, _) -> key.startsWith("app_total:") }
            .map { (key, value) ->
                val appName = key.substringAfter("app_total:")
                Pair(appName, (value as? Int) ?: 0)
            }
            .sortedByDescending { it.second }
            .take(limit)
    }

    // Get top N contacts contacted
    fun getTopContacts(limit: Int = 5): List<Pair<String, Int>> {
        return prefs.all
            .filter { (key, _) -> key.startsWith("contact_total:") }
            .map { (key, value) ->
                val contactName = key.substringAfter("contact_total:")
                Pair(contactName, (value as? Int) ?: 0)
            }
            .sortedByDescending { it.second }
            .take(limit)
    }

    // Get daily routine summary
    fun getDailyRoutineSummary(): String {
        val topApps = getTopApps(3)
        val topContacts = getTopContacts(2)

        val appSummary = topApps.joinToString(", ") { "${it.first} (${it.second}x)" }
        val contactSummary = topContacts.joinToString(", ") { "${it.first}" }

        return buildString {
            append("Your routine: You use $appSummary mostly. ")
            if (contactSummary.isNotEmpty()) {
                append("You talk to $contactSummary frequently. ")
            }
            append("Let me keep learning your patterns for better suggestions.")
        }
    }

    // Clear all learned data (privacy reset)
    fun clearAllData() {
        prefs.edit().clear().apply()
    }

    private fun getDayName(dayOfWeek: Int): String = when (dayOfWeek) {
        Calendar.SUNDAY -> "Sunday"
        Calendar.MONDAY -> "Monday"
        Calendar.TUESDAY -> "Tuesday"
        Calendar.WEDNESDAY -> "Wednesday"
        Calendar.THURSDAY -> "Thursday"
        Calendar.FRIDAY -> "Friday"
        Calendar.SATURDAY -> "Saturday"
        else -> "day"
    }
}
