package com.jarvis.assistant.ui

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.SweepGradient
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

enum class HudState { IDLE, LISTENING, THINKING, EXECUTING, SPEAKING, DONE, ERROR }

/**
 * Canvas-drawn Arc Reactor matching the target HUD:
 * multi concentric rings, rotating dashed segments, glowing triangle core.
 */
class ArcReactorView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private var accentColor: Int = Color.parseColor("#00E5FF")
    private var coreColor: Int = Color.parseColor("#E0FBFF")
    private var dimColor: Int = Color.parseColor("#0A7A96")

    private var rotationDeg = 0f
    private var rotationRev = 0f
    private var pulse = 0f

    private var spinAnim: ValueAnimator? = null
    private var pulseAnim: ValueAnimator? = null

    private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    var state: HudState = HudState.IDLE
        set(value) {
            field = value
            applyStateSpeeds()
            invalidate()
        }

    init {
        setBackgroundColor(Color.TRANSPARENT)
        startAnimations()
        applyStateSpeeds()
    }

    fun setAccentColor(hex: String) {
        accentColor = try {
            Color.parseColor(if (hex.startsWith("#")) hex else "#$hex")
        } catch (_: Exception) {
            Color.parseColor("#00E5FF")
        }
        invalidate()
    }

    private fun startAnimations() {
        spinAnim = ValueAnimator.ofFloat(0f, 360f).apply {
            duration = 12000L
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            addUpdateListener {
                rotationDeg = it.animatedValue as Float
                rotationRev = 360f - (it.animatedValue as Float) * 0.65f
                invalidate()
            }
            start()
        }
        pulseAnim = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 2400L
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            addUpdateListener {
                pulse = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    private fun applyStateSpeeds() {
        val (spinMs, pulseMs) = when (state) {
            HudState.IDLE -> 28000L to 2800L
            HudState.LISTENING -> 6000L to 900L
            HudState.THINKING, HudState.EXECUTING -> 4000L to 700L
            HudState.SPEAKING -> 9000L to 1100L
            HudState.DONE -> 18000L to 2000L
            HudState.ERROR -> 14000L to 600L
        }
        spinAnim?.duration = spinMs
        pulseAnim?.duration = pulseMs
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val cx = width / 2f
        val cy = height / 2f
        val r = min(cx, cy) * 0.92f

        val base = when (state) {
            HudState.THINKING, HudState.EXECUTING -> Color.parseColor("#FFB020")
            HudState.SPEAKING -> Color.parseColor("#E0FBFF")
            HudState.ERROR -> Color.parseColor("#FF5252")
            else -> accentColor
        }
        val bright = when (state) {
            HudState.THINKING -> Color.parseColor("#FFE0A0")
            HudState.SPEAKING -> Color.WHITE
            else -> coreColor
        }

        val alphaMul = when (state) {
            HudState.IDLE -> 0.75f
            HudState.LISTENING, HudState.SPEAKING -> 1f
            HudState.THINKING, HudState.EXECUTING -> 1f
            else -> 0.9f
        }

        // Outer soft glow disc
        fillPaint.shader = RadialGradient(
            cx, cy, r * 1.05f,
            intArrayOf(Color.argb((40 * alphaMul).toInt(), Color.red(base), Color.green(base), Color.blue(base)), Color.TRANSPARENT),
            floatArrayOf(0.55f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawCircle(cx, cy, r * 1.05f, fillPaint)
        fillPaint.shader = null

        // Outer teeth ring (slow)
        ringPaint.color = Color.argb((90 * alphaMul).toInt(), 255, 255, 255)
        ringPaint.strokeWidth = r * 0.012f
        ringPaint.pathEffect = android.graphics.DashPathEffect(floatArrayOf(r * 0.04f, r * 0.12f), 0f)
        canvas.save()
        canvas.rotate(rotationDeg * 0.3f, cx, cy)
        canvas.drawCircle(cx, cy, r * 0.98f, ringPaint)
        canvas.restore()

        // Main thick cyan ring
        ringPaint.pathEffect = android.graphics.DashPathEffect(floatArrayOf(r * 0.14f, r * 0.07f), 0f)
        ringPaint.strokeWidth = r * 0.028f
        ringPaint.color = Color.argb((220 * alphaMul).toInt(), Color.red(base), Color.green(base), Color.blue(base))
        canvas.save()
        canvas.rotate(rotationDeg, cx, cy)
        canvas.drawCircle(cx, cy, r * 0.86f, ringPaint)
        canvas.restore()

        // Energy blades (segmented, faster)
        ringPaint.pathEffect = android.graphics.DashPathEffect(floatArrayOf(r * 0.10f, r * 0.22f), 0f)
        ringPaint.strokeWidth = r * 0.038f
        ringPaint.color = Color.argb((200 * alphaMul).toInt(), Color.red(base), Color.green(base), Color.blue(base))
        canvas.save()
        canvas.rotate(rotationDeg * 1.4f, cx, cy)
        canvas.drawCircle(cx, cy, r * 0.72f, ringPaint)
        canvas.restore()

        // Counter-rotating dashed
        ringPaint.pathEffect = android.graphics.DashPathEffect(floatArrayOf(r * 0.04f, r * 0.06f), 0f)
        ringPaint.strokeWidth = r * 0.018f
        ringPaint.color = Color.argb((160 * alphaMul).toInt(), Color.red(base), Color.green(base), Color.blue(base))
        canvas.save()
        canvas.rotate(rotationRev, cx, cy)
        canvas.drawCircle(cx, cy, r * 0.58f, ringPaint)
        canvas.restore()

        // Inner technical ring
        ringPaint.pathEffect = android.graphics.DashPathEffect(floatArrayOf(r * 0.12f, r * 0.08f), 0f)
        ringPaint.strokeWidth = r * 0.012f
        ringPaint.color = Color.argb((100 * alphaMul).toInt(), 255, 255, 255)
        canvas.save()
        canvas.rotate(rotationDeg * 0.7f, cx, cy)
        canvas.drawCircle(cx, cy, r * 0.45f, ringPaint)
        canvas.restore()

        // Tightest ring
        ringPaint.pathEffect = android.graphics.DashPathEffect(floatArrayOf(r * 0.05f, r * 0.04f), 0f)
        ringPaint.strokeWidth = r * 0.022f
        ringPaint.color = Color.argb((230 * alphaMul).toInt(), Color.red(base), Color.green(base), Color.blue(base))
        canvas.save()
        canvas.rotate(rotationDeg * 2.2f, cx, cy)
        canvas.drawCircle(cx, cy, r * 0.34f, ringPaint)
        canvas.restore()

        // Core glow (breathing)
        val coreR = r * (0.18f + 0.025f * pulse)
        fillPaint.shader = RadialGradient(
            cx, cy, coreR * 1.8f,
            intArrayOf(
                Color.argb(255, Color.red(bright), Color.green(bright), Color.blue(bright)),
                Color.argb(200, Color.red(base), Color.green(base), Color.blue(base)),
                Color.argb(0, Color.red(base), Color.green(base), Color.blue(base))
            ),
            floatArrayOf(0f, 0.45f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawCircle(cx, cy, coreR * 1.6f, fillPaint)
        fillPaint.shader = null

        // Solid core
        fillPaint.color = bright
        canvas.drawCircle(cx, cy, coreR * 0.55f, fillPaint)
        fillPaint.color = Color.WHITE
        canvas.drawCircle(cx, cy, coreR * 0.22f, fillPaint)

        // Triangle (Iron Man style) in center
        val triR = coreR * 0.95f
        val path = Path()
        for (i in 0..2) {
            val a = Math.toRadians((-90 + i * 120).toDouble())
            val x = cx + (triR * cos(a)).toFloat()
            val y = cy + (triR * sin(a)).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        ringPaint.pathEffect = null
        ringPaint.style = Paint.Style.STROKE
        ringPaint.strokeWidth = r * 0.018f
        ringPaint.color = Color.argb((230 * alphaMul).toInt(), Color.red(base), Color.green(base), Color.blue(base))
        canvas.drawPath(path, ringPaint)

        // Small ticks around core
        ringPaint.strokeWidth = r * 0.008f
        ringPaint.color = Color.argb((140 * alphaMul).toInt(), Color.red(base), Color.green(base), Color.blue(base))
        for (i in 0 until 12) {
            val a = Math.toRadians((i * 30 + rotationDeg * 0.5).toDouble())
            val x1 = cx + (coreR * 1.35f * cos(a)).toFloat()
            val y1 = cy + (coreR * 1.35f * sin(a)).toFloat()
            val x2 = cx + (coreR * 1.55f * cos(a)).toFloat()
            val y2 = cy + (coreR * 1.55f * sin(a)).toFloat()
            canvas.drawLine(x1, y1, x2, y2, ringPaint)
        }
    }

    override fun onDetachedFromWindow() {
        spinAnim?.cancel()
        pulseAnim?.cancel()
        super.onDetachedFromWindow()
    }
}
