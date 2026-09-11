package com.jarvis.assistant.armor

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.FrameLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

/**
 * Full-screen Iron Man 3 style Hall of Armor.
 * Pan / pinch / long-press-drag suits on a cyan holographic grid.
 */
class ArmorHoloArchiveActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.parseColor("#020B14")
        window.navigationBarColor = Color.parseColor("#020B14")

        val root = FrameLayout(this).apply {
            setBackgroundColor(Color.parseColor("#020B14"))
        }

        val grid = HoloArmorGridView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            listener = object : HoloArmorGridView.Listener {
                override fun onSuitSelected(mark: ArmorMark) {
                    // soft feedback only
                }
                override fun onSuitActivated(mark: ArmorMark) {
                    // Double-tap → open detail
                    startActivity(
                        Intent(this@ArmorHoloArchiveActivity, ArmorDetailActivity::class.java)
                            .putExtra(ArmorDetailActivity.EXTRA_MARK, mark.number)
                    )
                }
            }
        }
        root.addView(grid)

        // Close chip
        val close = android.widget.TextView(this).apply {
            text = "✕"
            setTextColor(Color.parseColor("#00E5FF"))
            textSize = 22f
            setPadding(36, 36, 36, 36)
            setOnClickListener { finish() }
        }
        root.addView(close, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = android.view.Gravity.TOP or android.view.Gravity.END
        })

        setContentView(root)
    }
}
