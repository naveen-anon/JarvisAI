package com.jarvis.assistant.voice

import android.content.Context
import android.util.Log
import com.jarvis.assistant.util.SettingsManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * JARVIS voice — **Piper only** (on-device).
 * No Google TextToSpeech, no ElevenLabs.
 *
 * Add voice pack under assets/piper/ + libpiper_jni.so (see assets/piper/README.txt).
 */
class TextToSpeechHelper(context: Context) {

    private val appContext = context.applicationContext
    private val settings = SettingsManager(appContext)
    private val piper = PiperTtsEngine(appContext)
    private val scope = CoroutineScope(Dispatchers.Main)
    private var job: Job? = null
    private var pending: String? = null

    init {
        if (!piper.isReady) {
            Log.w(TAG, "Piper voice pack / native lib missing — speech disabled until installed")
        } else {
            pending?.let { speak(it) }
            pending = null
        }
    }

    fun speak(text: String) {
        if (text.isBlank()) return
        if (!piper.isReady) {
            pending = text
            Log.e(TAG, "Cannot speak — Piper not ready (install voice pack + libpiper_jni)")
            return
        }
        job?.cancel()
        piper.stop()
        val spoken = text.replace("...", ", ").replace(" — ", ", ").replace(" – ", ", ")
        job = scope.launch {
            val ok = withContext(Dispatchers.IO) {
                val speed = settings.getVoiceSpeed()
                val adjusted = if (speed < 0.9f) spoken.replace(", ", ",  ") else spoken
                piper.speak(adjusted)
            }
            if (!ok) Log.e(TAG, "Piper speak failed")
        }
    }

    fun stop() {
        job?.cancel()
        piper.stop()
    }

    fun shutdown() {
        stop()
        piper.release()
    }

    companion object {
        private const val TAG = "JarvisTTS"
    }
}
