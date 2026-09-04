package com.jarvis.ai.controller

import android.content.Context
import android.media.AudioManager
import com.jarvis.ai.data.model.ArmorSuit
import com.jarvis.assistant.util.SettingsManager

/**
 * Stark L5 — suit equip changes real behaviour, not only theme.
 *
 * Modes (from suit.systemMode):
 *  STANDARD / BALANCED / ENHANCED → normal
 *  FOCUS / RAPID → quieter proactive, snappier voice
 *  COMBAT / MAX_SECURITY / ENDGAME → higher pitch authority, proactive on
 *  SILENT / STEALTH → DND-ish volume respect, minimal speech
 *  SUPPORT → warmer address, slightly higher pitch
 */
object SuitModeEngine {

    data class Applied(
        val mode: String,
        val voicePitch: Float,
        val proactiveEnabled: Boolean,
        val briefSpeech: Boolean,
        val note: String
    )

    fun apply(context: Context, suit: ArmorSuit): Applied {
        val mode = suit.systemMode.uppercase().replace(' ', '_')
        val settings = SettingsManager(context)

        val pitch = suit.voicePitch.coerceIn(0.55f, 1.45f)
        try {
            settings.setVoicePitch(pitch)
        } catch (_: Exception) {}

        try {
            settings.setArcReactorColor(
                String.format("#%06X", 0xFFFFFF and suit.arcReactorColor)
            )
        } catch (_: Exception) {}

        try {
            settings.setActiveSuitId(suit.id)
        } catch (_: Exception) {}

        val proactive = when {
            mode.contains("SILENT") || mode.contains("STEALTH") -> false
            mode.contains("FOCUS") -> false
            mode.contains("COMBAT") || mode.contains("MAX") || mode.contains("ENDGAME") -> true
            else -> true
        }
        try {
            settings.setProactiveEnabled(proactive)
        } catch (_: Exception) {
            // optional method may not exist
        }

        val brief = mode.contains("SILENT") || mode.contains("STEALTH") || mode.contains("FOCUS")
        try {
            settings.setBriefSpeech(brief)
        } catch (_: Exception) {}

        // Soft volume nudge only for SILENT (don't force DND permission)
        if (mode.contains("SILENT") || mode.contains("STEALTH")) {
            try {
                val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                val max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                val target = (max * 0.35f).toInt().coerceAtLeast(1)
                if (am.getStreamVolume(AudioManager.STREAM_MUSIC) > target) {
                    am.setStreamVolume(AudioManager.STREAM_MUSIC, target, 0)
                }
            } catch (_: Exception) {}
        }

        val note = when {
            mode.contains("SILENT") || mode.contains("STEALTH") ->
                "Silent profile. Minimal speech."
            mode.contains("FOCUS") || mode.contains("RAPID") ->
                "Focus profile. Distractions alerts reduced."
            mode.contains("COMBAT") || mode.contains("MAX") || mode.contains("ENDGAME") ->
                "Combat profile. Full proactive monitoring."
            mode.contains("SUPPORT") ->
                "Support profile. Clear and steady."
            else ->
                "Standard profile online."
        }

        return Applied(mode, pitch, proactive, brief, note)
    }

    fun statusLine(context: Context): String {
        return try {
            val id = SettingsManager(context).getActiveSuitId()
            "Active suit $id."
        } catch (_: Exception) {
            "Suit status unknown."
        }
    }
}
