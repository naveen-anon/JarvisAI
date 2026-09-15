package com.jarvis.assistant.ui

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

enum class HudState { IDLE, LISTENING, THINKING, EXECUTING, SPEAKING, DONE, ERROR }

/**
 * Dense multi-ring Arc Reactor matching reference HUD:
 * solid outer ring, dashed energy rings, glowing triangle core.
 */
class ArcReactorView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private var accentColor: Int = Color.parseColor("#00E5FF")
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
            duration = 16000L
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            addUpdateListener {
                rotationDeg = it.animatedValue as Float
                rotationRev = 360f - (it.animatedValue as Float) * 0.55f
                invalidate()
            }
            start()
        }
        pulseAnim = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 2200L
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
            HudState.IDLE -> 22000L to 2600L
            HudState.LISTENING -> 5000L to 800L
            HudState.THINKING, HudState.EXECUTING -> 3500L to 600L
            HudState.SPEAKING -> 8000L to 1000L
            HudState.DONE -> 16000L to 1800L
            HudState.ERROR -> 12000L to 500L
        }
        spinAnim?.duration = spinMs
        pulseAnim?.duration = pulseMs
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val cx = width / 2f
        val cy = height / 2f
        val r = min(cx, cy) * 0.90f

        // IDLE + LISTENING always cyan (ignore settings accent so it never goes yellow while listening)
        val base = when (state) {
            HudState.THINKING, HudState.EXECUTING -> Color.parseColor("#FFB020")
            HudState.SPEAKING -> Color.parseColor("#B0F8FF")
            HudState.ERROR -> Color.parseColor("#FF5252")
            HudState.LISTENING -> Color.parseColor("#00E5FF")
            else -> Color.parseColor("#00E5FF") // IDLE + DONE = cyan, not settings amber
        }
        val bright = when (state) {
            HudState.THINKING -> Color.parseColor("#FFE8A0")
            HudState.SPEAKING -> Color.WHITE
            else -> Color.parseColor("#E8FDFF")
        }
        val alphaMul = when (state) {
            HudState.IDLE -> 0.88f
            HudState.LISTENING, HudState.SPEAKING -> 1f
            HudState.THINKING, HudState.EXECUTING -> 1f
            else -> 0.92f
        }

        // Soft outer glow
        fillPaint.shader = RadialGradient(
            cx, cy, r * 1.08f,
            intArrayOf(
                Color.argb((55 * alphaMul).toInt(), Color.red(base), Color.green(base), Color.blue(base)),
                Color.TRANSPARENT
            ),
            floatArrayOf(0.5f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawCircle(cx, cy, r * 1.08f, fillPaint)
        fillPaint.shader = null

        // Solid outer ring (thick)
        ringPaint.pathEffect = null
        ringPaint.strokeWidth = r * 0.035f
        ringPaint.color = Color.argb((200 * alphaMul).toInt(), Color.red(base), Color.green(base), Color.blue(base))
        canvas.drawCircle(cx, cy, r * 0.96f, ringPaint)

        // Outer dashed ticks
        ringPaint.pathEffect = android.graphics.DashPathEffect(floatArrayOf(r * 0.035f, r * 0.055f), 0f)
        ringPaint.strokeWidth = r * 0.018f
        ringPaint.color = Color.argb((160 * alphaMul).toInt(), Color.red(base), Color.green(base), Color.blue(base))
        canvas.save()
        canvas.rotate(rotationDeg * 0.25f, cx, cy)
        canvas.drawCircle(cx, cy, r * 0.90f, ringPaint)
        canvas.restore()

        // Main energy ring (segmented)
        ringPaint.pathEffect = android.graphics.DashPathEffect(floatArrayOf(r * 0.11f, r * 0.045f), 0f)
        ringPaint.strokeWidth = r * 0.032f
        ringPaint.color = Color.argb((230 * alphaMul).toInt(), Color.red(base), Color.green(base), Color.blue(base))
        canvas.save()
        canvas.rotate(rotationDeg, cx, cy)
        canvas.drawCircle(cx, cy, r * 0.78f, ringPaint)
        canvas.restore()

        // Mid dense ring
        ringPaint.pathEffect = android.graphics.DashPathEffect(floatArrayOf(r * 0.06f, r * 0.04f), 0f)
        ringPaint.strokeWidth = r * 0.022f
        ringPaint.color = Color.argb((180 * alphaMul).toInt(), Color.red(base), Color.green(base), Color.blue(base))
        canvas.save()
        canvas.rotate(rotationRev, cx, cy)
        canvas.drawCircle(cx, cy, r * 0.64f, ringPaint)
        canvas.restore()

        // Inner technical ring
        ringPaint.pathEffect = android.graphics.DashPathEffect(floatArrayOf(r * 0.09f, r * 0.05f), 0f)
        ringPaint.strokeWidth = r * 0.016f
        ringPaint.color = Color.argb((140 * alphaMul).toInt(), 255, 255, 255)
        canvas.save()
        canvas.rotate(rotationDeg * 0.8f, cx, cy)
        canvas.drawCircle(cx, cy, r * 0.50f, ringPaint)
        canvas.restore()

        // Tight ring around core
        ringPaint.pathEffect = android.graphics.DashPathEffect(floatArrayOf(r * 0.04f, r * 0.035f), 0f)
        ringPaint.strokeWidth = r * 0.028f
        ringPaint.color = Color.argb((240 * alphaMul).toInt(), Color.red(base), Color.green(base), Color.blue(base))
        canvas.save()
        canvas.rotate(rotationDeg * 1.8f, cx, cy)
        canvas.drawCircle(cx, cy, r * 0.38f, ringPaint)
        canvas.restore()

        // Core glow
        val coreR = r * (0.20f + 0.03f * pulse)
        fillPaint.shader = RadialGradient(
            cx, cy, coreR * 1.9f,
            intArrayOf(
                Color.argb(255, Color.red(bright), Color.green(bright), Color.blue(bright)),
                Color.argb(210, Color.red(base), Color.green(base), Color.blue(base)),
                Color.argb(0, Color.red(base), Color.green(base), Color.blue(base))
            ),
            floatArrayOf(0f, 0.4f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawCircle(cx, cy, coreR * 1.7f, fillPaint)
        fillPaint.shader = null

        // Core solid
        fillPaint.color = bright
        canvas.drawCircle(cx, cy, coreR * 0.5f, fillPaint)
        fillPaint.color = Color.WHITE
        canvas.drawCircle(cx, cy, coreR * 0.2f, fillPaint)

        // Triangle
        val triR = coreR * 1.05f
        val path = Path()
        for (i in 0..2) {
            val a = Math.toRadians((-90 + i * 120).toDouble())
            val x = cx + (triR * cos(a)).toFloat()
            val y = cy + (triR * sin(a)).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        ringPaint.pathEffect = null
        ringPaint.strokeWidth = r * 0.022f
        ringPaint.color = Color.argb((250 * alphaMul).toInt(), Color.red(base), Color.green(base), Color.blue(base))
        canvas.drawPath(path, ringPaint)

        // Inner triangle (thinner)
        val path2 = Path()
        val triR2 = triR * 0.55f
        for (i in 0..2) {
            val a = Math.toRadians((-90 + i * 120).toDouble())
            val x = cx + (triR2 * cos(a)).toFloat()
            val y = cy + (triR2 * sin(a)).toFloat()
            if (i == 0) path2.moveTo(x, y) else path2.lineTo(x, y)
        }
        path2.close()
        ringPaint.strokeWidth = r * 0.012f
        ringPaint.color = Color.argb((180 * alphaMul).toInt(), 255, 255, 255)
        canvas.drawPath(path2, ringPaint)
    }

    override fun onDetachedFromWindow() {
        spinAnim?.cancel()
        pulseAnim?.cancel()
        super.onDetachedFromWindow()
    }
}
