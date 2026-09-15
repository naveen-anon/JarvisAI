package com.jarvis.assistant.ui

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.BlurMaskFilter
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
 * Dense navy-cyan liquid-glass Arc Reactor (reference style).
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
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        maskFilter = BlurMaskFilter(14f, BlurMaskFilter.Blur.NORMAL)
    }

    var state: HudState = HudState.IDLE
        set(value) {
            field = value
            applyStateSpeeds()
            invalidate()
        }

    init {
        setLayerType(LAYER_TYPE_SOFTWARE, null)
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
            duration = 18000L
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            addUpdateListener {
                rotationDeg = it.animatedValue as Float
                rotationRev = 360f - (it.animatedValue as Float) * 0.5f
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
            HudState.IDLE -> 20000L to 2600L
            HudState.LISTENING -> 4500L to 700L
            HudState.THINKING, HudState.EXECUTING -> 3200L to 550L
            HudState.SPEAKING -> 7000L to 900L
            HudState.DONE -> 15000L to 1800L
            HudState.ERROR -> 10000L to 450L
        }
        spinAnim?.duration = spinMs
        pulseAnim?.duration = pulseMs
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val cx = width / 2f
        val cy = height / 2f
        val r = min(cx, cy) * 0.88f

        val cyan = Color.parseColor("#00E5FF")
        val cyanDeep = Color.parseColor("#0090B8")
        val base = when (state) {
            HudState.THINKING, HudState.EXECUTING -> Color.parseColor("#FFB020")
            HudState.SPEAKING -> Color.parseColor("#B8F4FF")
            HudState.ERROR -> Color.parseColor("#FF5252")
            else -> cyan
        }
        val bright = when (state) {
            HudState.THINKING -> Color.parseColor("#FFE8A0")
            HudState.SPEAKING -> Color.WHITE
            else -> Color.parseColor("#E8FDFF")
        }
        val a = if (state == HudState.IDLE) 0.92f else 1f

        // Outer liquid glow
        fillPaint.shader = RadialGradient(
            cx, cy, r * 1.15f,
            intArrayOf(
                Color.argb((70 * a).toInt(), Color.red(base), Color.green(base), Color.blue(base)),
                Color.argb((25 * a).toInt(), Color.red(base), Color.green(base), Color.blue(base)),
                Color.TRANSPARENT
            ),
            floatArrayOf(0.35f, 0.7f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawCircle(cx, cy, r * 1.15f, fillPaint)
        fillPaint.shader = null

        glowPaint.color = Color.argb((90 * a).toInt(), Color.red(base), Color.green(base), Color.blue(base))
        glowPaint.strokeWidth = r * 0.06f
        canvas.drawCircle(cx, cy, r * 0.98f, glowPaint)

        // Solid thick outer rim
        ringPaint.pathEffect = null
        ringPaint.strokeWidth = r * 0.045f
        ringPaint.color = Color.argb((230 * a).toInt(), Color.red(base), Color.green(base), Color.blue(base))
        canvas.drawCircle(cx, cy, r * 0.96f, ringPaint)

        ringPaint.strokeWidth = r * 0.012f
        ringPaint.color = Color.argb((100 * a).toInt(), 255, 255, 255)
        canvas.drawCircle(cx, cy, r * 0.935f, ringPaint)

        // Long dashes
        ringPaint.pathEffect = android.graphics.DashPathEffect(floatArrayOf(r * 0.12f, r * 0.04f), 0f)
        ringPaint.strokeWidth = r * 0.028f
        ringPaint.color = Color.argb((200 * a).toInt(), Color.red(base), Color.green(base), Color.blue(base))
        canvas.save()
        canvas.rotate(rotationDeg * 0.35f, cx, cy)
        canvas.drawCircle(cx, cy, r * 0.82f, ringPaint)
        canvas.restore()

        // Medium segments
        ringPaint.pathEffect = android.graphics.DashPathEffect(floatArrayOf(r * 0.08f, r * 0.035f), 0f)
        ringPaint.strokeWidth = r * 0.022f
        ringPaint.color = Color.argb((170 * a).toInt(), Color.red(base), Color.green(base), Color.blue(base))
        canvas.save()
        canvas.rotate(rotationDeg, cx, cy)
        canvas.drawCircle(cx, cy, r * 0.70f, ringPaint)
        canvas.restore()

        // Fine ticks counter
        ringPaint.pathEffect = android.graphics.DashPathEffect(floatArrayOf(r * 0.03f, r * 0.045f), 0f)
        ringPaint.strokeWidth = r * 0.016f
        ringPaint.color = Color.argb((140 * a).toInt(), Color.red(cyanDeep), Color.green(cyanDeep), Color.blue(cyanDeep))
        canvas.save()
        canvas.rotate(rotationRev, cx, cy)
        canvas.drawCircle(cx, cy, r * 0.58f, ringPaint)
        canvas.restore()

        // Dense near core
        ringPaint.pathEffect = android.graphics.DashPathEffect(floatArrayOf(r * 0.05f, r * 0.025f), 0f)
        ringPaint.strokeWidth = r * 0.024f
        ringPaint.color = Color.argb((220 * a).toInt(), Color.red(base), Color.green(base), Color.blue(base))
        canvas.save()
        canvas.rotate(rotationDeg * 1.6f, cx, cy)
        canvas.drawCircle(cx, cy, r * 0.46f, ringPaint)
        canvas.restore()

        // Tight solid
        ringPaint.pathEffect = null
        ringPaint.strokeWidth = r * 0.014f
        ringPaint.color = Color.argb((180 * a).toInt(), 255, 255, 255)
        canvas.drawCircle(cx, cy, r * 0.36f, ringPaint)

        // Core glow pulse
        val coreR = r * (0.22f + 0.035f * pulse)
        fillPaint.shader = RadialGradient(
            cx, cy, coreR * 2.0f,
            intArrayOf(
                Color.argb(255, Color.red(bright), Color.green(bright), Color.blue(bright)),
                Color.argb(220, Color.red(base), Color.green(base), Color.blue(base)),
                Color.argb(60, Color.red(base), Color.green(base), Color.blue(base)),
                Color.TRANSPARENT
            ),
            floatArrayOf(0f, 0.3f, 0.65f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawCircle(cx, cy, coreR * 1.9f, fillPaint)
        fillPaint.shader = null

        fillPaint.color = bright
        canvas.drawCircle(cx, cy, coreR * 0.48f, fillPaint)
        fillPaint.color = Color.WHITE
        canvas.drawCircle(cx, cy, coreR * 0.2f, fillPaint)

        // Triangle
        val triR = coreR * 1.15f
        val path = Path()
        for (i in 0..2) {
            val ang = Math.toRadians((-90 + i * 120).toDouble())
            val x = cx + (triR * cos(ang)).toFloat()
            val y = cy + (triR * sin(ang)).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        ringPaint.pathEffect = null
        ringPaint.strokeWidth = r * 0.028f
        ringPaint.color = Color.argb((250 * a).toInt(), Color.red(base), Color.green(base), Color.blue(base))
        canvas.drawPath(path, ringPaint)

        val path2 = Path()
        val triR2 = triR * 0.5f
        for (i in 0..2) {
            val ang = Math.toRadians((-90 + i * 120).toDouble())
            val x = cx + (triR2 * cos(ang)).toFloat()
            val y = cy + (triR2 * sin(ang)).toFloat()
            if (i == 0) path2.moveTo(x, y) else path2.lineTo(x, y)
        }
        path2.close()
        ringPaint.strokeWidth = r * 0.01f
        ringPaint.color = Color.argb((160 * a).toInt(), 255, 255, 255)
        canvas.drawPath(path2, ringPaint)
    }

    override fun onDetachedFromWindow() {
        spinAnim?.cancel()
        pulseAnim?.cancel()
        super.onDetachedFromWindow()
    }
}
