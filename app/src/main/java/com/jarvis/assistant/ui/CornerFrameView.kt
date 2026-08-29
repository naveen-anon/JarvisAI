package com.jarvis.assistant.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

/**
 * Draws angular corner brackets + small circuit-line ticks in each corner,
 * matching the Stark-Industries HUD frame style from the reference screenshots.
 * Use as a full-size background layer BEHIND your content (draws only near edges,
 * so it never blocks touches/content in the middle).
 */
class CornerFrameView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val accent = Color.parseColor("#00E5FF")
    private val accentDim = Color.parseColor("#0B7A94")

    private val pLine = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 4f
        strokeCap = Paint.Cap.ROUND
        color = accent
    }

    private val pDot = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = accent
    }

    private val pTick = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2.5f
        color = accentDim
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        val bracket = 42f     // bracket arm length
        val inset = 6f        // distance from edge

        drawCorner(canvas, inset, inset, 1f, 1f, bracket)               // top-left
        drawCorner(canvas, w - inset, inset, -1f, 1f, bracket)          // top-right
        drawCorner(canvas, inset, h - inset, 1f, -1f, bracket)          // bottom-left
        drawCorner(canvas, w - inset, h - inset, -1f, -1f, bracket)     // bottom-right

        // small dash ticks running along the top edge, like circuit traces
        drawEdgeTicks(canvas, w)
    }

    /**
     * Draws one "L"-shaped bracket at (cx, cy), with the horizontal arm extending
     * in dirX direction and the vertical arm extending in dirY direction.
     */
    private fun drawCorner(canvas: Canvas, cx: Float, cy: Float, dirX: Float, dirY: Float, len: Float) {
        canvas.drawLine(cx, cy, cx + len * dirX, cy, pLine)   // horizontal arm
        canvas.drawLine(cx, cy, cx, cy + len * dirY, pLine)   // vertical arm
        canvas.drawCircle(cx, cy, 3.5f, pDot)                 // corner accent dot
    }

    private fun drawEdgeTicks(canvas: Canvas, w: Float) {
        val y = 2f
        var x = 90f
        var toggle = true
        while (x < w - 90f) {
            val segLen = if (toggle) 14f else 6f
            canvas.drawLine(x, y, x + segLen, y, pTick)
            x += segLen + 8f
            toggle = !toggle
        }
    }
}
