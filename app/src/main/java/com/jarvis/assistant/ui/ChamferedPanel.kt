package com.jarvis.assistant.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.widget.FrameLayout

/**
 * Angular "HUD panel" container: cut (chamfered) top-right and bottom-left
 * corners instead of plain rounded corners, with a glowing accent border —
 * matches the reference design's card/chip/button shape used throughout
 * (STANDING BY box, stat chips, quick-action buttons, etc).
 *
 * Usage in XML:
 *   <com.jarvis.assistant.ui.ChamferedPanel
 *       android:layout_width="..." android:layout_height="..."> 
 *       ... content ...
 *   </com.jarvis.assistant.ui.ChamferedPanel>
 *
 * Configure accent/fill/chamfer size/corner style from code if needed:
 *   panel.accentColor = Color.parseColor("#A855F7")
 */
class ChamferedPanel @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    /** Border + glow color. */
    var accentColor: Int = Color.parseColor("#00E5FF")
        set(value) { field = value; invalidate() }

    /** Panel fill color (translucent navy by default). */
    var fillColor: Int = Color.parseColor("#E60A1624")
        set(value) { field = value; invalidate() }

    var accentAlpha: Int = 170
        set(value) { field = value; invalidate() }

    /** Which corners get chamfered. "tr-bl" (default) or "tl-br" or "all". */
    var cornerStyle: String = "tr-bl"
        set(value) { field = value; requestLayout(); invalidate() }

    private val density = context.resources.displayMetrics.density
    var chamferDp: Float = 12f
        set(value) { field = value; requestLayout(); invalidate() }

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val path = Path()

    init {
        setWillNotDraw(false)
        setPadding(paddingLeft + dp(4), paddingTop + dp(3), paddingRight + dp(4), paddingBottom + dp(3))
    }

    private fun dp(v: Int) = (v * density).toInt()

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        buildPath(w.toFloat(), h.toFloat())
    }

    private fun buildPath(w: Float, h: Float) {
        val c = (chamferDp * density).coerceAtMost(minOf(w, h) / 2.2f)
        path.reset()
        when (cornerStyle) {
            "tl-br" -> {
                path.moveTo(c, 0f)
                path.lineTo(w, 0f)
                path.lineTo(w, h - c)
                path.lineTo(w - c, h)
                path.lineTo(0f, h)
                path.lineTo(0f, c)
                path.close()
            }
            "all" -> {
                path.moveTo(c, 0f)
                path.lineTo(w - c, 0f)
                path.lineTo(w, c)
                path.lineTo(w, h - c)
                path.lineTo(w - c, h)
                path.lineTo(c, h)
                path.lineTo(0f, h - c)
                path.lineTo(0f, c)
                path.close()
            }
            else -> { // tr-bl
                path.moveTo(0f, 0f)
                path.lineTo(w - c, 0f)
                path.lineTo(w, c)
                path.lineTo(w, h)
                path.lineTo(c, h)
                path.lineTo(0f, h - c)
                path.close()
            }
        }
    }

    override fun onDraw(canvas: Canvas) {
        fillPaint.color = fillColor
        canvas.drawPath(path, fillPaint)
        strokePaint.color = accentColor
        strokePaint.alpha = accentAlpha
        strokePaint.strokeWidth = 1.4f * density
        canvas.drawPath(path, strokePaint)
        super.onDraw(canvas)
    }
}
