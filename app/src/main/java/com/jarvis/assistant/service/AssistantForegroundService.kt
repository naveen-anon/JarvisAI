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
    private lateinit var clapDetector: com.jarvis.assistant.voice.ClapDetector
    private var voiceSessionVerified = false
    private val scope = CoroutineScope(Dispatchers.Main)
    private val mainHandler = android.os.Handler(android.os.Looper.getMainLooper())

    var listener: AssistantListener? = null

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
        tts = TextToSpeechHelper(this)
        executor = CommandExecutor(this)
        gemini = GeminiClient(BuildConfig.GEMINI_API_KEY)
        networkStatus = NetworkStatusManager(this)
        weatherClient = WeatherClient(BuildConfig.OPENWEATHER_API_KEY)
        locationHelper = LocationHelper(this)
        pcBridge = PcBridgeServer(this) { speech -> processSpeech(speech) }
        voiceAuth = com.jarvis.assistant.voice.VoiceAuthManager(this)
        porcupine = com.jarvis.assistant.voice.PorcupineWakeWord(this)
        clapDetector = com.jarvis.assistant.voice.ClapDetector()
        offlineBrain = OfflineBrain(this, executor) { turnOn ->
            if (turnOn) pcBridge.start() else pcBridge.stop()
        }
        createNotificationChannel()
        scheduleDailySummary()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIF_ID, buildNotification("J.A.R.V.I.S. online — say \"Jarvis\" to activate"))
        val bg = com.jarvis.assistant.util.SettingsManager(this).getBackgroundListen()
        if (bg) {
            startWakeWordListening()
        } else {
            // Background listen off — still alive for notifications / manual listen / widget
        }
        if (intent?.action == com.jarvis.assistant.widget.JarvisWidgetActionReceiver.ACTION_START_LISTENING) {
            startListeningCycle()
        }
        return START_STICKY
    }

    private fun startWakeWordListening() {
        val settings = com.jarvis.assistant.util.SettingsManager(this)
        // Mic is exclusive: stop both before restarting
        if (::porcupine.isInitialized) porcupine.stop()
        if (::clapDetector.isInitialized) clapDetector.stop()

        // Double-clap wake (no API key). Skipped if Porcupine owns the mic.
        val wantClap = settings.getClapWakeEnabled()
        val wantVoice = porcupine.isAvailable

        if (wantVoice) {
            val ok = porcupine.start(
                onWake = {
                    mainHandler.post {
                        porcupine.stop()
                        if (::clapDetector.isInitialized) clapDetector.stop()
                        startListeningCycle()
                        mainHandler.postDelayed({
                            if (com.jarvis.assistant.util.SettingsManager(this).getBackgroundListen()) {
                                startWakeWordListening()
                            }
                        }, 4_000L)
                    }
                },
                onError = { msg ->
                    android.util.Log.w("JarvisService", "Porcupine: $msg — clap/STT fallback")
                    mainHandler.post {
                        if (wantClap) startClapWake(settings.getClapSensitivity())
                        else startSttWakeFallback()
                    }
                }
            )
            if (!ok) {
                if (wantClap) startClapWake(settings.getClapSensitivity())
                else startSttWakeFallback()
            }
            // When Porcupine is active it owns the mic — clap cannot run in parallel.
            // If user wants clap primarily, disable Picovoice key or we start clap when Porcupine fails.
        } else if (wantClap) {
            startClapWake(settings.getClapSensitivity())
        } else {
            startSttWakeFallback()
        }
    }

    private fun startClapWake(sensitivity: Float) {
        if (!::clapDetector.isInitialized) {
            clapDetector = com.jarvis.assistant.voice.ClapDetector()
        }
        clapDetector.start(sensitivity = sensitivity) {
            mainHandler.post {
                if (::clapDetector.isInitialized) clapDetector.stop()
                if (::porcupine.isInitialized) porcupine.stop()
                startListeningCycle()
                mainHandler.postDelayed({
                    if (com.jarvis.assistant.util.SettingsManager(this).getBackgroundListen()) {
                        startWakeWordListening()
                    }
                }, 4_000L)
            }
        }
        startForeground(NOTIF_ID, buildNotification("J.A.R.V.I.S. online — double clap to activate"))
        android.util.Log.i("JarvisService", "Clap wake armed (sensitivity=$sensitivity)")
    }

    /** Google SpeechRecognizer continuous keyword match (higher battery, no Picovoice key). */
    private fun startSttWakeFallback() {
        stt.listenContinuous { trailingCommand ->
            if (trailingCommand.isNotBlank()) {
                handleUserSpeech(trailingCommand)
            } else {
                startListeningCycle()
            }
        }
    }

    /** Text chat — same offline-first pipeline as voice. */
    fun submitText(text: String) {
        val trimmed = text.trim()
        if (trimmed.isBlank()) return
        handleUserSpeech(trimmed)
    }

    fun startListeningCycle() {
        listener?.onStateChanged(BrainState.LISTENING)
        stt.listenOnce(
            onResult = { speech ->
                if (speech.isNotBlank()) {
                    handleUserSpeech(speech)
                } else {
                    listener?.onStateChanged(BrainState.IDLE)
                }
            },
            onError = {
                listener?.onStateChanged(BrainState.ERROR)
                listener?.onStateChanged(BrainState.IDLE)
            }
        )
    }

    private fun handleUserSpeech(speech: String) {
        listener?.onTranscript(speech)
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
            buildMorningBriefing() to true
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
                CHANNEL_ID, "Jarvis Assistant", NotificationManager.IMPORTANCE_LOW
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
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("J.A.R.V.I.S.")
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_mic)
            .setOngoing(true)
            // Visible and tappable from the lock screen by default (unless the user has
            // hidden notification content on the lock screen in system settings) — this is
            // what makes "Jarvis works from the lock screen" possible without a full unlock.
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .addAction(R.drawable.ic_mic, "🎙 Listen", listenPendingIntent)
            .build()
    }

    inner class LocalBinder : Binder() {
        fun getService(): AssistantForegroundService = this@AssistantForegroundService
    }
    private val binder = LocalBinder()
    override fun onBind(intent: Intent?): IBinder = binder

    override fun onDestroy() {
        mainHandler.removeCallbacksAndMessages(null)
        if (::porcupine.isInitialized) porcupine.stop()
        if (::clapDetector.isInitialized) clapDetector.stop()
        stt.stopContinuous()
        stt.destroy()
        tts.shutdown()
        if (pcBridge.isRunning) pcBridge.stop()
        super.onDestroy()
    }
}
