package com.jarvis.assistant.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log

class SpeechToText(private val context: Context) {

    private var recognizer: SpeechRecognizer? = null
    private var isListeningContinuously = false
    private val wakeWord = "jarvis"

    fun listenOnce(onResult: (String) -> Unit, onError: () -> Unit) {
        stopInternal()
        recognizer = SpeechRecognizer.createSpeechRecognizer(context)
        recognizer?.setRecognitionListener(simpleListener(onResult, onError))
        recognizer?.startListening(buildIntent())
    }

    fun listenContinuous(onWakeWordDetected: (trailingCommand: String) -> Unit) {
        isListeningContinuously = true
        startContinuousSession(onWakeWordDetected)
    }

    private fun startContinuousSession(onWakeWordDetected: (String) -> Unit) {
        if (!isListeningContinuously) return

        recognizer = SpeechRecognizer.createSpeechRecognizer(context)
        recognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val heard = matches?.firstOrNull()?.lowercase()?.trim() ?: ""
                Log.d("SpeechToText", "Heard: $heard")

                if (heard.contains(wakeWord)) {
                    val trailing = heard.substringAfter(wakeWord).trim()
                    onWakeWordDetected(trailing)
                }
                restartContinuous(onWakeWordDetected)
            }

            override fun onError(error: Int) {
                restartContinuous(onWakeWordDetected)
            }

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
            }, 300)
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
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                onResult(matches?.firstOrNull() ?: "")
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

    private fun buildIntent() = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
        putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
    }

    fun destroy() {
        isListeningContinuously = false
        stopInternal()
    }
}
