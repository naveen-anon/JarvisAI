package com.jarvis.ai.controller

import com.jarvis.ai.data.model.ArmorSuit
import com.jarvis.ai.data.model.SuitMark
import com.jarvis.ai.data.repository.SuitRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object ArmorController {
    private val repository = SuitRepository()
    private val _currentSuit = MutableStateFlow<ArmorSuit>(repository.getAllSuits().first())
    val currentSuit: StateFlow<ArmorSuit> = _currentSuit.asStateFlow()

    fun equipSuit(suit: ArmorSuit) {
        _currentSuit.value = suit
        applySuitConfig(suit)
    }

    fun equipSuitByMark(mark: SuitMark): String {
        val foundSuit = repository.getAllSuits().find { it.mark == mark }
        return if (foundSuit != null) {
            equipSuit(foundSuit)
            "Deploying ${foundSuit.name}. System mode operational."
        } else {
            "Suit mark unavailable, Boss."
        }
    }

    private fun applySuitConfig(suit: ArmorSuit) {
        // Wire Pitch and UI Themes dynamically
        // Integrates with Voice/TTS System Pitch: suit.voicePitch
    }
}
