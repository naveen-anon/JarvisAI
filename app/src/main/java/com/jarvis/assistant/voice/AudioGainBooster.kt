package com.jarvis.assistant.voice

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.media.audiofx.AutomaticGainControl
import android.media.audiofx.NoiseSuppressor
import android.util.Log
import kotlin.math.sqrt

class AudioGainBooster(
    private val softwareGain: Float = 4.0f,
    private val triggerRms: Double = 300.0
) {
    private var audioRecord: AudioRecord? = null
    private var agc: AutomaticGainControl? = null
    private var noiseSuppressor: NoiseSuppressor? = null
    private var running = false
    private var listenerThread: Thread? = null

    private val sampleRate = 16000
    private val bufferSize = AudioRecord.getMinBufferSize(
        sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT
    )

    @Suppress("MissingPermission")
    fun startListening(onVoiceDetected: () -> Unit) {
        if (running) return
        running = true

        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.VOICE_RECOGNITION,
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferSize * 2
        )

        val sessionId = audioRecord?.audioSessionId ?: -1
        if (sessionId != -1) {
            if (AutomaticGainControl.isAvailable()) {
                agc = AutomaticGainControl.create(sessionId)?.apply { enabled = true }
            }
            if (NoiseSuppressor.isAvailable()) {
                noiseSuppressor = NoiseSuppressor.create(sessionId)?.apply { enabled = true }
            }
        }

        audioRecord?.startRecording()

        listenerThread = Thread {
            val buffer = ShortArray(bufferSize)
            while (running) {
                val read = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                if (read <= 0) continue

                var sumSquares = 0.0
                for (i in 0 until read) {
                    val boosted = (buffer[i] * softwareGain).toInt()
                        .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                    sumSquares += (boosted * boosted).toDouble()
                }
                val rms = sqrt(sumSquares / read)

                if (rms > triggerRms) {
                    running = false
                    onVoiceDetected()
                    break
                }
            }
        }
        listenerThread?.start()
    }

    fun stop() {
        running = false
        try {
            agc?.release()
            noiseSuppressor?.release()
            audioRecord?.stop()
            audioRecord?.release()
        } catch (e: Exception) {
            Log.e("AudioGainBooster", "stop() cleanup error", e)
        }
        audioRecord = null
        agc = null
        noiseSuppressor = null
    }
}
