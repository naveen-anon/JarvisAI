package com.jarvis.assistant.brain

import android.content.Context
import android.os.BatteryManager
import com.jarvis.assistant.executor.CommandExecutor
import com.jarvis.assistant.model.AssistantCommand
import com.jarvis.assistant.util.PersistentMemory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Fully offline reasoning core. Handles app control, device settings, contacts,
 * time/date/battery queries, arithmetic, notes, and small talk WITHOUT ever
 * touching the network.
 *
 * Design goal: this should answer the large majority of everyday commands on its
 * own. [handle] returns null only when it genuinely doesn't recognize the request —
 * that's the caller's (AssistantForegroundService's) signal to try Gemini *if and
 * only if* the device is online. Nothing in this class ever throws for a "no match"
 * case, so a fully airplane-mode device never crashes and never hangs waiting on a
 * socket that will never connect.
 */
class OfflineBrain(
    private val context: Context,
    private val executor: CommandExecutor
) {
    private val memory = PersistentMemory(context)

    fun handle(rawText: String): String? {
        val text = rawText.trim()
        if (text.isBlank()) return null
        val cmd = text.lowercase(Locale.getDefault())

        greeting(cmd)?.let { return it }

        if (containsAny(cmd, "what time", "current time", "time now") || cmd == "time") {
            return "It's ${timeFormat().format(Date())}."
        }
        if (containsAny(cmd, "what date", "today's date", "what day is it", "current date", "what day")) {
            return "Today is ${dateFormat().format(Date())}."
        }
        if (containsAny(cmd, "battery", "charge left", "battery level", "battery percentage")) {
            return "Battery is at ${batteryLevel()}%."
        }

        solveMath(cmd)?.let { return it }

        if (cmd.startsWith("remember ")) {
            val note = text.substringAfter(" ").trim()
            if (note.isBlank()) return "What should I remember?"
            memory.remember("last_note", note)
            return "Got it, I'll remember that."
        }
        if (containsAny(cmd, "what did i tell you", "what did you remember", "recall that", "recall my note")) {
            return memory.recall("last_note") ?: "You haven't told me anything to remember yet."
        }
        if (containsAny(cmd, "forget that", "forget it", "clear memory")) {
            memory.forget("last_note")
            return "Okay, forgotten."
        }

        toDeviceCommand(cmd, text)?.let { return executor.execute(it) }

        if (containsAny(cmd, "who are you", "what are you", "your name")) {
            return "I'm Jarvis. I run most commands right here on the device, no internet required."
        }
        if (containsAny(cmd, "what can you do", "help me", "list commands") || cmd == "help") {
            return "I can open apps, call or text contacts, toggle wifi, bluetooth, or the flashlight, " +
                "tell you the time, date, or battery level, do quick math, and remember short notes — " +
                "all fully offline. Anything else I'll send to the cloud model if you're connected."
        }

        return null
    }

    /** Quick heuristic used by the service to log/label whether a reply came from device or cloud. */
    fun canHandleOffline(rawText: String): Boolean = handle(rawText) != null

    private fun greeting(cmd: String): String? = when {
        cmd == "hi" || cmd == "hello" || cmd == "hey" || cmd == "hey jarvis" || cmd == "yo" ->
            "Hello! How can I help you?"
        containsAny(cmd, "good morning") ->
            "Good morning. Today is ${dateFormat().format(Date())}, it's ${timeFormat().format(Date())}, " +
                "and battery is at ${batteryLevel()}%. Standing by."
        containsAny(cmd, "good night") -> "Good night. Sleep well."
        containsAny(cmd, "thank you", "thanks") -> "Anytime."
        containsAny(cmd, "how are you") -> "Running at full capacity and ready to help."
        else -> null
    }

    private fun toDeviceCommand(cmd: String, original: String): AssistantCommand? = when {
        containsAny(cmd, "open camera") -> AssistantCommand("open_app", "Camera")
        containsAny(cmd, "open gallery", "open photos") -> AssistantCommand("open_app", "Gallery")
        containsAny(cmd, "open chrome", "open browser") -> AssistantCommand("open_app", "Chrome")
        containsAny(cmd, "open youtube") -> AssistantCommand("open_app", "YouTube")
        containsAny(cmd, "open whatsapp") -> AssistantCommand("open_app", "WhatsApp")
        containsAny(cmd, "open telegram") -> AssistantCommand("open_app", "Telegram")
        containsAny(cmd, "open settings") -> AssistantCommand("open_app", "Settings")
        containsAny(cmd, "open maps") -> AssistantCommand("open_app", "Maps")
        containsAny(cmd, "flashlight", "torch") -> AssistantCommand("toggle_setting", "flashlight")
        containsAny(cmd, "wifi") -> AssistantCommand("toggle_setting", "wifi")
        containsAny(cmd, "bluetooth") -> AssistantCommand("toggle_setting", "bluetooth")
        containsAny(cmd, "airplane mode") -> AssistantCommand("toggle_setting", "airplane_mode")

        cmd.startsWith("call ") ->
            AssistantCommand("call", original.substringAfter(" ").trim())

        cmd.startsWith("message ") || cmd.startsWith("text ") -> {
            val body = original.substringAfter(" ").trim()
            val target = body.substringBefore(" saying ").trim()
            val message = if (" saying " in body) body.substringAfter(" saying ").trim() else null
            AssistantCommand("send_sms", target, message)
        }

        cmd.startsWith("open ") -> AssistantCommand("open_app", original.substringAfter(" ").trim())
        cmd.startsWith("launch ") -> AssistantCommand("open_app", original.substringAfter(" ").trim())
        cmd.startsWith("start ") -> AssistantCommand("open_app", original.substringAfter(" ").trim())

        else -> null
    }

    private fun batteryLevel(): Int {
        val bm = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        return bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
    }

    /** Matches "12 plus 5", "what is 20 minus 8", "10 times 3", "20 divided by 4", "7 * 6", etc. */
    private fun solveMath(cmd: String): String? {
        val regex = Regex(
            "(-?\\d+(?:\\.\\d+)?)\\s*(plus|minus|times|multiplied by|divided by|\\+|-|\\*|x|/)\\s*(-?\\d+(?:\\.\\d+)?)"
        )
        val match = regex.find(cmd) ?: return null
        val (aStr, op, bStr) = match.destructured
        val a = aStr.toDoubleOrNull() ?: return null
        val b = bStr.toDoubleOrNull() ?: return null

        val result = when (op) {
            "plus", "+" -> a + b
            "minus", "-" -> a - b
            "times", "multiplied by", "*", "x" -> a * b
            "divided by", "/" -> if (b != 0.0) a / b else return "Can't divide by zero."
            else -> return null
        }
        val formatted = if (result == result.toLong().toDouble()) result.toLong().toString() else result.toString()
        return "That's $formatted"
    }

    private fun timeFormat() = SimpleDateFormat("hh:mm a", Locale.getDefault())
    private fun dateFormat() = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault())

    private fun containsAny(text: String, vararg needles: String) = needles.any { text.contains(it) }
}
