package com.jarvis.assistant.service

import com.jarvis.assistant.BuildConfig

import android.app.NotificationChannel
import android.app.NotificationManager
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
import com.jarvis.assistant.util.NetworkStatusManager
import com.jarvis.assistant.voice.SpeechToText
import com.jarvis.assistant.voice.TextToSpeechHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Always-alive foreground service. Wake-word detection (Porcupine) should call
 * startListeningCycle() on wake, rather than this service polling continuously —
 * continuous SpeechRecognizer usage will drain battery fast and Android will kill it.
 *
 * Command routing is offline-first: [OfflineBrain] tries to answer locally (app
 * control, settings, time/date/battery, math, notes, small talk — all on-device).
 * Gemini is only ever contacted when the offline brain doesn't recognize the
 * command AND [NetworkStatusManager] confirms there's an actual internet path.
 * This means the assistant keeps working — app launching, calling, texting,
 * toggling wifi/bluetooth/flashlight, telling the time — with zero connectivity,
 * and it never crashes trying to reach the cloud while offline.
 */
class AssistantForegroundService : Service() {

    private lateinit var stt: SpeechToText
    private lateinit var tts: TextToSpeechHelper
    private lateinit var executor: CommandExecutor
    private lateinit var gemini: GeminiClient
    private lateinit var offlineBrain: OfflineBrain
    private lateinit var networkStatus: NetworkStatusManager
    private val scope = CoroutineScope(Dispatchers.Main)

    /** Lets MainActivity (or any bound UI) reflect what the assistant is doing. */
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
        offlineBrain = OfflineBrain(this, executor)
        networkStatus = NetworkStatusManager(this)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIF_ID, buildNotification("Say \"Jarvis\" to activate"))
        startWakeWordListening()
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
            // 1) Try the offline brain first. Covers app control, calls, SMS,
            //    settings toggles, time/date/battery, math, notes, and small talk —
            //    entirely on-device, no network round trip involved.
            val offlineReply = offlineBrain.handle(speech)

            val (resultText, fromCloud) = if (offlineReply != null) {
                offlineReply to false
            } else if (networkStatus.isOnline()) {
                // 2) Only reach for Gemini if genuinely online. Any failure here
                //    (timeout, DNS failure, bad key, non-200 response) is caught so
                //    it degrades to a spoken message instead of crashing the service.
                try {
                    val command = gemini.getCommand(speech)
                    executor.execute(command) to true
                } catch (e: Exception) {
                    "I couldn't reach the cloud brain just now. Try a device command instead." to true
                }
            } else {
                // 3) Fully offline and the offline brain didn't recognize it —
                //    fail gracefully instead of ever touching the network.
                "I'm offline right now and don't have a local command for that yet." to false
            }

            listener?.onStateChanged(BrainState.SPEAKING)
            listener?.onResponse(resultText, fromCloud)
            tts.speak(resultText)
            listener?.onStateChanged(BrainState.IDLE)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "Jarvis Assistant", NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun buildNotification(text: String) =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Jarvis")
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_mic)
            .setOngoing(true)
            .build()

    inner class LocalBinder : Binder() {
        fun getService(): AssistantForegroundService = this@AssistantForegroundService
    }
    private val binder = LocalBinder()
    override fun onBind(intent: Intent?): IBinder = binder

    override fun onDestroy() {
        stt.stopContinuous()
        stt.destroy()
        tts.shutdown()
        super.onDestroy()
    }
}
