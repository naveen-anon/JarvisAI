package com.jarvis.ai.data.repository

import android.graphics.Color
import com.jarvis.ai.data.model.ArmorSuit
import com.jarvis.ai.data.model.SuitMark

/** MCU-inspired Mark list — theme + pitch profiles (not copyrighted assets). */
class SuitRepository {
    fun getAllSuits(): List<ArmorSuit> = listOf(
        ArmorSuit("mark_1", SuitMark.MARK_1, "Mark I — Cave Prototype",
            "Gray iron, first flight. Balanced basics.", Color.parseColor("#6B6B6B"), Color.parseColor("#3A3A3A"),
            Color.parseColor("#FFB300"), "BALANCED", 0.88f),
        ArmorSuit("mark_2", SuitMark.MARK_2, "Mark II — Silver Prototype",
            "Silver test frame. Clean HUD.", Color.parseColor("#C0C0C0"), Color.parseColor("#8A8A8A"),
            Color.parseColor("#00E5FF"), "TEST", 0.95f),
        ArmorSuit("mark_3", SuitMark.MARK_3, "Mark III — Classic Gold-Red",
            "Signature red/gold. Standard ops.", Color.parseColor("#C41E3A"), Color.parseColor("#F5C518"),
            Color.parseColor("#00E5FF"), "STANDARD", 1.0f),
        ArmorSuit("mark_5", SuitMark.MARK_5, "Mark V — Briefcase",
            "Portable rapid deploy.", Color.parseColor("#B0B0B0"), Color.parseColor("#C41E3A"),
            Color.parseColor("#00E5FF"), "RAPID", 1.05f),
        ArmorSuit("mark_6", SuitMark.MARK_6, "Mark VI — Triangle Reactor",
            "New element core look.", Color.parseColor("#B71C1C"), Color.parseColor("#FFD740"),
            Color.parseColor("#40C4FF"), "ENHANCED", 1.0f),
        ArmorSuit("mark_7", SuitMark.MARK_7, "Mark VII — Orbital",
            "Fast assemble profile.", Color.parseColor("#C62828"), Color.parseColor("#FFC107"),
            Color.parseColor("#00E5FF"), "COMBAT", 0.98f),
        ArmorSuit("mark_42", SuitMark.MARK_42, "Mark XLII — Prehensile",
            "Autonomous-ready plates.", Color.parseColor("#C41E3A"), Color.parseColor("#F5C518"),
            Color.parseColor("#00E5FF"), "AUTONOMOUS", 1.0f),
        ArmorSuit("mark_46", SuitMark.MARK_46, "Mark XLVI — Civil War",
            "Lean combat profile.", Color.parseColor("#B71C1C"), Color.parseColor("#FFD54F"),
            Color.parseColor("#00BCD4"), "COMBAT", 0.96f),
        ArmorSuit("mark_50", SuitMark.MARK_50, "Mark L — Nano Tech",
            "High-performance nano.", Color.parseColor("#D32F2F"), Color.parseColor("#FFD54F"),
            Color.parseColor("#00E5FF"), "HIGH_PERFORMANCE", 1.0f),
        ArmorSuit("mark_85", SuitMark.MARK_85, "Mark LXXXV — Endgame",
            "Maximum output profile.", Color.parseColor("#B71C1C"), Color.parseColor("#FFD740"),
            Color.parseColor("#40C4FF"), "ENDGAME", 0.95f),
        ArmorSuit("hulkbuster", SuitMark.HULKBUSTER, "Mark XLIV — Hulkbuster",
            "Heavy security, lower pitch.", Color.parseColor("#8B0000"), Color.parseColor("#FF8C00"),
            Color.parseColor("#FF3D00"), "MAX_SECURITY", 0.72f),
        ArmorSuit("endgame", SuitMark.MARK_ENDGAME, "Rescue Assist Profile",
            "Support mode — bright core.", Color.parseColor("#1565C0"), Color.parseColor("#FFD740"),
            Color.parseColor("#80D8FF"), "SUPPORT", 1.08f)
    )
    fun byMark(mark: SuitMark) = getAllSuits().find { it.mark == mark }
    fun byId(id: String) = getAllSuits().find { it.id == id }
}
