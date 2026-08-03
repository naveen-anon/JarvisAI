package com.jarvis.assistant.ui.theme

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke

// Shared palette
val BgDeep = Color(0xFF02040A)
val PanelBg = Color(0xFF0B1520)
val BorderCyan = Color(0xFF16303D)
val Cyan = Color(0xFF00D4FF)
val CyanDim = Color(0xFF4A8A9A)
val TextMain = Color(0xFFEAF6FA)
val TextDim = Color(0xFF7FA3AD)
val Success = Color(0xFF00FF88)

/**
 * Draws glowing corner brackets in each corner of the screen/card,
 * matching the Stark-Industries style frame from the reference screenshots.
 * Put this behind your content with Box { HudCornerBrackets(); YourContent() }
 */
@Composable
fun HudCornerBrackets(modifier: Modifier = Modifier, size: Float = 36f, inset: Float = 14f) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val w = this.size.width
        val h = this.size.height
        val stroke = Stroke(width = 2.5f)

        fun corner(x: Float, y: Float, dx: Float, dy: Float) {
            drawLine(Cyan, Offset(x, y), Offset(x + dx * size, y), strokeWidth = stroke.width)
            drawLine(Cyan, Offset(x, y), Offset(x, y + dy * size), strokeWidth = stroke.width)
        }

        corner(inset, inset, 1f, 1f)                  // top-left
        corner(w - inset, inset, -1f, 1f)              // top-right
        corner(inset, h - inset, 1f, -1f)               // bottom-left
        corner(w - inset, h - inset, -1f, -1f)          // bottom-right
    }
}
