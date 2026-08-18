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
    private var voiceSessionVerified = false
    private val scope = CoroutineScope(Dispatchers.Main)

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

        val result = if (offlineReply != null) {
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
        stt.stopContinuous()
        stt.destroy()
        tts.shutdown()
        if (pcBridge.isRunning) pcBridge.stop()
        super.onDestroy()
    }
}
