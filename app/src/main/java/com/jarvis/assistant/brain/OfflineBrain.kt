package com.jarvis.assistant.brain

import android.content.Context
import android.os.BatteryManager
import com.jarvis.assistant.executor.CommandExecutor
import com.jarvis.assistant.model.AssistantCommand
import com.jarvis.assistant.util.ConversationContext
import com.jarvis.assistant.util.PersistentMemory
import com.jarvis.assistant.util.SettingsManager
import com.jarvis.assistant.util.SmartSuggestionEngine
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Fully offline reasoning core. Handles app control, device settings, contacts,
 * time/date/battery, arithmetic, notes, general chatting, settings, and smart
 * suggestions — WITHOUT ever touching the network.
 *
 * Key features:
 * - Offline first: always tried before cloud
 * - Smart suggestions: proactively suggests actions based on time/behavior
 * - Conversation context: remembers recent chats for contextual responses
 * - Settings management: voice speed, colors, preferences
 * - General chatting: jokes, facts, small talk — keeps working in airplane mode
 */
class OfflineBrain(
    private val context: Context,
    private val executor: CommandExecutor
) {
    private val memory = PersistentMemory(context)
    private val settings = SettingsManager(context)
    private val conversationContext = ConversationContext()
    private val suggestions = SmartSuggestionEngine(context, memory, settings)
    private val autoLearn = AutoLearnEngine(context)

    fun handle(rawText: String): String? {
        val text = rawText.trim()
        if (text.isBlank()) return null
        val cmd = text.lowercase(Locale.getDefault())

        // Add to conversation history
        conversationContext.addMessage(text, isUserInput = true)

        // Settings management
        handleSettings(cmd, text)?.let { return it }

        // Greetings and small talk
        greeting(cmd)?.let { return it }
        smallTalk(cmd)?.let { return it }

        // Info queries
        if (containsAny(cmd, "what time", "current time", "time now") || cmd == "time") {
            return "It's ${timeFormat().format(Date())}."
        }
        if (containsAny(cmd, "what date", "today's date", "what day is it", "current date", "what day")) {
            return "Today is ${dateFormat().format(Date())}."
        }
        if (containsAny(cmd, "battery", "charge left", "battery level", "battery percentage")) {
            return "Battery is at ${batteryLevel()}%."
        }

        // Math
        solveMath(cmd)?.let { return it }

        // Memory and notes
        if (cmd.startsWith("remember ")) {
            val note = text.substringAfter(" ").trim()
            if (note.isBlank()) return "What should I remember?"
            memory.remember("last_note", note)
            conversationContext.addMessage("I'll remember that.", isUserInput = false)
            return "Got it, I'll remember that."
        }
        if (containsAny(cmd, "what did i tell you", "what did you remember", "recall that", "recall my note")) {
            val recalled = memory.recall("last_note") ?: "You haven't told me anything to remember yet."
            conversationContext.addMessage(recalled, isUserInput = false)
            return recalled
        }
        if (containsAny(cmd, "forget that", "forget it", "clear memory")) {
            memory.forget("last_note")
            return "Okay, forgotten."
        }

        // Device commands (apps, calls, SMS, settings toggles)
        toDeviceCommand(cmd, text)?.let { return executor.execute(it) }

        // Help and info
        if (containsAny(cmd, "who are you", "what are you", "your name")) {
            return "I'm Jarvis. I run most commands right here on the device, no internet required. " +
                   "I can chat, remember things, suggest actions, and handle settings too."
        }
        if (containsAny(cmd, "what can you do", "help me", "list commands") || cmd == "help") {
            return "I can open apps, call or text contacts, toggle wifi/bluetooth/flashlight, " +
                   "tell you time/date/battery, do math, remember notes, chat with you, " +
                   "suggest helpful actions, and manage settings — all offline."
        }

        // Smart suggestions (only if we should)
        if (conversationContext.shouldSuggestAction()) {
            val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            suggestions.generateSuggestion(
                hour,
                memory.recall("current_location"),
                conversationContext.getUserMood(),
                conversationContext.getRecentMessages()
            )?.let { return it }
        }

        return null
    }

    fun canHandleOffline(rawText: String): Boolean = handle(rawText) != null

    /** Settings management — voice, colors, preferences, etc. */
    private fun handleSettings(cmd: String, original: String): String? = when {
        // Voice settings
        containsAny(cmd, "change voice", "set voice") -> {
            val voiceType = when {
                "male" in cmd -> { settings.setVoiceType("male"); "male" }
                "female" in cmd -> { settings.setVoiceType("female"); "female" }
                "robot" in cmd -> { settings.setVoiceType("robot"); "robot" }
                else -> settings.getVoiceType()
            }
            "Voice changed to $voiceType."
        }

        containsAny(cmd, "voice speed", "speak faster", "speak slower") -> {
            val speed = when {
                "faster" in cmd || "speed up" in cmd -> { settings.setVoiceSpeed(1.5f); 1.5f }
                "slower" in cmd || "slow down" in cmd -> { settings.setVoiceSpeed(0.8f); 0.8f }
                else -> settings.getVoiceSpeed()
            }
            "Voice speed set to $speed."
        }

        // Arc Reactor color customization
        containsAny(cmd, "change color", "set color", "reactor color") -> {
            val color = when {
                "blue" in cmd -> { settings.setArcReactorColor("#00D4FF"); "blue" }
                "red" in cmd -> { settings.setArcReactorColor("#FF0066"); "red" }
                "green" in cmd -> { settings.setArcReactorColor("#00FF00"); "green" }
                "yellow" in cmd -> { settings.setArcReactorColor("#FFFF00"); "yellow" }
                "orange" in cmd -> { settings.setArcReactorColor("#FF8800"); "orange" }
                "purple" in cmd -> { settings.setArcReactorColor("#9900FF"); "purple" }
                else -> settings.getArcReactorColor()
            }
            "Arc reactor color changed to $color."
        }

        containsAny(cmd, "glow", "intensity", "brightness") -> {
            val intensity = when {
                "increase" in cmd || "brighter" in cmd -> { settings.setGlowIntensity(1.5f); 1.5f }
                "decrease" in cmd || "dimmer" in cmd -> { settings.setGlowIntensity(0.7f); 0.7f }
                else -> settings.getGlowIntensity()
            }
            "Glow intensity set to ${(intensity * 100).toInt()}%."
        }

        // Theme
        containsAny(cmd, "dark mode", "light mode", "change theme") -> {
            val theme = when {
                "dark" in cmd -> { settings.setTheme("dark"); "dark" }
                "light" in cmd -> { settings.setTheme("light"); "light" }
                else -> settings.getTheme()
            }
            "Theme changed to $theme."
        }

        // User name
        cmd.startsWith("my name is ") -> {
            val name = original.substringAfter("is ").trim()
            settings.setUserName(name)
            conversationContext.addUserPreference("name", name)
            "Nice to meet you, $name."
        }

        // Suggestions toggle
        containsAny(cmd, "disable suggestions", "turn off suggestions") -> {
            settings.setSmartSuggestionsEnabled(false)
            "Smart suggestions disabled."
        }
        containsAny(cmd, "enable suggestions", "turn on suggestions") -> {
            settings.setSmartSuggestionsEnabled(true)
            "Smart suggestions enabled."
        }

        // Show current settings
        containsAny(cmd, "show settings", "what are my settings", "settings") -> {
            val userName = settings.getUserName()
            val voiceType = settings.getVoiceType()
            val voiceSpeed = settings.getVoiceSpeed()
            val color = settings.getArcReactorColor()
            val suggestionsOn = if (settings.getSmartSuggestionsEnabled()) "on" else "off"
            "Settings: User is $userName, voice is $voiceType at $voiceSpeed speed, " +
                    "reactor color is $color, suggestions are $suggestionsOn."
        }

        else -> null
    }

    /** General chatting and small talk — works offline for casual conversation */
    private fun smallTalk(cmd: String): String? = when {
        // Jokes
        containsAny(cmd, "tell me a joke", "make me laugh", "tell a joke", "joke") -> {
            listOf(
                "Why don't scientists trust atoms? Because they make up everything!",
                "I told my computer I needed a break, and now it won't stop sending me Kit-Kat ads.",
                "Why did the AI go to school? To improve its learning rate!",
                "What do you call an AI that's always late? A slow processor.",
                "Why do programmers prefer dark mode? Because light attracts bugs!"
            ).random()
        }

        // Facts
        containsAny(cmd, "tell me a fact", "interesting fact", "fact", "did you know") -> {
            listOf(
                "Honey never spoils. Archaeologists have found 3000-year-old honey in Egyptian tombs that was still edible.",
                "Bananas are berries, but strawberries aren't technically berries.",
                "A group of flamingos is called a 'flamboyance.'",
                "Octopuses have three hearts: two pump blood to the gills, one to the rest of the body.",
                "Cleopatra lived closer to the invention of the iPhone than to the construction of the Great Pyramid."
            ).random()
        }

        // Mood check
        containsAny(cmd, "how am i feeling", "what's my mood", "my mood") -> {
            "You seem ${conversationContext.getUserMood()} based on our recent chat."
        }

        // Conversation recap
        containsAny(cmd, "what did we talk about", "what have we discussed", "recap") -> {
            val recent = conversationContext.getRecentMessages(3)
                .filter { !it.isUserInput }
                .map { it.text }
            if (recent.isEmpty()) "We haven't discussed much yet."
            else "We talked about: ${recent.joinToString("; ")}"
        }

        else -> null
    }

    private fun greeting(cmd: String): String? = when {
        cmd == "hi" || cmd == "hello" || cmd == "hey" || cmd == "hey jarvis" || cmd == "yo" ->
            "Hello! How can I help you?"
        containsAny(cmd, "good morning") ->
            "Good morning, ${settings.getUserName()}. Today is ${dateFormat().format(Date())}, " +
                    "it's ${timeFormat().format(Date())}, and battery is at ${batteryLevel()}%. " +
                    "Ready to assist."
        containsAny(cmd, "good night", "good evening") -> "Good night. Sleep well."
        containsAny(cmd, "thank you", "thanks") -> "Anytime."
        containsAny(cmd, "how are you") -> "Running at full capacity. How about you?"
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
