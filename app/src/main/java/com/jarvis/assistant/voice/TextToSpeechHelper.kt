package com.jarvis.assistant.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import com.jarvis.assistant.util.SettingsManager
import java.util.Locale
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean

/**
 * JARVIS TTS — Piper first, Android system TTS always as fallback.
 * Never goes fully mute if the device has any TTS engine.
 */
class TextToSpeechHelper(context: Context) {

    private val app = context.applicationContext
    private val settings = SettingsManager(app)
    private val piper = PiperTtsEngine(app)

    private var androidTts: TextToSpeech? = null
    private val androidReady = AtomicBoolean(false)
    private var pending: String? = null

    init {
        androidTts = TextToSpeech(app) { status ->
            if (status == TextToSpeech.SUCCESS) {
                androidReady.set(true)
                configureAndroidVoice()
                pending?.let { t ->
                    pending = null
                    speak(t)
                }
                Log.i(TAG, "Android TTS ready (fallback)")
            } else {
                Log.e(TAG, "Android TTS init failed: $status")
            }
        }
        androidTts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}
            override fun onDone(utteranceId: String?) {}
            override fun onError(utteranceId: String?) {
                Log.e(TAG, "Android TTS utterance error")
            }
        })
        if (piper.isReady) Log.i(TAG, "Piper available")
        else Log.w(TAG, "Piper not ready — using Android TTS fallback")
    }

    fun speak(text: String) {
        val cleaned = text
            .replace("...", ", ")
            .replace(" — ", ", ")
            .replace(" – ", ", ")
            .trim()
        if (cleaned.isEmpty()) return

        // Prefer Piper when native stack is ready
        if (piper.isReady) {
            try {
                piper.stop()
                val ok = piper.speak(cleaned)
                if (ok) {
                    Log.i(TAG, "Spoke via Piper")
                    return
                }
                Log.w(TAG, "Piper speak failed — falling back to Android TTS")
            } catch (e: Exception) {
                Log.e(TAG, "Piper error: ${e.message}")
            }
        }

        speakAndroid(cleaned)
    }

    private fun speakAndroid(text: String) {
        val tts = androidTts
        if (tts == null || !androidReady.get()) {
            pending = text
            Log.w(TAG, "Android TTS not ready — queued")
            return
        }
        try {
            configureAndroidVoice()
            tts.stop()
            val id = UUID.randomUUID().toString()
            val r = tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, id)
            if (r == TextToSpeech.ERROR) Log.e(TAG, "Android speak ERROR")
            else Log.i(TAG, "Spoke via Android TTS")
        } catch (e: Exception) {
            Log.e(TAG, "Android speak failed: ${e.message}")
        }
    }

    private fun configureAndroidVoice() {
        val tts = androidTts ?: return
        try {
            // Prefer UK English male-ish system voice when available
            tts.language = Locale.UK
            val voices = try { tts.voices } catch (_: Exception) { null }
            if (voices != null) {
                val male = voices.firstOrNull { v ->
                    val n = v.name.lowercase()
                    (n.contains("male") || n.contains("gb") || n.contains("uk")) &&
                        !n.contains("female") &&
                        v.locale.language == "en"
                } ?: voices.firstOrNull {
                    it.locale.language == "en" && !it.name.lowercase().contains("female")
                }
                if (male != null) tts.voice = male
            }
            val speed = settings.getVoiceSpeed().coerceIn(0.7f, 1.4f)
            val pitch = settings.getVoicePitch().coerceIn(0.8f, 1.2f)
            tts.setSpeechRate(speed)
            tts.setPitch(pitch)
        } catch (e: Exception) {
            Log.w(TAG, "configureAndroidVoice: ${e.message}")
        }
    }

    fun stop() {
        try { piper.stop() } catch (_: Exception) {}
        try { androidTts?.stop() } catch (_: Exception) {}
    }

    fun shutdown() {
        stop()
        try { piper.release() } catch (_: Exception) {}
        try {
            androidTts?.shutdown()
        } catch (_: Exception) {}
        androidTts = null
        androidReady.set(false)
    }

    companion object {
        private const val TAG = "JarvisTTS"
    }
}
