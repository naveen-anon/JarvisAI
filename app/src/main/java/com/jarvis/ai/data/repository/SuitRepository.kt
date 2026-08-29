package com.jarvis.ai.data.repository

import androidx.compose.ui.graphics.Color
import com.jarvis.ai.data.model.ArmorSuit
import com.jarvis.ai.data.model.SuitMark

class SuitRepository {
    fun getAllSuits(): List<ArmorSuit> {
        return listOf(
            ArmorSuit(
                id = "mark_1",
                mark = SuitMark.MARK_1,
                name = "Mark I - Standard Armor",
                description = "Basic functional UI with essential AI features and classic themes.",
                primaryColor = Color(0xFF808080),
                secondaryColor = Color(0xFF404040),
                arcReactorColor = Color(0xFFFFB300),
                systemMode = "BALANCED",
                voicePitch = 0.9f
            ),
            ArmorSuit(
                id = "mark_50",
                mark = SuitMark.MARK_50,
                name = "Mark L - Nano Tech",
                description = "High-performance mode with fluid animations, vision AI, and max processing power.",
                primaryColor = Color(0xFFD32F2F),
                secondaryColor = Color(0xFFFFD54F),
                arcReactorColor = Color(0xFF00E5FF),
                systemMode = "HIGH_PERFORMANCE",
                voicePitch = 1.0f
            ),
            ArmorSuit(
                id = "hulkbuster",
                mark = SuitMark.HULKBUSTER,
                name = "Mark XLIV - Hulkbuster",
                description = "Heavy duty security mode. Activates App Lock, Focus Mode, and max security controls.",
                primaryColor = Color(0xFF8B0000),
                secondaryColor = Color(0xFFFF8C00),
                arcReactorColor = Color(0xFFFF3D00),
                systemMode = "MAX_SECURITY",
                voicePitch = 0.7f
            )
        )
    }
}
