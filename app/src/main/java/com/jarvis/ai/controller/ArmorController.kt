package com.jarvis.ai.controller

import android.content.Context
import android.content.Intent
import com.jarvis.ai.data.model.ArmorSuit
import com.jarvis.ai.data.model.SuitMark
import com.jarvis.ai.data.repository.SuitRepository
import com.jarvis.assistant.util.SettingsManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object ArmorController {
    const val ACTION_SUIT_CHANGED = "com.jarvis.assistant.SUIT_CHANGED"

    private val repository = SuitRepository()
    private var appContext: Context? = null

    private val _currentSuit = MutableStateFlow(repository.getAllSuits().first())
    val currentSuit: StateFlow<ArmorSuit> = _currentSuit.asStateFlow()

    /** Call from Application.onCreate (or first Activity). */
    fun init(context: Context) {
        appContext = context.applicationContext
        val settings = SettingsManager(context.applicationContext)
        val savedId = settings.getActiveSuitId()
        val saved = repository.byId(savedId)
        if (saved != null) {
            _currentSuit.value = saved
            // Re-apply persisted suit without re-broadcast storm on cold start
            applySuitConfig(saved, broadcast = false)
        }
    }

    fun equipSuit(suit: ArmorSuit) {
        _currentSuit.value = suit
        applySuitConfig(suit, broadcast = true)
    }

    fun equipSuitByMark(mark: SuitMark): String {
        val found = repository.byMark(mark)
        return if (found != null) {
            equipSuit(found)
            "Deploying ${found.name}. Mode ${found.systemMode} online."
        } else {
            "Suit mark unavailable, Boss."
        }
    }

    private fun applySuitConfig(suit: ArmorSuit, broadcast: Boolean) {
        val ctx = appContext ?: return
        val settings = SettingsManager(ctx)

        // Voice pitch → TTS reads this on every speak
        settings.setVoicePitch(suit.voicePitch.coerceIn(0.5f, 1.5f))

        // Arc reactor / HUD accent
        settings.setArcReactorColor(toHex(suit.arcReactorColor))

        // Optional secondary theme keys
        settings.setSuitPrimaryColor(toHex(suit.primaryColor))
        settings.setSuitSecondaryColor(toHex(suit.secondaryColor))
        settings.setSystemMode(suit.systemMode)
        settings.setActiveSuitId(suit.id)

        if (broadcast) {
            try {
                ctx.sendBroadcast(Intent(ACTION_SUIT_CHANGED).setPackage(ctx.packageName))
            } catch (_: Exception) {
            }
        }
    }

    private fun toHex(color: Int): String =
        String.format("#%06X", 0xFFFFFF and color)
}
