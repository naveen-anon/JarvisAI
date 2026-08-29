package com.jarvis.assistant.util

import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CalendarHelper(private val context: Context) {

    data class Event(val title: String, val startMs: Long, val location: String?)

    fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_CALENDAR) ==
            PackageManager.PERMISSION_GRANTED

    fun nextEvents(limit: Int = 2): List<Event> {
        if (!hasPermission()) return emptyList()
        val now = System.currentTimeMillis()
        val end = now + 7L * 24 * 60 * 60 * 1000
        val builder = CalendarContract.Instances.CONTENT_URI.buildUpon()
        ContentUris.appendId(builder, now)
        ContentUris.appendId(builder, end)
        val events = mutableListOf<Event>()
        try {
            context.contentResolver.query(
                builder.build(),
                arrayOf(
                    CalendarContract.Instances.TITLE,
                    CalendarContract.Instances.BEGIN,
                    CalendarContract.Instances.EVENT_LOCATION
                ),
                null, null,
                "${CalendarContract.Instances.BEGIN} ASC"
            )?.use { c ->
                val iTitle = c.getColumnIndex(CalendarContract.Instances.TITLE)
                val iBegin = c.getColumnIndex(CalendarContract.Instances.BEGIN)
                val iLoc = c.getColumnIndex(CalendarContract.Instances.EVENT_LOCATION)
                while (c.moveToNext() && events.size < limit) {
                    val title = if (iTitle >= 0) c.getString(iTitle) ?: "Event" else "Event"
                    val begin = if (iBegin >= 0) c.getLong(iBegin) else continue
                    val loc = if (iLoc >= 0) c.getString(iLoc) else null
                    events.add(Event(title, begin, loc))
                }
            }
        } catch (_: Exception) {
            return emptyList()
        }
        return events
    }

    fun formatBrief(limit: Int = 2): String {
        if (!hasPermission()) return "Calendar permission is not granted, sir."
        val events = nextEvents(limit)
        if (events.isEmpty()) return "No upcoming events on the calendar, sir."
        val fmt = SimpleDateFormat("EEE h:mm a", Locale.getDefault())
        return events.joinToString(" ") { e ->
            val whenStr = fmt.format(Date(e.startMs))
            val loc = e.location?.takeIf { it.isNotBlank() }?.let { " at $it" } ?: ""
            "${e.title} at $whenStr$loc."
        }
    }
}
