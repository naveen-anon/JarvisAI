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

/**
 * Enhanced wake-word + speech pipeline for J.A.R.V.I.S.
 * - Many English / Hindi / romanized variants
 * - Partial-result early trigger (faster response)
 * - Word-boundary + fuzzy matching to cut false positives
 * - Soft-voice tuned silence windows
 * - Debounce so one "Jarvis" doesn't fire twice
 */
class SpeechToText(private val context: Context) {

    private var recognizer: SpeechRecognizer? = null
    private var isListeningContinuously = false
    private var lastWakeMs = 0L
    private val mainHandler = Handler(Looper.getMainLooper())

    // Primary + common mis-hearings from Android SpeechRecognizer
    private val wakeWords = listOf(
        // English / romanized
        "jarvis", "jarwis", "jaarvis", "jarviz", "jarves", "jarvus",
        "jervis", "jerves", "jarvish", "jarvis.", "jarvis,",
        // Hindi Devanagari
        "जार्विस", "जारविस", "जर्विस", "जारवीस",
        // Phrases
        "hey jarvis", "ok jarvis", "okay jarvis", "hi jarvis",
        "yo jarvis", "hello jarvis", "jarvis ji", "jarvis bhai"
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
                // 7 = ERROR_NO_MATCH, 6 = ERROR_SPEECH_TIMEOUT — normal for continuous
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
        if (now - lastWakeMs < 1800) return // debounce 1.8s

        val match = findWakeWord(heard) ?: return
        lastWakeMs = now
        onFired()

        // Everything after the wake word is the command (may be empty → just open listen cycle)
        val trailing = extractTrailing(heard, match)
        Log.d("SpeechToText", "Wake matched='$match' trailing='$trailing'")
        onWakeWordDetected(trailing)
    }

    /** Prefer longer phrase matches first (e.g. "hey jarvis" over "jarvis"). */
    private fun findWakeWord(heard: String): String? {
        val sorted = wakeWords.sortedByDescending { it.length }
        for (w in sorted) {
            if (matchesWake(heard, w)) return w
        }
        // Fuzzy: allow 1-char drift on short english tokens (jarvis ↔ jarwis already listed)
        val tokens = heard.split(Regex("\\s+"))
        for (token in tokens) {
            if (token.length in 5..8) {
                for (w in wakeWords.filter { it.length in 5..8 && !it.contains(" ") }) {
                    if (levenshtein(token, w) <= 1) return w
                }
            }
        }
        return null
    }

    private fun matchesWake(heard: String, wake: String): Boolean {
        if (heard == wake) return true
        if (heard.startsWith("$wake ")) return true
        if (heard.endsWith(" $wake")) return true
        if (heard.contains(" $wake ")) return true
        // Hindi / no-space edge cases
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
        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            ?: return ""
        if (matches.isEmpty()) return ""
        // Prefer the candidate that contains a wake word; else longest non-blank
        val withWake = matches.firstOrNull { m ->
            val lower = m.lowercase()
            wakeWords.any { matchesWake(lower, it) }
        }
        if (withWake != null) return withWake
        return matches.filter { it.isNotBlank() }.maxByOrNull { it.trim().length }
            ?: matches.first()
    }

    private fun buildIntent(forWakeWord: Boolean) = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
        putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)

        // Prefer device locale; also bias toward Indian English / Hindi when available
        val locale = Locale.getDefault()
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, locale.toString())
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, locale.toString())

        if (forWakeWord) {
            // Slightly shorter windows so "Jarvis" alone is accepted quickly
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1200L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 800L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 300L)
        } else {
            // Command capture — soft voice friendly
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 2000L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1200L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 400L)
        }
    }

    private fun levenshtein(a: String, b: String): Int {
        val m = a.length
        val n = b.length
        val dp = Array(m + 1) { IntArray(n + 1) }
        for (i in 0..m) dp[i][0] = i
        for (j in 0..n) dp[0][j] = j
        for (i in 1..m) {
            for (j in 1..n) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                dp[i][j] = minOf(dp[i - 1][j] + 1, dp[i][j - 1] + 1, dp[i - 1][j - 1] + cost)
            }
        }
        return dp[m][n]
    }

    fun destroy() {
        isListeningContinuously = false
        mainHandler.removeCallbacksAndMessages(null)
        stopInternal()
    }
}
