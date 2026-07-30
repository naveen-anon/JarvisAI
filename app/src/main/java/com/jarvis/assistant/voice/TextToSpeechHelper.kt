package com.jarvis.assistant.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import com.jarvis.assistant.util.SettingsManager
import java.util.Locale

class TextToSpeechHelper(context: Context) {

    private var ready = false
    private var pending: String? = null
    private val settings = SettingsManager(context)

    private lateinit var tts: TextToSpeech

    init {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                ready = true
                tts.language = Locale.US
                pending?.let { speak(it) }
                pending = null
            }
        }
    }

    fun speak(text: String) {
        if (text.isBlank()) return
        if (!ready) {
            pending = text
            return
        }
        applyVoiceSettings()
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "jarvis_utterance")
    }

    /**
     * Re-reads voice settings before every utterance (instead of once at init) so a
     * "change voice to male/female/robot" command takes effect immediately on the very
     * next thing Jarvis says, without needing to recreate the TTS engine.
     *
     * Android's TTS doesn't reliably expose distinct male/female system voices across
     * devices, so "voice type" here is approximated with pitch/rate presets rather than
     * swapping the underlying TTS voice — a robot-ish or higher/lower-pitched voice,
     * not a different person's voice.
     */
    private fun applyVoiceSettings() {
        val userSpeed = settings.getVoiceSpeed()
        val userPitch = settings.getVoicePitch()

        val (pitch, rate) = when (settings.getVoiceType()) {
            "male" -> 0.85f * userPitch to userSpeed
            "female" -> 1.25f * userPitch to userSpeed
            "robot" -> 0.55f * userPitch to (userSpeed * 0.9f)
            else -> userPitch to userSpeed
        }
        tts.setPitch(pitch.coerceIn(0.3f, 2.0f))
        tts.setSpeechRate(rate.coerceIn(0.3f, 3.0f))
    }

    fun shutdown() {
        tts.stop()
        tts.shutdown()
    }
}
