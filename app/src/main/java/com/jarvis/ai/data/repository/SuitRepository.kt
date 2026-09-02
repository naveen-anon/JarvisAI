package com.jarvis.ai.data.repository

import android.graphics.Color
import com.jarvis.ai.data.model.ArmorSuit
import com.jarvis.ai.data.model.SuitMark

class SuitRepository {
    fun getAllSuits(): List<ArmorSuit> = listOf(
        ArmorSuit(
            id = "mark_1",
            mark = SuitMark.MARK_1,
            name = "Mark I — Standard",
            description = "Balanced mode. Classic HUD, essential AI, stable power draw.",
            primaryColor = Color.parseColor("#808080"),
            secondaryColor = Color.parseColor("#404040"),
            arcReactorColor = Color.parseColor("#FFB300"),
            systemMode = "BALANCED",
            voicePitch = 0.9f
        ),
        ArmorSuit(
            id = "mark_5",
            mark = SuitMark.MARK_5,
            name = "Mark V — Portable",
            description = "Lightweight rapid-deploy. Faster responses, compact UI feel.",
            primaryColor = Color.parseColor("#C0C0C0"),
            secondaryColor = Color.parseColor("#C41E3A"),
            arcReactorColor = Color.parseColor("#00E5FF"),
            systemMode = "RAPID",
            voicePitch = 1.05f
        ),
        ArmorSuit(
            id = "mark_42",
            mark = SuitMark.MARK_42,
            name = "Mark XLII — Prehensile",
            description = "Autonomous-ready. Adaptive theme, full feature set.",
            primaryColor = Color.parseColor("#C41E3A"),
            secondaryColor = Color.parseColor("#F5C518"),
            arcReactorColor = Color.parseColor("#00E5FF"),
            systemMode = "AUTONOMOUS",
            voicePitch = 1.0f
        ),
        ArmorSuit(
            id = "mark_50",
            mark = SuitMark.MARK_50,
            name = "Mark L — Nano Tech",
            description = "High-performance. Fluid feel, max processing profile.",
            primaryColor = Color.parseColor("#D32F2F"),
            secondaryColor = Color.parseColor("#FFD54F"),
            arcReactorColor = Color.parseColor("#00E5FF"),
            systemMode = "HIGH_PERFORMANCE",
            voicePitch = 1.0f
        ),
        ArmorSuit(
            id = "mark_85",
            mark = SuitMark.MARK_85,
            name = "Mark LXXXV — Endgame",
            description = "Maximum output profile. Bright reactor, sharp presence.",
            primaryColor = Color.parseColor("#B71C1C"),
            secondaryColor = Color.parseColor("#FFD740"),
            arcReactorColor = Color.parseColor("#40C4FF"),
            systemMode = "ENDGAME",
            voicePitch = 0.95f
        ),
        ArmorSuit(
            id = "hulkbuster",
            mark = SuitMark.HULKBUSTER,
            name = "Mark XLIV — Hulkbuster",
            description = "Max security profile. Lower voice pitch, heavy presence.",
            primaryColor = Color.parseColor("#8B0000"),
            secondaryColor = Color.parseColor("#FF8C00"),
            arcReactorColor = Color.parseColor("#FF3D00"),
            systemMode = "MAX_SECURITY",
            voicePitch = 0.7f
        )
    )

    fun byMark(mark: SuitMark): ArmorSuit? = getAllSuits().find { it.mark == mark }
    fun byId(id: String): ArmorSuit? = getAllSuits().find { it.id == id }
}
