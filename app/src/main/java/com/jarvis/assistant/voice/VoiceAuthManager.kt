package com.jarvis.assistant.voice

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.core.content.ContextCompat
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * "Only my voice can use Jarvis" — Phase 5 style feature. This is a lightweight,
 * dependency-free speaker verification: it captures a short audio sample, turns it into
 * an averaged log-mel-filterbank "voiceprint" (a much simpler cousin of what real speaker
 * embedding models compute), and compares new samples against the enrolled one with
 * cosine similarity.
 *
 * Honest limits: this is NOT a trained neural speaker-ID model (those need a pretrained
 * embedding network this environment can't produce or download). It's good enough to
 * reject a clearly different voice for a personal single-user assistant — it is not
 * hardened against someone deliberately trying to mimic your voice or replay a recording.
 *
 * All computation and storage is on-device; no audio ever leaves the phone.
 */
class VoiceAuthManager(private val context: Context) {

    private val prefs = context.getSharedPreferences("jarvis_voice_auth", Context.MODE_PRIVATE)
    private val sampleRate = 16000
    private val frameSize = 512   // power of 2, required by the FFT below (~32ms @ 16kHz)
    private val hopSize = 256     // 50% overlap between frames
    private val melBands = 20

    fun isEnabled(): Boolean = prefs.getBoolean("enabled", false)
    fun setEnabled(enabled: Boolean) = prefs.edit().putBoolean("enabled", enabled).apply()
    fun isEnrolled(): Boolean = prefs.contains("voiceprint")

    fun resetEnrollment() {
        prefs.edit().remove("voiceprint").putBoolean("enabled", false).apply()
    }

    /**
     * Blocking — always call from a background thread (e.g. Dispatchers.IO).
     * Records [durationMs] of mono 16-bit PCM audio. Returns null on missing permission,
     * a device that refuses to initialize the recorder, or too little audio captured.
     */
    fun captureSample(durationMs: Int = 1500): ShortArray? {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) return null

        val minBuf = AudioRecord.getMinBufferSize(
            sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT
        )
        if (minBuf <= 0) return null

        val recorder = try {
            AudioRecord(
                MediaRecorder.AudioSource.MIC, sampleRate,
                AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, minBuf * 2
            )
        } catch (e: SecurityException) {
            return null
        }

        if (recorder.state != AudioRecord.STATE_INITIALIZED) {
            recorder.release()
            return null
        }

        val totalSamples = (sampleRate.toLong() * durationMs / 1000).toInt()
        val buffer = ShortArray(totalSamples)
        var read = 0

        try {
            recorder.startRecording()
            while (read < totalSamples) {
                val n = recorder.read(buffer, read, totalSamples - read)
                if (n <= 0) break
                read += n
            }
        } finally {
            recorder.stop()
            recorder.release()
        }

        // Need at least ~0.4s of audio for a usable feature vector.
        return if (read > sampleRate * 4 / 10) buffer.copyOf(read) else null
    }

    /** Averages [samplesList]'s features into one stored voiceprint. */
    fun enrollFromSamples(samplesList: List<ShortArray>): Boolean {
        if (samplesList.isEmpty()) return false
        val features = samplesList.map { extractFeatures(it) }
        val avg = FloatArray(melBands)
        for (f in features) for (i in 0 until melBands) avg[i] += f[i]
        for (i in 0 until melBands) avg[i] = avg[i] / features.size.toFloat()

        prefs.edit().putString("voiceprint", avg.joinToString(",")).apply()
        return true
    }

    /** True if [samples] is close enough to the enrolled voiceprint (or nothing enrolled yet). */
    fun verify(samples: ShortArray, threshold: Float = 0.75f): Boolean {
        val stored = prefs.getString("voiceprint", null) ?: return true
        val storedVec = stored.split(",").mapNotNull { it.toFloatOrNull() }.toFloatArray()
        if (storedVec.size != melBands) return true // corrupted/legacy data — fail open
        val candidate = extractFeatures(samples)
        return cosineSimilarity(storedVec, candidate) >= threshold
    }

    private fun extractFeatures(samples: ShortArray): FloatArray {
        val floatSamples = FloatArray(samples.size) { samples[it] / 32768f }
        val melFilters = buildMelFilterbank()
        val accum = FloatArray(melBands)
        var frameCount = 0

        var offset = 0
        while (offset + frameSize <= floatSamples.size) {
            val frame = FloatArray(frameSize)
            for (i in 0 until frameSize) {
                val w = 0.54f - 0.46f * cos(2 * PI * i / (frameSize - 1)).toFloat() // Hamming
                frame[i] = floatSamples[offset + i] * w
            }
            val spectrum = magnitudeSpectrum(frame)
            for (b in 0 until melBands) {
                var energy = 0f
                for (k in spectrum.indices) energy += spectrum[k] * melFilters[b][k]
                accum[b] += ln(energy.coerceAtLeast(1e-8f))
            }
            frameCount++
            offset += hopSize
        }

        if (frameCount == 0) return FloatArray(melBands)
        for (b in 0 until melBands) accum[b] = accum[b] / frameCount.toFloat()

        // Mean-normalize so overall loudness/mic gain doesn't dominate the comparison.
        val mean = accum.average().toFloat()
        return FloatArray(melBands) { accum[it] - mean }
    }

    private fun magnitudeSpectrum(frame: FloatArray): FloatArray {
        val n = frame.size
        val real = frame.copyOf()
        val imag = FloatArray(n)
        fft(real, imag)
        val half = n / 2
        return FloatArray(half) { sqrt(real[it] * real[it] + imag[it] * imag[it]) }
    }

    /** In-place iterative radix-2 Cooley-Tukey FFT. Requires size to be a power of 2. */
    private fun fft(real: FloatArray, imag: FloatArray) {
        val n = real.size
        var j = 0
        for (i in 1 until n) {
            var bit = n shr 1
            while (j and bit != 0) {
                j = j xor bit
                bit = bit shr 1
            }
            j = j or bit
            if (i < j) {
                var tmp = real[i]; real[i] = real[j]; real[j] = tmp
                tmp = imag[i]; imag[i] = imag[j]; imag[j] = tmp
            }
        }
        var len = 2
        while (len <= n) {
            val ang = -2 * PI / len
            val wr = cos(ang).toFloat()
            val wi = sin(ang).toFloat()
            var i = 0
            while (i < n) {
                var curWr = 1f
                var curWi = 0f
                for (k in 0 until len / 2) {
                    val uR = real[i + k]; val uI = imag[i + k]
                    val vR = real[i + k + len / 2] * curWr - imag[i + k + len / 2] * curWi
                    val vI = real[i + k + len / 2] * curWi + imag[i + k + len / 2] * curWr
                    real[i + k] = uR + vR; imag[i + k] = uI + vI
                    real[i + k + len / 2] = uR - vR; imag[i + k + len / 2] = uI - vI
                    val nWr = curWr * wr - curWi * wi
                    val nWi = curWr * wi + curWi * wr
                    curWr = nWr; curWi = nWi
                }
                i += len
            }
            len = len shl 1
        }
    }

    private fun buildMelFilterbank(): Array<FloatArray> {
        val half = frameSize / 2
        fun hzToMel(hz: Float) = 2595f * log10(1f + hz / 700f)
        fun melToHz(mel: Float) = 700f * (10f.pow(mel / 2595f) - 1f)

        val minMel = hzToMel(0f)
        val maxMel = hzToMel(sampleRate / 2f)
        val melPoints = FloatArray(melBands + 2) { minMel + (maxMel - minMel) * it / (melBands + 1) }
        val hzPoints = melPoints.map { melToHz(it) }
        val bin = hzPoints.map { (it * frameSize / sampleRate).toInt().coerceIn(0, half - 1) }

        return Array(melBands) { b ->
            val filter = FloatArray(half)
            val left = bin[b]; val center = bin[b + 1]; val right = bin[b + 2]
            for (k in left until center) {
                if (center > left) filter[k] = (k - left).toFloat() / (center - left)
            }
            for (k in center until right) {
                if (right > center) filter[k] = (right - k).toFloat() / (right - center)
            }
            filter
        }
    }

    private fun cosineSimilarity(a: FloatArray, b: FloatArray): Float {
        var dot = 0f; var normA = 0f; var normB = 0f
        for (i in a.indices) {
            dot += a[i] * b[i]
            normA += a[i] * a[i]
            normB += b[i] * b[i]
        }
        if (normA == 0f || normB == 0f) return 0f
        return dot / (sqrt(normA) * sqrt(normB))
    }
}
