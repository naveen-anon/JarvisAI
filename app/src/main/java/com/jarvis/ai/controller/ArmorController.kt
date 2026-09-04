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

    fun init(context: Context) {
        appContext = context.applicationContext
        repository.byId(SettingsManager(context.applicationContext).getActiveSuitId())?.let {
            _currentSuit.value = it
            applySuitConfig(it, false)
        }
    }

    fun equipSuit(suit: ArmorSuit) {
        // behaviour apply is done from UI with Context
        _currentSuit.value = suit
        applySuitConfig(suit, true)
    }

    fun equipSuitByMark(mark: SuitMark): String {
        val found = repository.byMark(mark) ?: return "Suit mark unavailable, Boss."
        equipSuit(found)
        return "Deploying ${found.name}. Mode ${found.systemMode} online."
    }

    private fun applySuitConfig(suit: ArmorSuit, broadcast: Boolean) {
        val ctx = appContext ?: return
        val s = SettingsManager(ctx)
        s.setVoicePitch(suit.voicePitch.coerceIn(0.5f, 1.5f))
        s.setArcReactorColor(String.format("#%06X", 0xFFFFFF and suit.arcReactorColor))
        s.setSuitPrimaryColor(String.format("#%06X", 0xFFFFFF and suit.primaryColor))
        s.setSuitSecondaryColor(String.format("#%06X", 0xFFFFFF and suit.secondaryColor))
        s.setSystemMode(suit.systemMode)
        s.setActiveSuitId(suit.id)
        if (broadcast) {
            try { ctx.sendBroadcast(Intent(ACTION_SUIT_CHANGED).setPackage(ctx.packageName)) } catch (_: Exception) {}
        }
    }

    fun equipWithContext(context: android.content.Context, suit: ArmorSuit): String {
        equipSuit(suit)
        val applied = SuitModeEngine.apply(context, suit)
        return "${suit.name}: ${applied.note}"
    }

}
