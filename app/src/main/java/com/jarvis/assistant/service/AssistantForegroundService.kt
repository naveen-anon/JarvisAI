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
import com.jarvis.assistant.util.LocationHelper
import com.jarvis.assistant.util.NetworkStatusManager
import com.jarvis.assistant.util.WeatherClient
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
        startForeground(NOTIF_ID, buildNotification("Say \"Jarvis\" to activate"))
        startWakeWordListening()
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
            tts.speak(resultText)
            listener?.onStateChanged(BrainState.IDLE)
        }
    }

    /**
     * Shared reasoning pipeline used both by on-device voice input and by the PC bridge
     * (Phase 5). Order: weather (needs network+location, handled specially since it doesn't
     * fit the pure offline/cloud split) → offline brain → Gemini cloud fallback.
     */
    private suspend fun processSpeech(speech: String): Pair<String, Boolean> {
        // Voice authentication — checked at most once per service session (not per command),
        // so it's a one-time gate rather than repeated friction. Once it passes, it stays
        // passed until the service restarts.
        if (voiceAuth.isEnabled() && voiceAuth.isEnrolled() && !voiceSessionVerified) {
            val sample = withContext(Dispatchers.IO) { voiceAuth.captureSample() }
            val passed = sample != null && voiceAuth.verify(sample)
            if (!passed) {
                return "Voice not recognized. This command wasn't run — try speaking again." to false
            }
            voiceSessionVerified = true
        }

        weatherReplyIfAsked(speech)?.let {
            offlineBrain.recordInteraction()
            return it to false
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
                JarvisLlmClient(apiKeyProvider = { BuildConfig.GROQ_API_KEY }).chat(speech)
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
                        "Online intelligence isn't configured yet, sir. Device commands still work offline." to true
                    } else {
                        "I couldn't reach the cloud brain just now, sir. Try a device command instead." to true
                    }
                }
            }
        } else {
            "I'm offline right now and don't have a local command for that yet, sir." to false
        }

        offlineBrain.recordInteraction()
        return result
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
            .setContentTitle("Jarvis")
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
