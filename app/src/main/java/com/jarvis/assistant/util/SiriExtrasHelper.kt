package com.jarvis.assistant.util

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.net.Uri
import android.provider.CalendarContract
import android.provider.ContactsContract
import com.jarvis.assistant.accessibility.JarvisAccessibilityService
import java.util.Calendar
import java.util.Locale

/**
 * Remaining Siri-like helpers: calendar create, rewrite, now-playing, share screen, screen actions.
 */
class SiriExtrasHelper(private val context: Context) {

    fun createEventFromSpeech(raw: String?): String {
        val text = raw?.trim().orEmpty()
        if (text.isBlank()) return "What event should I add, sir?"
        val title = extractEventTitle(text)
        val cal = Calendar.getInstance()
        // Defaults: tomorrow 10:00 if no time found
        cal.add(Calendar.DAY_OF_YEAR, 1)
        cal.set(Calendar.HOUR_OF_DAY, 10)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        parseRelativeDay(text, cal)
        parseClock(text, cal)
        val begin = cal.timeInMillis
        cal.add(Calendar.HOUR_OF_DAY, 1)
        val end = cal.timeInMillis
        return try {
            val intent = Intent(Intent.ACTION_INSERT).apply {
                data = CalendarContract.Events.CONTENT_URI
                putExtra(CalendarContract.Events.TITLE, title)
                putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, begin)
                putExtra(CalendarContract.EXTRA_EVENT_END_TIME, end)
                putExtra(CalendarContract.Events.DESCRIPTION, "Created by Jarvis")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            "Opening calendar to save \"$title\", sir. Confirm the time on screen."
        } catch (e: Exception) {
            "Couldn't open calendar: ${e.message}"
        }
    }

    fun rewriteText(mode: String?, source: String?): String {
        val text = source?.trim().orEmpty().ifBlank {
            ClipboardHelper(context).readAloudFriendly().removePrefix("Clipboard:").trim()
        }
        if (text.isBlank() || text.contains("empty", true)) {
            return "Nothing to rewrite. Copy text or say the sentence, sir."
        }
        val m = mode?.lowercase().orEmpty()
        val out = when {
            "formal" in m || "professional" in m -> formalize(text)
            "short" in m || "shorter" in m || "brief" in m -> shorten(text)
            "expand" in m || "longer" in m -> expand(text)
            "friendly" in m || "casual" in m -> "Hey — $text"
            else -> formalize(text)
        }
        try {
            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
            cm.setPrimaryClip(android.content.ClipData.newPlainText("jarvis_rewrite", out))
        } catch (_: Exception) {}
        return "Rewritten: $out"
    }

    fun nowPlaying(): String {
        return try {
            val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val music = am.isMusicActive
            if (!music) return "Nothing appears to be playing right now, sir."
            // Media metadata needs notification listener / MediaSession — give honest status
            "Audio is active on the device, sir. Use next, pause, or play to control it."
        } catch (e: Exception) {
            "Couldn't check media status."
        }
    }

    fun shareScreenText(): String {
        val text = screenText()
        if (text.isBlank()) {
            return "No screen text. Enable Accessibility for Jarvis, sir."
        }
        val clip = if (text.length > 1500) text.take(1500) + "…" else text
        return try {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, clip)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(intent, "Share via Jarvis").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            "Share sheet open with screen text, sir."
        } catch (e: Exception) {
            "Couldn't share: ${e.message}"
        }
    }

    fun addPhoneFromScreen(): String {
        val text = screenText()
        val phone = Regex("""(?:\+?\d[\d\-\s()]{8,}\d)""").find(text)?.value?.replace(Regex("""[^\d+]"""), "")
            ?: return "No phone number found on screen, sir."
        return try {
            val intent = Intent(ContactsContract.Intents.Insert.ACTION).apply {
                type = ContactsContract.RawContacts.CONTENT_TYPE
                putExtra(ContactsContract.Intents.Insert.PHONE, phone)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            "Opening contacts to save $phone, sir."
        } catch (e: Exception) {
            "Couldn't open contacts: ${e.message}"
        }
    }

    fun emailFromScreen(): String {
        val text = screenText()
        val email = Regex("""[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}""").find(text)?.value
            ?: return "No email address on screen, sir."
        return try {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:$email")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            "Composing mail to $email, sir."
        } catch (e: Exception) {
            "Couldn't open mail: ${e.message}"
        }
    }

    fun openLinkFromScreen(): String {
        val text = screenText()
        val url = Regex("""https?://[^\s]+""", RegexOption.IGNORE_CASE).find(text)?.value
            ?: return "No link on screen, sir."
        return try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            "Opening link, sir."
        } catch (e: Exception) {
            "Couldn't open link."
        }
    }

    fun proactiveTip(): String {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val parts = mutableListOf<String>()
        when {
            hour in 5..10 -> parts += "Good morning. A short briefing or calendar check might help."
            hour in 17..21 -> parts += "Evening window — review notifications or set tomorrow's alarm if needed."
            hour >= 22 || hour < 5 -> parts += "Late hours. Consider focus mode if you're winding down."
            else -> parts += "Standing by for commands."
        }
        try {
            val notif = com.jarvis.assistant.notifications.JarvisNotificationListener.summaryOrHelp()
            if (notif.isNotBlank() && !notif.contains("access is off", true) && notif.length < 220) {
                parts += notif
            }
        } catch (_: Exception) {}
        try {
            val cal = CalendarHelper(context).formatBrief(1)
            if (!cal.contains("not granted") && !cal.contains("No upcoming")) parts += cal
        } catch (_: Exception) {}
        return parts.joinToString(" ")
    }

    private fun screenText(): String {
        val svc = JarvisAccessibilityService.instance ?: return ""
        return try {
            svc.getScreenText().replace(Regex("""\s+"""), " ").trim()
        } catch (_: Exception) {
            ""
        }
    }

    private fun extractEventTitle(text: String): String {
        var t = text
        listOf(
            Regex("""(?i)(add|create|schedule|set)\s+(a\s+)?(calendar\s+)?(event|meeting|appointment)\s*(for|about|to)?\s*"""),
            Regex("""(?i)(event|meeting)\s+"""),
            Regex("""(?i)\b(tomorrow|today|monday|tuesday|wednesday|thursday|friday|saturday|sunday)\b"""),
            Regex("""(?i)\b\d{1,2}(:\d{2})?\s*(am|pm)?\b"""),
            Regex("""(?i)\b(at|on|for)\b""")
        ).forEach { t = t.replace(it, " ") }
        t = t.replace(Regex("""\s+"""), " ").trim()
        return t.ifBlank { "Jarvis event" }.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
    }

    private fun parseRelativeDay(text: String, cal: Calendar) {
        val l = text.lowercase()
        when {
            "day after tomorrow" in l -> cal.add(Calendar.DAY_OF_YEAR, 2)
            "tomorrow" in l || "kal" in l -> { /* already +1 default; ensure */ cal.timeInMillis = System.currentTimeMillis(); cal.add(Calendar.DAY_OF_YEAR, 1) }
            "today" in l || "aaj" in l -> {
                cal.timeInMillis = System.currentTimeMillis()
            }
        }
        val days = listOf(
            "sunday" to Calendar.SUNDAY, "monday" to Calendar.MONDAY, "tuesday" to Calendar.TUESDAY,
            "wednesday" to Calendar.WEDNESDAY, "thursday" to Calendar.THURSDAY,
            "friday" to Calendar.FRIDAY, "saturday" to Calendar.SATURDAY
        )
        for ((name, dow) in days) {
            if (name in l) {
                while (cal.get(Calendar.DAY_OF_WEEK) != dow) cal.add(Calendar.DAY_OF_YEAR, 1)
                break
            }
        }
    }

    private fun parseClock(text: String, cal: Calendar) {
        val m = Regex("""(?i)\b(\d{1,2})(?::(\d{2}))?\s*(am|pm)?\b""").find(text) ?: return
        var h = m.groupValues[1].toIntOrNull() ?: return
        val min = m.groupValues[2].toIntOrNull() ?: 0
        val ap = m.groupValues[3].lowercase()
        if (ap == "pm" && h < 12) h += 12
        if (ap == "am" && h == 12) h = 0
        if (ap.isBlank() && h <= 7) h += 12 // bare "5" → 5pm-ish default evening meetings often wrong; keep as 24h if >= 8
        cal.set(Calendar.HOUR_OF_DAY, h.coerceIn(0, 23))
        cal.set(Calendar.MINUTE, min.coerceIn(0, 59))
    }

    private fun formalize(t: String): String {
        val s = t.trim().replaceFirstChar { it.uppercase() }
        return if (s.endsWith(".") || s.endsWith("!") || s.endsWith("?")) {
            "I wanted to share the following: $s Kind regards."
        } else {
            "I wanted to share the following: $s. Kind regards."
        }
    }

    private fun shorten(t: String): String {
        val s = t.replace(Regex("""\s+"""), " ").trim()
        return if (s.length <= 80) s else s.take(77).trimEnd() + "…"
    }

    private fun expand(t: String): String =
        "Regarding this: ${t.trim()}. Please review and let me know if you need anything else."
}
