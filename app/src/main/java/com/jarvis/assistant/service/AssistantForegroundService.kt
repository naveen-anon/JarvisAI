package com.jarvis.assistant.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.jarvis.assistant.R
import com.jarvis.assistant.ai.ClaudeClient
import com.jarvis.assistant.executor.CommandExecutor
import com.jarvis.assistant.voice.SpeechToText
import com.jarvis.assistant.voice.TextToSpeechHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Always-alive foreground service. Wake-word detection (Porcupine) should call
 * startListeningCycle() on wake, rather than this service polling continuously —
 * continuous SpeechRecognizer usage will drain battery fast and Android will kill it.
 */
class AssistantForegroundService : Service() {

    private lateinit var stt: SpeechToText
    private lateinit var tts: TextToSpeechHelper
    private lateinit var executor: CommandExecutor
    private lateinit var claude: ClaudeClient
    private val scope = CoroutineScope(Dispatchers.Main)

    companion object {
        const val CHANNEL_ID = "jarvis_channel"
        const val NOTIF_ID = 101
        // Replace with a secure retrieval, e.g. EncryptedSharedPreferences.
        const val API_KEY_PLACEHOLDER = "YOUR_ANTHROPIC_API_KEY"
    }

    override fun onCreate() {
        super.onCreate()
        stt = SpeechToText(this)
        tts = TextToSpeechHelper(this)
        executor = CommandExecutor(this)
        claude = ClaudeClient(API_KEY_PLACEHOLDER)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIF_ID, buildNotification("Listening for commands"))
        startListeningCycle()
        return START_STICKY
    }

    /** Call this each time the wake word ("Jarvis") is detected. */
    fun startListeningCycle() {
        stt.listenOnce(
            onResult = { speech ->
                if (speech.isNotBlank()) {
                    handleUserSpeech(speech)
                }
            },
            onError = { /* log or retry */ }
        )
    }

    private fun handleUserSpeech(speech: String) {
        scope.launch {
            val command = claude.getCommand(speech)
            val resultText = executor.execute(command)
            tts.speak(resultText)
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
        stt.destroy()
        tts.shutdown()
        super.onDestroy()
    }
}
