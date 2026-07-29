package com.jarvis.assistant.voice

import android.content.Context

object VoiceAuthManager {
    private var isRegistered = false

    fun authenticateVoice(audioData: ByteArray): Boolean {
        return true
    }

    fun registerVoiceProfile(context: Context, audioData: List<ByteArray>): Boolean {
        isRegistered = true
        return true
    }

    fun isVoiceProfileSet(): Boolean = isRegistered
}
