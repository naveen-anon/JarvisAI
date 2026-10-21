package com.jarvis.assistant.service

import com.jarvis.assistant.BuildConfig

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.jarvis.assistant.R
import com.jarvis.assistant.ai.GeminiClient
import com.jarvis.assistant.brain.BrainState
import com.jarvis.assistant.brain.OfflineBrain
import com.jarvis.assistant.executor.CommandExecutor
import com.jarvis.assistant.network.PcBridgeServer
import com.jarvis.assistant.util.FileFinder
import com.jarvis.assistant.util.LocationHelper
import com.jarvis.assistant.util.NetworkStatusManager
import com.jarvis.assistant.util.WeatherClient
import com.jarvis.assistant.util.PersistentMemory
import com.jarvis.assistant.util.SettingsManager
import com.jarvis.assistant.voice.SpeechToText
import com.jarvis.assistant.voice.TextToSpeechHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar
import com.jarvis.assistant.ai.JarvisLlmClient

/**
 * Always-alive foreground service. Wake-word detection (Porcupine) should call
 * startListeningCycle() on wake, rather than this service polling continuously —
 * continuous SpeechRecognizer usage will drain battery fast and Android will kill it.
 *
 * Command routing is offline-first: OfflineBrain tries to answer locally (app
 * control, settings, time/date/battery, math, notes, small talk — all on-device).
 * Gemini is only ever contacted when the offline brain doesn't recognize the
 * command AND NetworkStatusManager confirms there's an actual internet path.
 */
class AssistantForegroundService : Service() {

    private lateinit var stt: SpeechToText
    private lateinit var clapDetector: com.jarvis.assistant.voice.ClapDetector
    private var callAnnouncer: com.jarvis.assistant.util.IncomingCallAnnouncer? = null
    private lateinit var tts: TextToSpeechHelper
    private lateinit var executor: CommandExecutor
    private lateinit var gemini: GeminiClient
    private lateinit var offlineBrain: OfflineBrain
    private lateinit var networkStatus: NetworkStatusManager
    private lateinit var weatherClient: WeatherClient
    private lateinit var locationHelper: LocationHelper
    private lateinit var pcBridge: PcBridgeServer
    private lateinit var voiceAuth: com.jarvis.assistant.voice.VoiceAuthManager
    private lateinit var porcupine: com.jarvis.assistant.voice.PorcupineWakeWord
    private var openWake: com.jarvis.assistant.voice.OpenWakeWordEngine? = null
    private var voiceSessionVerified = false
    private val scope = CoroutineScope(Dispatchers.Main)
    private val mainHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private var proactive: com.jarvis.assistant.util.ProactiveAlertManager? = null
    private val proactiveTick = object : Runnable {
        override fun run() {
            proactive?.tick()
            mainHandler.postDelayed(this, 5 * 60_000L)
        }
    }

    var listener: AssistantListener? = null
    private var lastListenCycleMs = 0L
    private var listenCycleRunning = false

    interface AssistantListener {
        fun onStateChanged(state: BrainState)
        fun onTranscript(text: String)
        fun onResponse(text: String, fromCloud: Boolean)
    }

    companion object {
        const val CHANNEL_ID = "jarvis_channel"
        const val NOTIF_ID = 101
    }

    override fun onCreate() {
        super.onCreate()
        stt = SpeechToText(this)
        // Hard stop any residual continuous STT from previous process death
        try { stt.stopContinuous() } catch (_: Exception) {}
        clapDetector = com.jarvis.assistant.voice.ClapDetector()
        val smBoot = com.jarvis.assistant.util.SettingsManager(this)
        // Mic must NOT open until clap / Porcupine wake / explicit Listen button.
        // Clap wake is OFF by default so AudioRecord does not hold the mic at boot.
        // Clap wake disabled — continuous mic not allowed in background
        android.util.Log.i("JarvisService", "Clap wake skipped (no background mic)")
        try { clapDetector.stop() } catch (_: Exception) {}
        tts = TextToSpeechHelper(this)
        callAnnouncer = com.jarvis.assistant.util.IncomingCallAnnouncer(this) { tts }
        try { callAnnouncer?.start() } catch (_: Exception) {}
        executor = CommandExecutor(this)
        gemini = GeminiClient(BuildConfig.GEMINI_API_KEY)
        networkStatus = NetworkStatusManager(this)
        weatherClient = WeatherClient(BuildConfig.OPENWEATHER_API_KEY)
        locationHelper = LocationHelper(this)
        pcBridge = PcBridgeServer(this) { speech -> processSpeech(speech) }
        voiceAuth = com.jarvis.assistant.voice.VoiceAuthManager(this)
        porcupine = com.jarvis.assistant.voice.PorcupineWakeWord(this)
        offlineBrain = OfflineBrain(this, executor) { turnOn ->
            if (turnOn) pcBridge.start() else pcBridge.stop()
        }
        createNotificationChannel()
        scheduleDailySummary()
        proactive = com.jarvis.assistant.util.ProactiveAlertManager(this) { msg ->
            mainHandler.post {
                try { try { com.jarvis.assistant.ui.HudController.speaking() } catch (_: Exception) {}
                    tts.speak(msg) } catch (_: Exception) {}
            }
        }
        proactive?.start()
        mainHandler.postDelayed(proactiveTick, 60_000L)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIF_ID, buildNotification("J.A.R.V.I.S. online — say Hey Jarvis"))
        // Siri-style: on-device KWS only (not continuous Google STT).
        try { stt.stopContinuous() } catch (_: Exception) {}
        startWakeWordListening()
        val action = intent?.action
        if (action == com.jarvis.assistant.widget.JarvisWidgetActionReceiver.ACTION_START_LISTENING) {
            startListeningCycle()
        }
        return START_STICKY
    }


    /** Stop every continuous mic owner (OpenWakeWord, Porcupine, Google STT continuous). */
    private fun releaseAllMics() {
        try { openWake?.stop() } catch (_: Exception) {}
        try { porcupine.stop() } catch (_: Exception) {}
        try { stt.stopContinuous() } catch (_: Exception) {}
        try { clapDetector.stop() } catch (_: Exception) {}
    }

    private fun startWakeWordListening() {
        // Siri-style pipeline:
        // Background = OpenWakeWord (Hey Jarvis) or Porcupine — NOT Google continuous STT.
        listenCycleRunning = false
        try { stt.stopContinuous() } catch (_: Exception) {}
        try { porcupine.stop() } catch (_: Exception) {}
        try { openWake?.stop() } catch (_: Exception) {}
        // Brief pause so previous AudioRecord releases (fixes "mic used but no wake")
        try { Thread.sleep(200) } catch (_: Exception) {}

        try {
            if (openWake == null) {
                openWake = com.jarvis.assistant.voice.OpenWakeWordEngine(this)
            }
            val okOw = openWake!!.start {
                mainHandler.post {
                    try { openWake?.stop() } catch (_: Exception) {}
                    // Siri-like: glow + listening state immediately on "Hey Jarvis"
                    try { com.jarvis.assistant.ui.HudController.listening() } catch (_: Exception) {}
                    try { com.jarvis.assistant.ui.ActivationEdgeOverlay.show(this@AssistantForegroundService) } catch (_: Exception) {}
                    startListeningCycle()
                }
            }
            if (okOw) {
                android.util.Log.i("JarvisService", "Siri-style wake ON — say Hey Jarvis")
                return
            }
        } catch (e: Exception) {
            android.util.Log.w("JarvisService", "OpenWakeWord failed: " + e.message)
        }

        if (porcupine.isAvailable) {
            val ok = porcupine.start(
                onWake = {
                    mainHandler.post {
                        try { porcupine.stop() } catch (_: Exception) {}
                        try { com.jarvis.assistant.ui.HudController.listening() } catch (_: Exception) {}
                        try { com.jarvis.assistant.ui.ActivationEdgeOverlay.show(this@AssistantForegroundService) } catch (_: Exception) {}
                        startListeningCycle()
                    }
                },
                onError = { msg ->
                    android.util.Log.w("JarvisService", "Porcupine: " + msg)
                }
            )
            if (ok) {
                android.util.Log.i("JarvisService", "Porcupine wake ON — say Jarvis")
                return
            }
        }
        android.util.Log.w("JarvisService", "No on-device wake engine — STT fallback")
        startSttWakeFallback()
    }




    /** Google SpeechRecognizer continuous keyword match (higher battery, no Picovoice key). */

    /**
     * Continuous Google STT wake is DISABLED — it holds the mic open and drains battery.
     * Without Porcupine key: rely on clap wake + notification "Listen" + in-app Talk button.
     */
    private fun startSttWakeFallback() {
        val sm = com.jarvis.assistant.util.SettingsManager(this)
        if (!sm.getBackgroundListen()) {
            android.util.Log.i("JarvisService", "Background listen off — STT wake idle")
            try { stt.stopContinuous() } catch (_: Exception) {}
            return
        }
        android.util.Log.i("JarvisService", "STT continuous wake (Hey Jarvis)")
        try {
            stt.listenContinuous { trailing ->
                mainHandler.post {
                    try { stt.stopContinuous() } catch (_: Exception) {}
                    try { porcupine.stop() } catch (_: Exception) {}
                    if (trailing.isNotBlank()) {
                        handleUserSpeech(trailing)
                    } else {
                        startListeningCycle()
                    }
                    mainHandler.postDelayed({
                        if (com.jarvis.assistant.util.SettingsManager(this).getBackgroundListen()) {
                            startWakeWordListening()
                        }
                    }, 5_000L)
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("JarvisService", "STT wake failed: ${e.message}")
        }
    }







    /** Text chat — same offline-first pipeline as voice. */
    fun submitText(text: String) {
        val trimmed = text.trim()
        if (trimmed.isBlank()) return
        handleUserSpeech(trimmed)
    }


    private fun forceReleaseCommandMic() {
        try { stt.stopContinuous() } catch (_: Exception) {}
        try { openWake?.stop() } catch (_: Exception) {}
        try { porcupine.stop() } catch (_: Exception) {}
        listenCycleRunning = false
        try { com.jarvis.assistant.ui.ActivationEdgeOverlay.hide() } catch (_: Exception) {}
    }

    private fun resumeWakeSoon(delayMs: Long = 1800L) {
        mainHandler.postDelayed({
            try {
                // Critical: Google STT must be fully stopped before OpenWakeWord takes AudioRecord
                forceReleaseCommandMic()
                // Small gap so AudioRecord is free
                mainHandler.postDelayed({
                    try {
                        startWakeWordListening()
                        android.util.Log.i("JarvisService", "Hey Jarvis wake armed")
                    } catch (e: Exception) {
                        android.util.Log.w("JarvisService", "resume wake: " + e.message)
                    }
                }, 350L)
            } catch (e: Exception) {
                android.util.Log.w("JarvisService", "resume wake: " + e.message)
            }
        }, delayMs)
    }





    /**
     * Siri-style: after answering, keep a short follow-up window so the user can
     * speak the next command without saying "Hey Jarvis" again.
     */
    private fun startFollowUpListen() {
        mainHandler.postDelayed({
            if (listenCycleRunning) return@postDelayed
            try { openWake?.stop() } catch (_: Exception) {}
            try { porcupine.stop() } catch (_: Exception) {}
            try { stt.stopContinuous() } catch (_: Exception) {}
            listenCycleRunning = true
            lastListenCycleMs = System.currentTimeMillis()
            try { com.jarvis.assistant.ui.HudController.listening() } catch (_: Exception) {}
            try { com.jarvis.assistant.ui.ActivationEdgeOverlay.show(this@AssistantForegroundService) } catch (_: Exception) {}
            listener?.onStateChanged(BrainState.LISTENING)
            try {
                stt.listenOnce(
                    onResult = { speech ->
                        listenCycleRunning = false
                        try { stt.stopContinuous() } catch (_: Exception) {}
                        val s = speech.trim()
                        val cancel = s.equals("stop", true) || s.equals("cancel", true) ||
                            s.equals("never mind", true) || s.equals("nevermind", true) ||
                            s.equals("bas", true) || s.equals("cancel karo", true)
                        if (s.isNotBlank() && !cancel) {
                            handleUserSpeech(s)
                        } else {
                            try { com.jarvis.assistant.ui.HudController.idle() } catch (_: Exception) {}
                            try { com.jarvis.assistant.ui.ActivationEdgeOverlay.hide() } catch (_: Exception) {}
                            listener?.onStateChanged(BrainState.IDLE)
                            resumeWakeSoon(600L)
                        }
                    },
                    onError = {
                        listenCycleRunning = false
                        try { stt.stopContinuous() } catch (_: Exception) {}
                        try { com.jarvis.assistant.ui.HudController.idle() } catch (_: Exception) {}
                        try { com.jarvis.assistant.ui.ActivationEdgeOverlay.hide() } catch (_: Exception) {}
                        listener?.onStateChanged(BrainState.IDLE)
                        resumeWakeSoon(600L)
                    }
                )
            } catch (_: Exception) {
                listenCycleRunning = false
                resumeWakeSoon(600L)
            }
        }, 2200L) // after reply TTS starts
    }

    fun startListeningCycle() {
        val now = System.currentTimeMillis()
        // Allow re-tap after 1.5s (old 8s gate blocked reactor retries)
        if (listenCycleRunning && now - lastListenCycleMs < 1500L) {
            android.util.Log.d("JarvisService", "listen cycle busy")
            return
        }
        lastListenCycleMs = now
        listenCycleRunning = true

        // Hold wake engines OFF for entire command window
        try { openWake?.stop() } catch (_: Exception) {}
        try { porcupine.stop() } catch (_: Exception) {}
        try { stt.stopContinuous() } catch (_: Exception) {}
        // Note: do not removeCallbacksAndMessages(null) — that kills proactive timers.

        try { com.jarvis.assistant.ui.HudController.listening() } catch (_: Exception) {}
        try { com.jarvis.assistant.ui.ActivationEdgeOverlay.show(this) } catch (_: Exception) {}
        listener?.onStateChanged(BrainState.LISTENING)
        try {
            android.widget.Toast.makeText(this, "Listening…", android.widget.Toast.LENGTH_SHORT).show()
        } catch (_: Exception) {}

        // Siri-style ack — then wait for TTS before STT (long listen window still applies)
        val ack = try {
            val sm = SettingsManager(this)
            val name = sm.getUserName().trim().lowercase()
            when {
                name in listOf("ma'am", "madam", "mam", "miss", "mrs", "ms") -> "Yes ma'am"
                name.endsWith("a") || name in listOf("priya", "neha", "anjali", "riya", "shreya", "kavya") -> "Yes ma'am"
                else -> "Yes sir"
            }
        } catch (_: Exception) {
            "Yes sir"
        }
        try { openWake?.stop() } catch (_: Exception) {}
        try { porcupine.stop() } catch (_: Exception) {}
        try { tts.speak(ack) } catch (_: Exception) {}

        // Safety timeout (longer session)
        mainHandler.postDelayed({
            if (listenCycleRunning && System.currentTimeMillis() - lastListenCycleMs >= 20000L) {
                listenCycleRunning = false
                try { stt.stopContinuous() } catch (_: Exception) {}
                try { com.jarvis.assistant.ui.HudController.idle() } catch (_: Exception) {}
                try { com.jarvis.assistant.ui.ActivationEdgeOverlay.hide() } catch (_: Exception) {}
                listener?.onStateChanged(BrainState.IDLE)
                android.util.Log.w("JarvisService", "listen cycle timed out — reset")
                resumeWakeSoon(800L)
            }
        }, 20000L)

        fun beginStt(retry: Boolean = false) {
            try {
                try { openWake?.stop() } catch (_: Exception) {}
                try { porcupine.stop() } catch (_: Exception) {}
                stt.listenOnce(
                    onResult = { speech ->
                        listenCycleRunning = false
                        try { stt.stopContinuous() } catch (_: Exception) {}
                        val s = speech.trim()
                        val echo = s.equals("yes sir", true) ||
                            s.equals("yes ma'am", true) ||
                            s.equals("yes mam", true) ||
                            s.equals("yes", true) ||
                            s.equals("listening", true)
                        android.util.Log.i("JarvisService", "STT result: [$s] echo=$echo")
                        if (s.isNotBlank() && !echo) {
                            handleUserSpeech(s)
                        } else {
                            try { com.jarvis.assistant.ui.HudController.idle() } catch (_: Exception) {}
                            try { com.jarvis.assistant.ui.ActivationEdgeOverlay.hide() } catch (_: Exception) {}
                            listener?.onStateChanged(BrainState.IDLE)
                            resumeWakeSoon(1200L)
                        }
                    },
                    onError = {
                        // One automatic retry (speech timeout / no-match is common)
                        if (!retry && listenCycleRunning) {
                            android.util.Log.w("JarvisService", "STT error — retry once")
                            mainHandler.postDelayed({ beginStt(retry = true) }, 400L)
                        } else {
                            listenCycleRunning = false
                            try { stt.stopContinuous() } catch (_: Exception) {}
                            try { com.jarvis.assistant.ui.HudController.idle() } catch (_: Exception) {}
                            try { com.jarvis.assistant.ui.ActivationEdgeOverlay.hide() } catch (_: Exception) {}
                            listener?.onStateChanged(BrainState.IDLE)
                            android.util.Log.w("JarvisService", "STT onError final")
                            resumeWakeSoon(1200L)
                        }
                    }
                )
            } catch (e: Exception) {
                listenCycleRunning = false
                android.util.Log.e("JarvisService", "listenOnce failed: ${e.message}")
                listener?.onStateChanged(BrainState.IDLE)
                resumeWakeSoon(1200L)
            }
        }

        // After Yes sir finishes, open long STT window
        mainHandler.postDelayed({ beginStt(false) }, 1600L)
    }


    private fun handleUserSpeech(speech: String) {
        listener?.onTranscript(speech)
        try { com.jarvis.assistant.ui.ActivationEdgeOverlay.hide() } catch (_: Exception) {}
        listener?.onStateChanged(BrainState.THINKING)

        scope.launch {
            val (resultText, fromCloud) = processSpeech(speech)

            listener?.onStateChanged(BrainState.SPEAKING)
            listener?.onResponse(resultText, fromCloud)

            // A full code listing read aloud is useless and slow — speak a short line
            // instead when the reply is a code block or unusually long, while the full
            // text still reaches the screen via onResponse() above.
            val speechText = if (resultText.contains("```") || resultText.length > 400) {
                "I've displayed the full details on screen for your review, sir."
            } else {
                resultText
            }
            tts.speak(speechText)
            listener?.onStateChanged(BrainState.IDLE)
            // Siri-level: after speaking, one follow-up listen (no second Hey Jarvis), then wake
            mainHandler.postDelayed({
                if (!listenCycleRunning) startFollowUpListen()
            }, 2500L)
        }
    }

    /**
     * Shared reasoning pipeline used both by on-device voice input and by the PC bridge
     * (Phase 5). Order: weather (needs network+location, handled specially since it doesn't
     * fit the pure offline/cloud split) → file lookup (if a filename was mentioned) →
     * offline brain → Gemini cloud fallback.
     */
    
    /** MCU-style context: long-term memory + current utterance for smarter cloud replies. */
    private fun buildJarvisContext(currentSpeech: String): String {
        val mem = PersistentMemory(this).buildMemoryContext(
            SettingsManager(this).getUserName()
        )
        return buildString {
            append(mem)
            append("\n\nCurrent user utterance: ")
            append(currentSpeech)
            append("\nRespond as JARVIS: anticipate needs, be precise, stay in character.")
        }
    }

    
    private suspend fun buildMorningBriefing(): String {
        val sb = StringBuilder()
        val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        val hello = when {
            hour < 12 -> "Good morning"
            hour < 17 -> "Good afternoon"
            else -> "Good evening"
        }
        val name = try {
            SettingsManager(this).getUserName().let {
                if (it.isBlank() || it == "User") "sir" else it
            }
        } catch (_: Exception) {
            "sir"
        }
        sb.append("$hello, $name. ")

        val time = java.text.SimpleDateFormat("h:mm a", java.util.Locale.getDefault())
            .format(java.util.Date())
        sb.append("The time is $time. ")

        try {
            val bm = getSystemService(android.content.Context.BATTERY_SERVICE) as android.os.BatteryManager
            val bat = bm.getIntProperty(android.os.BatteryManager.BATTERY_PROPERTY_CAPACITY)
            sb.append("Power levels are at $bat percent. ")
        } catch (_: Exception) {
        }

        val online = try {
            networkStatus.isOnline()
        } catch (_: Exception) {
            false
        }
        sb.append(if (online) "Network is online. " else "Network is offline. ")

        // Calendar (next events)
        try {
            val cal = com.jarvis.assistant.util.CalendarHelper(this).formatBrief(2)
            if (!cal.contains("permission is not granted") && !cal.contains("No upcoming")) {
                sb.append("Calendar: ").append(cal).append(" ")
            } else if (cal.contains("No upcoming")) {
                sb.append("No upcoming calendar events. ")
            }
        } catch (_: Exception) {
        }

        // Notifications summary (short)
        try {
            val notifs = com.jarvis.assistant.notifications.JarvisNotificationListener.summaryOrHelp(5)
            if (!notifs.contains("Notification access is off") && !notifs.contains("No active")) {
                // Keep briefing shorter
                val short = if (notifs.length > 350) notifs.take(350) + "…" else notifs
                sb.append(short).append(" ")
            } else if (notifs.contains("Notification access is off")) {
                sb.append("Notification access is off. ")
            }
        } catch (_: Exception) {
        }

        if (online) {
            try {
                val loc = locationHelper.getCurrentLocation()
                if (loc != null) {
                    val w = weatherClient.getWeather(loc.lat, loc.lon)
                    if (w != null) {
                        sb.append("Weather in ${loc.cityName}: ${w.tempCelsius} degrees, ${w.condition}. ")
                    }
                }
            } catch (_: Exception) {
            }
            try {
                val headlines = com.jarvis.assistant.util.NewsClient().topHeadlines(3)
                if (headlines.isNotEmpty()) {
                    sb.append("Top headlines: ")
                    sb.append(headlines.mapIndexed { i, h -> "${i + 1}. $h" }.joinToString(" "))
                    sb.append(" ")
                }
            } catch (_: Exception) {
            }
        } else {
            sb.append("Weather and news unavailable offline. ")
        }

        sb.append("All systems are nominal. How may I assist you?")
        return sb.toString()
    }


    private suspend fun processSpeech(speech: String): Pair<String, Boolean> {

        // Multi-command: split and run each segment through offline brain + executor path once
        try {
            val segments = com.jarvis.assistant.util.CommandChainSplitter.split(speech)
            if (segments != null && segments.size >= 2 && segments.size <= 5) {
                val replies = mutableListOf<String>()
                for ((i, seg) in segments.withIndex()) {
                    val one = try {
                        offlineBrain.handle(seg) ?: "Done."
                    } catch (_: Exception) {
                        "Step failed."
                    }
                    // Skip special tokens
                    val clean = when (one) {
                        "REQUEST_BRIEFING" -> try {
                            com.jarvis.assistant.util.BriefingHelper(this@AssistantForegroundService)
                                .build(includeWeather = networkStatus.isOnline()).text
                        } catch (_: Exception) { "Briefing unavailable." }
                        else -> one
                    }
                    replies += clean
                    if (i < segments.lastIndex) kotlinx.coroutines.delay(650)
                }
                val summary = buildString {
                    append("Executed ${segments.size} steps, sir. ")
                    replies.forEachIndexed { i, r ->
                        append("Step ${i + 1}: ")
                        append(r.trim().trimEnd('.'))
                        append(". ")
                    }
                }
                return summary.trim() to false
            }
        } catch (_: Exception) { }

        // Voice authentication — checked at most once per service session (not per command),
        // so it's a one-time gate rather than repeated friction. Once it passes, it stays
        // passed until the service restarts.
        if (voiceAuth.isEnabled() && voiceAuth.isEnrolled() && !voiceSessionVerified) {
            val sample = withContext(Dispatchers.IO) { voiceAuth.captureSample() }
            val passed = sample != null && voiceAuth.verify(sample)
            if (!passed) {
                return "Voice authentication failed, sir. The command was not executed. Please try again." to false
            }
            voiceSessionVerified = true
        }

        weatherReplyIfAsked(speech)?.let {
            offlineBrain.recordInteraction()
            return it to false
        }

        // Checked before the offline brain so a filename like "upi_osint.py" isn't
        // misread as a "check <contact>"-style command.
        fileCommandReplyIfAsked(speech)?.let {
            offlineBrain.recordInteraction()
            return it to true
        }

        val offlineReply = try {
            offlineBrain.handle(speech)
        } catch (e: SecurityException) {
            "I don't have permission to do that. Please grant it in the app's settings."
        } catch (e: Exception) {
            "Something went wrong running that command."
        }

        val result = if (offlineReply == "REQUEST_BRIEFING") {
            try {
                val b = com.jarvis.assistant.util.BriefingHelper(this).build(includeWeather = networkStatus.isOnline())
                b.text to true
            } catch (_: Exception) {
                buildMorningBriefing() to true
            }
        } else if (offlineReply != null) {
            offlineReply to false
        } else if (networkStatus.isOnline()) {
            val groq = try {
                JarvisLlmClient(apiKeyProvider = { BuildConfig.GROQ_API_KEY }).chat(
                    speech,
                    history = emptyList(),
                    memoryContext = buildJarvisContext(speech)
                )
            } catch (e: Exception) {
                null
            }
            if (groq != null && groq.ok && groq.text.isNotBlank()) {
                groq.text to true
            } else {
                try {
                    val command = gemini.getCommand(speech)
                    try { com.jarvis.assistant.ui.HudController.executing() } catch (_: Exception) {}
                executor.execute(command) to true
                } catch (e: Exception) {
                    val hint = groq?.error.orEmpty()
                    if (hint.contains("API key", ignoreCase = true)) {
                        "Cloud intelligence is not configured, sir. On-device commands remain fully operational." to true
                    } else {
                        "I was unable to reach external systems, sir. Please try an on-device command." to true
                    }
                }
            }
        } else {
            "I'm currently offline and lack a local protocol for that request, sir." to false
        }

        offlineBrain.recordInteraction()
        return result
    }

    /**
     * If the user mentioned a filename (e.g. "upi_osint.py padho" or "fix errors in main.py"),
     * finds it on shared storage, reads it, and hands it to the cloud LLM along with the
     * user's original phrasing. Returns null if no filename was mentioned so the normal
     * pipeline continues untouched.
     */
    private suspend fun fileCommandReplyIfAsked(speech: String): String? {
        val fileName = FileFinder.extractFileName(speech) ?: return null

        if (!FileFinder.hasStorageAccess()) {
            return "I need storage access to read files, sir. Please grant \"All files access\" " +
                "for Jarvis in Settings, then try again."
        }
        if (!networkStatus.isOnline()) {
            return "I found that you mean a file, sir, but reading it needs the cloud brain and I'm offline right now."
        }

        val file = withContext(Dispatchers.IO) { FileFinder.findFile(fileName) }
            ?: return "I couldn't find a file named \"$fileName\" on this device, sir."

        val content = withContext(Dispatchers.IO) { FileFinder.readTextCapped(file) }
            ?: return "I found \"$fileName\" but couldn't read it, sir — it may not be a text file."

        val combined = buildString {
            append("The user said: \"").append(speech).append("\"\n\n")
            append("Attached file \"").append(fileName).append("\" (path: ").append(file.absolutePath).append("):\n\n")
            append("```\n").append(content).append("\n```\n\n")
            append("Respond to their request about this file.")
        }

        val result = try {
            JarvisLlmClient(apiKeyProvider = { BuildConfig.GROQ_API_KEY }).chat(combined)
        } catch (e: Exception) {
            null
        }

        return if (result != null && result.ok && result.text.isNotBlank()) {
            result.text
        } else {
            "I found the file but couldn't process it right now, sir."
        }
    }

    /** Phase 5 — "Live weather aur location". Returns null if the speech isn't a weather question. */
    private suspend fun weatherReplyIfAsked(speech: String): String? {
        val lower = speech.lowercase()
        val isWeatherQuestion = listOf("weather", "temperature", "mausam", "kitni garmi", "kitni thand")
            .any { lower.contains(it) }
        if (!isWeatherQuestion) return null

        if (!networkStatus.isOnline()) return "I need an internet connection to check the weather."

        val location = locationHelper.getCurrentLocation()
            ?: return "I couldn't get your current location. Make sure location permission is granted."
        val weather = weatherClient.getWeather(location.lat, location.lon)
            ?: return "I couldn't reach the weather service just now."

        return "It's ${weather.tempCelsius}°C and ${weather.condition} in ${location.cityName}."
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "Jarvis Assistant", NotificationManager.IMPORTANCE_DEFAULT
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    /** Phase 5 — "Daily activity summary", scheduled once for ~8 PM every day. */
    private fun scheduleDailySummary() {
        val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
        val receiverIntent = Intent(this, DailySummaryReceiver::class.java).apply {
            action = DailySummaryReceiver.ACTION_DAILY_SUMMARY
        }
        val pendingIntent = PendingIntent.getBroadcast(
            this, 0, receiverIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val triggerTime = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 20)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            if (before(Calendar.getInstance())) add(Calendar.DAY_OF_YEAR, 1)
        }

        try {
            alarmManager.setInexactRepeating(
                AlarmManager.RTC,
                triggerTime.timeInMillis,
                AlarmManager.INTERVAL_DAY,
                pendingIntent
            )
        } catch (e: SecurityException) {
            // SCHEDULE_EXACT_ALARM not granted on some OEMs — daily summary just won't fire;
            // "my routine" by voice still works on demand regardless.
        }
    }

    private fun buildNotification(text: String): android.app.Notification {
        val listenIntent = Intent(this, com.jarvis.assistant.widget.JarvisWidgetActionReceiver::class.java).apply {
            action = com.jarvis.assistant.widget.JarvisWidgetActionReceiver.ACTION_START_LISTENING
        }
        val listenPendingIntent = PendingIntent.getBroadcast(
            this, 0, listenIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val openApp = Intent(this, com.jarvis.assistant.MainActivity::class.java)
        val openPi = PendingIntent.getActivity(
            this, 2, openApp, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("J.A.R.V.I.S")
            .setContentText(text.ifBlank { "Standing by · tap Listen" })
            .setSmallIcon(R.drawable.ic_mic)
            .setColor(0xFF00D9FF.toInt())
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(openPi)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(text.ifBlank { "Standing by. Use Listen for voice commands without unlocking." })
            )
            .addAction(R.drawable.ic_mic, "Listen", listenPendingIntent)
            .build()
    }

    inner class LocalBinder : Binder() {
        fun getService(): AssistantForegroundService = this@AssistantForegroundService
    }
    private val binder = LocalBinder()
    override fun onBind(intent: Intent?): IBinder = binder

    override fun onDestroy() {
        try { openWake?.stop() } catch (_: Exception) {}
        try { callAnnouncer?.stop() } catch (_: Exception) {}
        mainHandler.removeCallbacksAndMessages(null)
        proactive?.stop()
        proactive = null
        if (::porcupine.isInitialized) porcupine.stop()
        stt.stopContinuous()
        stt.destroy()
        tts.shutdown()
        if (pcBridge.isRunning) pcBridge.stop()
        super.onDestroy()
    }
}
