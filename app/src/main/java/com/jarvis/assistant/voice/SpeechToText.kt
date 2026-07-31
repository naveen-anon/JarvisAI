package com.jarvis.assistant.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import java.util.Locale

/**
 * Speech pipeline tuned for quieter / soft voices.
 */
class SpeechToText(private val context: Context) {

    private var recognizer: SpeechRecognizer? = null
    private var isListeningContinuously = false

    private val wakeWords = listOf("jarvis", "जार्विस", "jarwis", "jaarvis", "jarviz")

    fun listenOnce(onResult: (String) -> Unit, onError: () -> Unit) {
        stopInternal()
        recognizer = SpeechRecognizer.createSpeechRecognizer(context)
        recognizer?.setRecognitionListener(simpleListener(onResult, onError))
        recognizer?.startListening(buildIntent())
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
            override fun onResults(results: Bundle?) {
                val heard = bestTranscript(results).lowercase().trim()
                Log.d("SpeechToText", "Heard: $heard")
                val matchedWakeWord = wakeWords.firstOrNull { heard.contains(it) }
                if (matchedWakeWord != null) {
                    onWakeWordDetected(heard.substringAfter(matchedWakeWord).trim())
                }
                restartContinuous(onWakeWordDetected)
            }
            override fun onError(error: Int) { restartContinuous(onWakeWordDetected) }
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        recognizer?.startListening(buildIntent())
    }

    private fun restartContinuous(onWakeWordDetected: (String) -> Unit) {
        recognizer?.destroy()
        if (isListeningContinuously) {
            android.os.Handler(context.mainLooper).postDelayed({
                startContinuousSession(onWakeWordDetected)
            }, 250)
        }
    }

    fun stopContinuous() {
        isListeningContinuously = false
        stopInternal()
    }

    private fun stopInternal() {
        recognizer?.destroy()
        recognizer = null
    }

    private fun simpleListener(onResult: (String) -> Unit, onError: () -> Unit) =
        object : RecognitionListener {
            override fun onResults(results: Bundle?) { onResult(bestTranscript(results)) }
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
        return matches.filter { it.isNotBlank() }.maxByOrNull { it.trim().length } ?: matches.first()
    }

    private fun buildIntent() = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toString())
        putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
        putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 2000L)
        putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1000L)
        putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 400L)
    }

    fun destroy() {
        isListeningContinuously = false
        stopInternal()
    }
}
