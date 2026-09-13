package com.jarvis.assistant.armor

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.jarvis.assistant.R

class ArmorSuitsActivity : AppCompatActivity() {

    private data class SuitUi(
        val id: String,
        val label: String,
        val title: String,
        val desc: String,
        val stats: String,
        val mode: String,
        val chipId: Int
    )

    private val suits = listOf(
        SuitUi("mark_1", "MK I", "MARK I · PROTOTYPE",
            "Prototype armor. Heavy plating. Limited mobility.",
            "PWR · 42%   FLIGHT · NO   WEAPONS · BASIC",
            "VOICE PITCH · 0.95   REACTOR · GOLD", R.id.chip_mk1),
        SuitUi("mark_3", "MK III", "MARK III · CLASSIC",
            "First fully flight-capable combat suit.",
            "PWR · 78%   FLIGHT · YES   WEAPONS · REPULSOR",
            "VOICE PITCH · 1.00   REACTOR · GOLD", R.id.chip_mk3),
        SuitUi("mark_5", "MK V", "MARK V · BRIEFCASE",
            "Portable deploy. Rapid fold/unfold.",
            "PWR · 70%   FLIGHT · YES   WEAPONS · LIGHT",
            "VOICE PITCH · 1.05   REACTOR · GOLD", R.id.chip_mk5),
        SuitUi("mark_42", "MK 42", "MARK 42 · PREHENSILE",
            "Autonomous plate assembly. Remote call-in.",
            "PWR · 88%   FLIGHT · YES   WEAPONS · FULL",
            "VOICE PITCH · 1.00   REACTOR · CYAN-GOLD", R.id.chip_mk42),
        SuitUi("mark_50", "MK 50", "MARK 50 · NANO",
            "Nanotech morph. Adaptive weapons.",
            "PWR · 95%   FLIGHT · YES   WEAPONS · ADAPTIVE",
            "VOICE PITCH · 1.02   REACTOR · GOLD", R.id.chip_mk50),
        SuitUi("mark_85", "MK 85", "MARK 85 · ENDGAME",
            "Peak nanotech combat configuration.",
            "PWR · 99%   FLIGHT · YES   WEAPONS · MAX",
            "VOICE PITCH · 1.00   REACTOR · GOLD", R.id.chip_mk85),
        SuitUi("hulkbuster", "HULKBUSTER", "HULKBUSTER · HEAVY",
            "Heavy modular frame. Extreme load.",
            "PWR · 90%   FLIGHT · LIMITED   WEAPONS · HEAVY",
            "VOICE PITCH · 0.90   REACTOR · GREEN-GOLD", R.id.chip_hulk)
    )

    private var selected = suits[0]

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_armor_suits)
        bindChips()
        render(selected)
        findViewById<TextView>(R.id.btnEquipArmor).setOnClickListener { equip(selected) }
        findViewById<TextView>(R.id.tabArmorHome).setOnClickListener { finish() }
    }

    private fun bindChips() {
        suits.forEach { s ->
            findViewById<TextView>(s.chipId).setOnClickListener {
                selected = s
                render(s)
            }
        }
    }

    private fun render(s: SuitUi) {
        findViewById<TextView>(R.id.txtSelectedMark).text = s.title
        findViewById<TextView>(R.id.txtArmorDesc).text = s.desc
        findViewById<TextView>(R.id.txtArmorStats).text = s.stats
        findViewById<TextView>(R.id.txtArmorMode).text = s.mode
        suits.forEach {
            findViewById<TextView>(it.chipId).setBackgroundResource(
                if (it.id == s.id) R.drawable.bg_jg_card_selected
                else R.drawable.bg_jg_chip
            )
        }
    }

    private fun equip(s: SuitUi) {
        try {
            val sm = Class.forName("com.jarvis.assistant.util.SettingsManager")
            val inst = sm.getConstructor(android.content.Context::class.java).newInstance(this)
            sm.getMethod("setActiveSuitId", String::class.java).invoke(inst, s.id)
        } catch (_: Throwable) { }
        findViewById<TextView>(R.id.btnEquipArmor).text = "EQUIPPED · ${s.label}"
    }
}
