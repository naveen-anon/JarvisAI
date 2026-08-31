package com.jarvis.ai.data.repository

import android.graphics.Color
import com.jarvis.ai.data.model.ArmorSuit
import com.jarvis.ai.data.model.SuitMark

class SuitRepository {
    fun getAllSuits(): List<ArmorSuit> = listOf(
        ArmorSuit("mark_1", SuitMark.MARK_1, "Mark I — Standard",
            "Balanced mode. Classic HUD.", Color.parseColor("#808080"), Color.parseColor("#404040"),
            Color.parseColor("#FFB300"), "BALANCED", 0.9f),
        ArmorSuit("mark_5", SuitMark.MARK_5, "Mark V — Portable",
            "Rapid deploy profile.", Color.parseColor("#C0C0C0"), Color.parseColor("#C41E3A"),
            Color.parseColor("#00E5FF"), "RAPID", 1.05f),
        ArmorSuit("mark_42", SuitMark.MARK_42, "Mark XLII — Prehensile",
            "Autonomous-ready.", Color.parseColor("#C41E3A"), Color.parseColor("#F5C518"),
            Color.parseColor("#00E5FF"), "AUTONOMOUS", 1.0f),
        ArmorSuit("mark_50", SuitMark.MARK_50, "Mark L — Nano Tech",
            "High-performance.", Color.parseColor("#D32F2F"), Color.parseColor("#FFD54F"),
            Color.parseColor("#00E5FF"), "HIGH_PERFORMANCE", 1.0f),
        ArmorSuit("mark_85", SuitMark.MARK_85, "Mark LXXXV — Endgame",
            "Maximum output.", Color.parseColor("#B71C1C"), Color.parseColor("#FFD740"),
            Color.parseColor("#40C4FF"), "ENDGAME", 0.95f),
        ArmorSuit("hulkbuster", SuitMark.HULKBUSTER, "Mark XLIV — Hulkbuster",
            "Max security, lower pitch.", Color.parseColor("#8B0000"), Color.parseColor("#FF8C00"),
            Color.parseColor("#FF3D00"), "MAX_SECURITY", 0.7f)
    )
    fun byMark(mark: SuitMark) = getAllSuits().find { it.mark == mark }
    fun byId(id: String) = getAllSuits().find { it.id == id }
}
