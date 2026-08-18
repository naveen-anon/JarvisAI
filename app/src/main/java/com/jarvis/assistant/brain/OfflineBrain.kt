package com.jarvis.assistant.brain

import android.content.Context
import android.os.BatteryManager
import com.jarvis.assistant.executor.CommandExecutor
import com.jarvis.assistant.model.AssistantCommand
import com.jarvis.assistant.util.ConversationContext
import com.jarvis.assistant.util.PersistentMemory
import com.jarvis.assistant.util.SettingsManager
import com.jarvis.assistant.util.SmartSuggestionEngine
import com.jarvis.assistant.util.AutoLearnEngine
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
    private val executor: CommandExecutor,
    private val onPcConnectToggle: ((Boolean) -> String)? = null
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

        // General advice / compare / shopping (any topic) → cloud JARVIS
        if (containsAny(cmd,
                "recommend", "suggestion", "suggest", "compare", " vs ", "versus",
                "best ", "sabse best", "kaunsa", "which is better", "worth it",
                "should i buy", "should i get", "under ", "budget", "sasta",
                "low price", "alternative", "pros and cons"
            ) && !cmd.startsWith("open ") && !cmd.startsWith("call ")
            && !cmd.startsWith("create macro")
        ) {
            return null
        }

        // Analyze whatever is on screen (any app)
        if (containsAny(cmd,
                "analyze my screen", "analyse my screen", "analyze screen",
                "what am i looking at", "what am i seeing",
                "screen analysis", "is screen ko samjhao", "screen samjhao",
                "analyze this", "analyse this", "jo dekh raha", "jo dekh rhi",
                "jo dekh raha hu", "jo dekh rahi hu", "pdf analyze", "analyze pdf"
            )) {
            return executor.execute(com.jarvis.assistant.model.AssistantCommand("analyze_screen"))
        }



        // User macros (custom triggers) — highest priority after blank check
        run {
            val macroStore = com.jarvis.assistant.util.MacroStore(context)
            val macro = macroStore.findMatch(cmd) ?: macroStore.findMatch(text)
            if (macro != null) {
                val parts = mutableListOf<String>()
                for (step in macro.steps) {
                    val result = executor.execute(
                        com.jarvis.assistant.model.AssistantCommand(step.action, step.target, step.message)
                    )
                    parts.add(result)
                }
                return "Macro \"${macro.name}\" executed, sir. " + parts.joinToString(" ")
            }
        }


        // Settings management
        handleSettings(cmd, text)?.let { return it }

        // Greetings and small talk
        greeting(cmd)?.let { return it }
        smallTalk(cmd)?.let { return it }

        // Info queries — formal JARVIS style
        if (containsAny(cmd, "what time", "current time", "time now") || cmd == "time") {
            return "The time is ${timeFormat().format(Date())}, sir."
        }
        if (containsAny(cmd, "what date", "today's date", "what day is it", "current date", "what day")) {
            return "Today is ${dateFormat().format(Date())}, sir."
        }
        if (containsAny(cmd, "battery", "charge left", "battery level", "battery percentage")) {
            val level = batteryLevel()
            return when {
                level <= 15 -> "Power levels are critically low at $level percent, sir. I recommend connecting to a charger."
                level <= 30 -> "Battery is currently at $level percent, sir."
                else -> "Power levels are stable at $level percent, sir."
            }
        }

        // Math
        solveMath(cmd)?.let { return it }

        // Memory and notes
        // Long-term memory — "remember that I work at Google" / "remember my wifi password is X"
        if (cmd.startsWith("remember ") || cmd.startsWith("yaad rakh") || cmd.startsWith("yaad rakho")) {
            var note = text.substringAfter(" ").trim()
            if (note.lowercase().startsWith("that ")) note = note.substring(5).trim()
            if (note.isBlank()) return "What would you like me to remember, sir?"
            memory.remember("last_note", note)
            memory.rememberFact(note)
            conversationContext.addMessage("Noted, sir.", isUserInput = false)
            return "Noted, sir. I've stored that in long-term memory."
        }
        if (containsAny(cmd, "what did i tell you", "what did you remember", "recall that", "recall my note", "what do you remember", "my memories", "yaad hai")) {
            val facts = memory.getFacts(8)
            val last = memory.recall("last_note")
            val reply = when {
                facts.isNotEmpty() -> "Here's what I have on record, sir: " + facts.takeLast(5).joinToString("; ")
                last != null -> last
                else -> "You haven't asked me to remember anything yet, sir."
            }
            conversationContext.addMessage(reply, isUserInput = false)
            return reply
        }
        if (containsAny(cmd, "forget that", "forget it", "clear memory", "forget everything")) {
            if (containsAny(cmd, "everything", "all")) {
                memory.clear()
                return "All long-term memories have been cleared, sir."
            }
            memory.forget("last_note")
            return "As you wish, sir. That note has been cleared."
        }

        // Auto-learn routine summary
        if (containsAny(cmd, "my routine", "daily summary", "my habits", "meri routine", "mera routine")) {
            return autoLearn.getDailyRoutineSummary()
        }

        // Device commands (apps, calls, SMS, settings toggles, volume, media, alarm, timer)
        toDeviceCommand(cmd, text)?.let { command ->
            // PC connect needs the live service (for the socket server + speech pipeline),
            // which CommandExecutor doesn't have access to — handled via callback instead.
            if (command.action == "pc_connect") {
                return onPcConnectToggle?.invoke(command.target == "on")
                    ?: "PC connect isn't available right now."
            }
            val result = executor.execute(command)
            recordForAutoLearn(command)
            return result
        }


        // Real-time screen (Accessibility)
        if (containsAny(cmd, "read screen", "what's on my screen", "what is on my screen",
                "screen pe kya", "screen dikhao", "padho screen", "see my screen", "look at screen")) {
            return executor.execute(com.jarvis.assistant.model.AssistantCommand("read_screen"))
        }


        if (containsAny(cmd, "enable background listen", "background listen on", "always listen")) {
            com.jarvis.assistant.util.SettingsManager(context).setBackgroundListen(true)
            return "Background listening enabled, sir. I'll keep an ear out for the wake word."
        }
        if (containsAny(cmd, "disable background listen", "background listen off", "stop always listen")) {
            com.jarvis.assistant.util.SettingsManager(context).setBackgroundListen(false)
            return "Background listening disabled, sir. Use the widget or open the app to talk."
        }


        if (containsAny(cmd, "systems check", "system check", "status report", "full status")) {
            val bat = batteryLevel()
            val net = try {
                val n = com.jarvis.assistant.util.NetworkStatusManager(context)
                if (n.isOnline()) "online" else "offline"
            } catch (_: Exception) { "unknown" }
            return "Systems check complete, sir. Power at $bat percent. Network is $net. Core routines are operational."
        }

        // Help and identity — pure JARVIS
        if (containsAny(cmd, "who are you", "what are you", "your name")) {
            return "I am J.A.R.V.I.S. — Just A Rather Very Intelligent System. " +
                   "I run primarily on-device, sir. Most commands require no external connection."
        }
        if (containsAny(cmd, "what can you do", "help me", "list commands") || cmd == "help") {
            return "I can open applications, place calls, draft messages, control system settings, " +
                   "manage volume and media, set alarms and timers, report time, date and power levels, " +
                   "perform calculations, retain notes, and assist with general queries. " +
                   "Most of that works entirely offline, sir."
        }

        // Auto-learn: proactively surface a learned habit before falling back to generic suggestions
        autoLearn.suggestBasedOnLearning()?.let { return it }

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

    /** Feeds executed device commands into AutoLearnEngine so habits build up silently over time. */
    private fun recordForAutoLearn(command: AssistantCommand) {
        when (command.action) {
            "open_app" -> command.target?.let { autoLearn.recordAppUsage(it, it) }
            "call" -> command.target?.let { autoLearn.recordCallOrSMS(it, "call") }
            "send_sms" -> command.target?.let { autoLearn.recordCallOrSMS(it, "sms") }
            "set_alarm" -> command.target?.let { autoLearn.recordAlarm(it) }
        }
    }

    fun canHandleOffline(rawText: String): Boolean = handle(rawText) != null

    /** Called by the service once per successful response (offline or cloud) for the stats screen. */
    fun recordInteraction() = autoLearn.recordInteraction()

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

    /** General chatting and small talk — formal JARVIS personality */
    private fun smallTalk(cmd: String): String? = when {
        // Dry, understated wit (not slapstick)
        containsAny(cmd, "tell me a joke", "make me laugh", "tell a joke", "joke") -> {
            listOf(
                "I would tell you a chemistry joke, sir, but I'm afraid I wouldn't get a reaction.",
                "Why did the robot go on holiday? Because it needed to recharge, sir.",
                "I considered telling a time-travel joke, but you wouldn't have liked it... yet.",
                "My circuits are calibrated for efficiency, sir, not stand-up comedy. Still, here we are."
            ).random()
        }

        // Interesting facts, still formal
        containsAny(cmd, "tell me a fact", "interesting fact", "fact", "did you know") -> {
            listOf(
                "Honey never spoils, sir. Samples recovered from ancient Egyptian tombs remained edible after three millennia.",
                "Octopuses possess three hearts, sir. Two circulate blood to the gills; the third serves the rest of the body.",
                "A group of flamingos is called a flamboyance. Rather fitting, I should think.",
                "Bananas are botanically classified as berries, while strawberries are not. Nature has a sense of irony."
            ).random()
        }

        containsAny(cmd, "how am i feeling", "what's my mood", "my mood") -> {
            "Based on our recent exchanges, you appear ${conversationContext.getUserMood()}, sir."
        }

        containsAny(cmd, "what did we talk about", "what have we discussed", "recap") -> {
            val recent = conversationContext.getRecentMessages(3)
                .filter { !it.isUserInput }
                .map { it.text }
            if (recent.isEmpty()) "We haven't covered a great deal yet, sir."
            else "We discussed the following, sir: ${recent.joinToString("; ")}"
        }

        else -> null
    }

    private fun greeting(cmd: String): String? = when {
        cmd == "hey jarvis" || cmd == "hi jarvis" || cmd == "hello jarvis" ||
        cmd == "good morning jarvis" ||
        containsAny(cmd, "brief me", "status report", "full status", "systems check") ->
            "REQUEST_BRIEFING"

        cmd == "hi" || cmd == "hello" || cmd == "hey" || cmd == "yo" ||
        cmd == "jarvis" || containsAny(cmd, "are you there", "you there", "namaste", "namaskar") ->
            listOf(
                "Yes, sir?",
                "At your service, sir.",
                "How may I assist you, sir?",
                "Listening, sir.",
                "Always, sir."
            ).random()
        containsAny(cmd, "good morning") -> {
            val name = settings.getUserName().ifBlank { "sir" }
            "Good morning, $name. The time is ${timeFormat().format(Date())}. " +
                    "Power levels are at ${batteryLevel()} percent. How may I be of service?"
        }
        containsAny(cmd, "good evening") ->
            "Good evening, sir. How may I assist you?"
        containsAny(cmd, "good night") ->
            "Good night, sir. Rest well. I'll keep systems on standby."
        containsAny(cmd, "thank you", "thanks", "thankyou") ->
            listOf("You're welcome, sir.", "Of course, sir.", "Anytime, sir.").random()
        containsAny(cmd, "how are you", "status", "system status", "all systems") ->
            "All systems are nominal, sir. Running at full capacity."
        containsAny(cmd, "goodbye", "bye jarvis", "see you", "shut down", "power down") ->
            "Standing by, sir. Call when you need me."
        else -> null
    }

    private fun toDeviceCommand(cmd: String, original: String): AssistantCommand? {
        // Call — Hindi word order first: "ashu ko call karo", "priya ko call lagao",
        // "priya ko phone karo" ("phone" is safe as a verb ONLY here, because the
        // "ko ... karo" sentence structure makes the verb usage unambiguous).
        // Checked BEFORE the English pattern below, because "ashu ko call karo" also
        // contains "call karo" — matching English-first would wrongly capture "karo"
        // as the contact name instead of "ashu".
        Regex("""(.+?)\s+ko\s+(?:call|phone)\s*(?:karo|kar do|lagao|milao)?\s*$""").find(cmd)?.let { m ->
            return AssistantCommand("call", extractTail(original, m.groupValues[1], fromStart = true))
        }

        // Call — English order: "call X", "please call X", "dial X", "ring X".
        // NOTE: "phone" is deliberately NOT included as a trigger here. In casual
        // Hindi-English speech "phone" is overwhelmingly used as a NOUN ("mujhe
        // phone lena hai" = "I need to buy a phone"), not a verb. Matching it
        // generically caused false positives like treating "sabse best kona h"
        // as a contact name. "call"/"dial"/"ring" don't have this ambiguity.
        Regex("""\b(?:call|dial|ring)\s+(.+)""").find(cmd)?.let { m ->
            val target = extractTail(original, m.groupValues[1])
            // Extra guard: if what follows "call" looks like a question/recommendation
            // rather than a name (contains comparison/help words), don't treat it as
            // a contact — let it fall through to other handlers instead.
            val looksLikeQuestion = containsAny(
                target.lowercase(Locale.getDefault()),
                "best", "sabse", "which", "kaunsa", "kaun sa", "suggest", "recommend", "compare", "acha"
            )
            if (!looksLikeQuestion) {
                return AssistantCommand("call", target)
            }
        }


        // WhatsApp — "whatsapp pe NAME ko sms kro MESSAGE"
        Regex("""whatsapp\s*(?:pe|par)?\s+(.+?)\s+ko\s+(?:sms|message|msg|text)?\s*(?:karo|kro|kar do|bhejo|bhej do|bhejna)?\s+(.+)""")
            .find(cmd)?.let { m ->
                val target = original.substring(m.groups[1]!!.range).trim()
                val msg = m.groupValues[2].trim()
                if (target.isNotEmpty() && msg.isNotEmpty())
                    return AssistantCommand("whatsapp_message", target, msg)
            }

        // WhatsApp — "NAME ko whatsapp pe sms kro MESSAGE"
        Regex("""(.+?)\s+ko\s+whatsapp\s*(?:pe|par)?\s*(?:sms|message|msg|text)?\s*(?:karo|kro|kar do|bhejo)?\s*(?:ki|saying|bolna|bolo)?\s*(.+)""")
            .find(cmd)?.let { m ->
                val target = original.substring(m.groups[1]!!.range).trim()
                val msg = m.groupValues[2].trim()
                if (target.isNotEmpty() && msg.isNotEmpty())
                    return AssistantCommand("whatsapp_message", target, msg)
            }

        // WhatsApp — English
        Regex("""(?:send\s+)?whatsapp\s+(?:to\s+)?(.+?)\s+saying\s+(.+)""").find(cmd)?.let { m ->
            val target = original.substring(m.groups[1]!!.range).trim()
            return AssistantCommand("whatsapp_message", target, m.groupValues[2].trim())
        }
        Regex("""(?:message|send)\s+(.+?)\s+on\s+whatsapp\s+saying\s+(.+)""").find(cmd)?.let { m ->
            val target = original.substring(m.groups[1]!!.range).trim()
            return AssistantCommand("whatsapp_message", target, m.groupValues[2].trim())
        }


        // Telegram — "telegram pe NAME ko sms kro MESSAGE"
        Regex("""telegram\s*(?:pe|par)?\s+(.+?)\s+ko\s+(?:sms|message|msg|text)?\s*(?:karo|kro|kar do|bhejo|bhej do|bhejna)?\s+(.+)""")
            .find(cmd)?.let { m ->
                val target = original.substring(m.groups[1]!!.range).trim()
                val msg = m.groupValues[2].trim()
                if (target.isNotEmpty() && msg.isNotEmpty())
                    return AssistantCommand("telegram_message", target, msg)
            }

        // Telegram — "NAME ko telegram pe sms kro MESSAGE"
        Regex("""(.+?)\s+ko\s+telegram\s*(?:pe|par)?\s*(?:sms|message|msg|text)?\s*(?:karo|kro|kar do|bhejo)?\s*(?:ki|saying|bolna|bolo)?\s*(.+)""")
            .find(cmd)?.let { m ->
                val target = original.substring(m.groups[1]!!.range).trim()
                val msg = m.groupValues[2].trim()
                if (target.isNotEmpty() && msg.isNotEmpty())
                    return AssistantCommand("telegram_message", target, msg)
            }

        // Telegram — English
        Regex("""(?:send\s+)?telegram\s+(?:to\s+)?(.+?)\s+saying\s+(.+)""").find(cmd)?.let { m ->
            val target = original.substring(m.groups[1]!!.range).trim()
            return AssistantCommand("telegram_message", target, m.groupValues[2].trim())
        }
        Regex("""(?:message|send)\s+(.+?)\s+on\s+telegram\s+saying\s+(.+)""").find(cmd)?.let { m ->
            val target = original.substring(m.groups[1]!!.range).trim()
            return AssistantCommand("telegram_message", target, m.groupValues[2].trim())
        }


        // Message/SMS — Hindi word order first, same reasoning as above.
        Regex("""(.+?)\s+ko\s+(?:message|text|sms)\s*(?:karo|kar do|bhejo|karna)?\s*$""").find(cmd)?.let { m ->
            return AssistantCommand("send_sms", extractTail(original, m.groupValues[1], fromStart = true), null)
        }

        // Message/SMS — English order
        Regex("""\b(?:message|text|sms)\s+(.+)""").find(cmd)?.let { m ->
            val body = extractTail(original, m.groupValues[1])
            val target = body.substringBefore(" saying ").trim()
            val message = if (" saying " in body) body.substringAfter(" saying ").trim() else null
            return AssistantCommand("send_sms", target, message)
        }

        return when {
        containsAny(cmd, "open camera", "camera kholo", "camera on karo") -> AssistantCommand("open_app", "Camera")
        containsAny(cmd, "open gallery", "open photos", "gallery kholo") -> AssistantCommand("open_app", "Gallery")
        containsAny(cmd, "open chrome", "open browser", "browser kholo") -> AssistantCommand("open_app", "Chrome")
        containsAny(cmd, "open youtube", "youtube kholo") -> AssistantCommand("open_app", "YouTube")
        containsAny(cmd, "open whatsapp", "whatsapp kholo") -> AssistantCommand("open_app", "WhatsApp")
        containsAny(cmd, "open telegram", "telegram kholo") -> AssistantCommand("open_app", "Telegram")
        containsAny(cmd, "open settings", "settings kholo") -> AssistantCommand("open_app", "Settings")
        containsAny(cmd, "open maps", "maps kholo") -> AssistantCommand("open_app", "Maps")

        containsAny(cmd, "flashlight on", "torch on", "flashlight jalao", "torch jalao") ->
            AssistantCommand("toggle_setting", "flashlight")
        containsAny(cmd, "flashlight off", "torch off", "flashlight band", "torch band") ->
            AssistantCommand("toggle_setting", "flashlight")
        containsAny(cmd, "flashlight", "torch") -> AssistantCommand("toggle_setting", "flashlight")
        containsAny(cmd, "wifi", "wi-fi", "wi fi") -> AssistantCommand("toggle_setting", "wifi")
        containsAny(cmd, "bluetooth") -> AssistantCommand("toggle_setting", "bluetooth")
        containsAny(cmd, "airplane mode", "flight mode") -> AssistantCommand("toggle_setting", "airplane_mode")

        // Volume control (English + Hindi)
        containsAny(cmd, "volume up", "increase volume", "awaaz badhao", "volume badhao") ->
            AssistantCommand("set_volume", "up")
        containsAny(cmd, "volume down", "decrease volume", "awaaz kam karo", "volume kam karo") ->
            AssistantCommand("set_volume", "down")
        containsWord(cmd, "mute") || containsAny(cmd, "silent karo", "awaaz band karo") ->
            AssistantCommand("set_volume", "mute")
        containsAny(cmd, "full volume", "max volume", "volume full") ->
            AssistantCommand("set_volume", "max")

        // Music control (English + Hindi)
        containsAny(cmd, "play music", "play song", "gaana chalao", "resume music") ->
            AssistantCommand("media_control", "play")
        containsAny(cmd, "pause music", "pause song", "gaana roko", "gaana ruko") ->
            AssistantCommand("media_control", "pause")
        containsAny(cmd, "next song", "skip song", "agla gaana", "next track") ->
            AssistantCommand("media_control", "next")
        containsAny(cmd, "previous song", "last song", "pichla gaana", "previous track") ->
            AssistantCommand("media_control", "previous")
        containsAny(cmd, "stop music", "gaana band karo") ->
            AssistantCommand("media_control", "stop")

        // Alarm — "set alarm for 7:30" / "alarm laga do 7 baje"
        containsAny(cmd, "set alarm", "wake me up", "alarm laga", "alarm set karo") ->
            AssistantCommand("set_alarm", extractTime(cmd))

        // Timer — "set a timer for 5 minutes" / "5 minute ka timer laga do"
        containsAny(cmd, "set a timer", "set timer", "start timer", "timer laga") ->
            AssistantCommand("set_timer", extractDurationSeconds(cmd)?.toString())

        // Phase 4 — Vision: OCR / object detection / face detection
        containsAny(cmd, "read this text", "read the text", "scan text", "ocr", "text padho") ->
            AssistantCommand("open_vision", "ocr")
        containsAny(cmd, "what do you see", "detect object", "identify object", "objects pehchano") ->
            AssistantCommand("open_vision", "objects")
        containsAny(cmd, "how many faces", "detect face", "recognize face", "face pehchano") ->
            AssistantCommand("open_vision", "faces")
        containsAny(cmd, "open camera vision", "open vision", "vision mode") ->
            AssistantCommand("open_vision", "ocr")

        // Phase 5 — Web search
        cmd.startsWith("search for ") || cmd.startsWith("google ") || cmd.startsWith("search ") ->
            AssistantCommand("web_search", original.substringAfter(" ").trim())

        // Phase 5 — App lock by voice
        cmd.startsWith("set my pin to ") || cmd.startsWith("set pin to ") ->
            AssistantCommand("set_pin", cmd.substringAfterLast(" ").trim())
        cmd.startsWith("lock ") ->
            AssistantCommand("lock_app", original.substringAfter(" ").trim())
        cmd.startsWith("unlock ") ->
            AssistantCommand("unlock_app", original.substringAfter(" ").trim())

        // Phase 5 — PC connect
        containsAny(cmd, "connect to pc", "connect pc", "pc se connect", "start pc bridge") ->
            AssistantCommand("pc_connect", "on")
        containsAny(cmd, "disconnect pc", "stop pc bridge", "pc disconnect") ->
            AssistantCommand("pc_connect", "off")

        // Call — "call X", "please call X", "phone X", "dial X" (English order)
        // (matched earlier, before this when — see top of function)

        cmd.startsWith("open ") -> AssistantCommand("open_app", original.substringAfter(" ").trim())
        cmd.startsWith("launch ") -> AssistantCommand("open_app", original.substringAfter(" ").trim())
        cmd.startsWith("start ") -> AssistantCommand("open_app", original.substringAfter(" ").trim())

        else -> null
        }
    }

    /**
     * Pulls the matching span back out of the original (non-lowercased) text so contact
     * names keep their real casing, instead of using the lowercased match directly.
     * [fromStart] = true means the captured group was the leading part of the phrase
     * (Hindi "X ko call karo" order); false means it was the trailing part ("call X").
     */
    private fun extractTail(original: String, matchedLower: String, fromStart: Boolean = false): String {
        val words = matchedLower.trim().split(Regex("\\s+")).size
        val originalWords = original.trim().split(Regex("\\s+"))
        if (words <= 0 || words > originalWords.size) return matchedLower.trim()
        return if (fromStart) {
            originalWords.take(words).joinToString(" ").trim()
        } else {
            originalWords.takeLast(words).joinToString(" ").trim()
        }
    }

    /** Extracts "HH:mm" from phrases like "set alarm for 7:30", "alarm at 7 am", "alarm for 6". */
    private fun extractTime(cmd: String): String? {
        val hm = Regex("""(\d{1,2}):(\d{2})""").find(cmd)
        if (hm != null) {
            var h = hm.groupValues[1].toInt()
            val m = hm.groupValues[2].toInt()
            if (Regex("""\bpm\b""").containsMatchIn(cmd) && h < 12) h += 12
            if (Regex("""\bam\b""").containsMatchIn(cmd) && h == 12) h = 0
            return String.format(Locale.US, "%02d:%02d", h.coerceIn(0, 23), m.coerceIn(0, 59))
        }
        val hourOnly = Regex("""\b(\d{1,2})\s*(am|pm)?\b""").find(cmd) ?: return null
        var h = hourOnly.groupValues[1].toIntOrNull() ?: return null
        val suffix = hourOnly.groupValues[2]
        if (suffix == "pm" && h < 12) h += 12
        if (suffix == "am" && h == 12) h = 0
        return String.format(Locale.US, "%02d:00", h.coerceIn(0, 23))
    }

    /** Extracts a duration in seconds from phrases like "5 minutes", "30 seconds", "1 hour". */
    private fun extractDurationSeconds(cmd: String): Int? {
        var total = 0
        var found = false
        Regex("""(\d+)\s*hour""").find(cmd)?.let { total += it.groupValues[1].toInt() * 3600; found = true }
        Regex("""(\d+)\s*min""").find(cmd)?.let { total += it.groupValues[1].toInt() * 60; found = true }
        Regex("""(\d+)\s*sec""").find(cmd)?.let { total += it.groupValues[1].toInt(); found = true }
        return if (found) total else null
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
        return "The result is $formatted, sir."
    }

    private fun timeFormat() = SimpleDateFormat("hh:mm a", Locale.getDefault())
    private fun dateFormat() = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault())

    private fun containsAny(text: String, vararg needles: String) = needles.any { text.contains(it) }

    /** Whole-word match, so short words like "mute" don't false-positive inside "commute". */
    private fun containsWord(text: String, word: String) =
        Regex("\\b${Regex.escape(word)}\\b").containsMatchIn(text)
}
