package com.jarvis.assistant.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import java.util.Locale

class SpeechToText(private val context: Context) {

    private var recognizer: SpeechRecognizer? = null
    private var isListeningContinuously = false
    private var lastWakeMs = 0L
    private val mainHandler = Handler(Looper.getMainLooper())

    private val wakeWords = listOf(
        "jarvis", "jarwis", "jaarvis", "jarviz", "jarves", "jarvus",
        "jervis", "jarvish",
        "जार्विस", "जारविस", "जर्विस",
        "hey jarvis", "ok jarvis", "okay jarvis", "hi jarvis",
        "hello jarvis", "jarvis ji"
    )

    fun listenOnce(onResult: (String) -> Unit, onError: () -> Unit) {
        stopInternal()
        recognizer = SpeechRecognizer.createSpeechRecognizer(context)
        recognizer?.setRecognitionListener(simpleListener(onResult, onError))
        recognizer?.startListening(buildIntent(forWakeWord = false))
    }

    fun listenContinuous(onWakeWordDetected: (trailingCommand: String) -> Unit) {
        if (isListeningContinuously) return
        isListeningContinuously = true
        startContinuousSession(onWakeWordDetected)
    }

    fun isListening(): Boolean = isListeningContinuously

    private fun startContinuousSession(onWakeWordDetected: (String) -> Unit) {
        if (!isListeningContinuously) return
        recognizer = SpeechRecognizer.createSpeechRecognizer(context)
        recognizer?.setRecognitionListener(object : RecognitionListener {
            private var triggeredThisSession = false

            override fun onPartialResults(partialResults: Bundle?) {
                if (triggeredThisSession) return
                val heard = bestTranscript(partialResults).lowercase().trim()
                if (heard.isBlank()) return
                tryFireWake(heard, onWakeWordDetected) { triggeredThisSession = true }
            }

            override fun onResults(results: Bundle?) {
                if (!triggeredThisSession) {
                    val heard = bestTranscript(results).lowercase().trim()
                    Log.d("SpeechToText", "Final heard: $heard")
                    tryFireWake(heard, onWakeWordDetected) { triggeredThisSession = true }
                }
                restartContinuous(onWakeWordDetected)
            }

            override fun onError(error: Int) {
                Log.d("SpeechToText", "Recognizer error: $error")
                restartContinuous(onWakeWordDetected)
            }

            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        recognizer?.startListening(buildIntent(forWakeWord = true))
    }

    private fun tryFireWake(
        heard: String,
        onWakeWordDetected: (String) -> Unit,
        onFired: () -> Unit
    ) {
        val now = System.currentTimeMillis()
        if (now - lastWakeMs < 1800) return
        val match = findWakeWord(heard) ?: return
        lastWakeMs = now
        onFired()
        val trailing = extractTrailing(heard, match)
        Log.d("SpeechToText", "Wake matched=$match trailing=$trailing")
        onWakeWordDetected(trailing)
    }

    private fun findWakeWord(heard: String): String? {
        for (w in wakeWords.sortedByDescending { it.length }) {
            if (matchesWake(heard, w)) return w
        }
        return null
    }

    private fun matchesWake(heard: String, wake: String): Boolean {
        if (heard == wake) return true
        if (heard.startsWith("$wake ")) return true
        if (heard.endsWith(" $wake")) return true
        if (heard.contains(" $wake ")) return true
        if (wake.length >= 4 && heard.contains(wake)) return true
        return false
    }

    private fun extractTrailing(heard: String, wake: String): String {
        val idx = heard.indexOf(wake)
        if (idx < 0) return ""
        return heard.substring(idx + wake.length).trim().trimStart(',', '.', ' ')
    }

    private fun restartContinuous(onWakeWordDetected: (String) -> Unit) {
        recognizer?.destroy()
        recognizer = null
        if (!isListeningContinuously) return
        mainHandler.postDelayed({
            if (isListeningContinuously) startContinuousSession(onWakeWordDetected)
        }, 280)
    }

    fun stopContinuous() {
        isListeningContinuously = false
        mainHandler.removeCallbacksAndMessages(null)
        stopInternal()
    }

    private fun stopInternal() {
        try {
            recognizer?.cancel()
            recognizer?.destroy()
        } catch (_: Exception) {
        }
        recognizer = null
    }

    private fun simpleListener(onResult: (String) -> Unit, onError: () -> Unit) =
        object : RecognitionListener {
            override fun onResults(results: Bundle?) {
                onResult(bestTranscript(results))
            }
            override fun onError(error: Int) = onError()
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        }

    private fun bestTranscript(results: Bundle?): String {
        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION) ?: return ""
        if (matches.isEmpty()) return ""
        val boost = listOf(
            "jarvis", "screen", "analyze", "analyse", "weather", "mausam",
            "remember", "macro", "whatsapp", "call", "brief", "morning",
            "chrome", "bluetooth", "flashlight", "volume", "alarm"
        )
        return matches.filter { it.isNotBlank() }.maxByOrNull { m ->
            val lower = m.lowercase()
            var score = m.length
            boost.forEach { if (lower.contains(it)) score += 40 }
            score
        } ?: matches.first()
    }

    private fun buildIntent(forWakeWord: Boolean) = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
        putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-IN")
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "en-IN")
        if (forWakeWord) {
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1200L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 800L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 300L)
        } else {
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 2000L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1200L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 400L)
        }
    }

    fun destroy() {
        isListeningContinuously = false
        mainHandler.removeCallbacksAndMessages(null)
        stopInternal()
    }
}
