package com.jarvis.assistant.ui

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

enum class HudState { IDLE, LISTENING, THINKING, SPEAKING }

/**
 * Iron Man Arc Reactor — pure round (no triangle).
 * Concentric metal rings, rotating segments, white-hot core, cyan glow.
 */
class ArcReactorView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    var state: HudState = HudState.IDLE
        set(value) {
            field = value
            applyStateParams()
        }

    private val cyan = Color.parseColor("#00E5FF")
    private val cyanHi = Color.parseColor("#E0FBFF")
    private val cyanDim = Color.parseColor("#0B7A94")
    private val amber = Color.parseColor("#FFB020")
    private val darkMetal = Color.parseColor("#0A1520")
    private val midMetal = Color.parseColor("#1A3A4A")

    private var accent = cyan
    private var colorAnim: ValueAnimator? = null

    private var rotOuter = 0f
    private var rotMid = 0f
    private var rotInner = 0f
    private var pulse = 1f
    private var glowA = 0.7f

    private val pStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val pFill = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val pGlow = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }

    private val aOuter = ValueAnimator.ofFloat(0f, 360f).apply {
        duration = 12000; repeatCount = ValueAnimator.INFINITE
        interpolator = LinearInterpolator()
        addUpdateListener { rotOuter = it.animatedValue as Float; invalidate() }
    }
    private val aMid = ValueAnimator.ofFloat(360f, 0f).apply {
        duration = 8000; repeatCount = ValueAnimator.INFINITE
        interpolator = LinearInterpolator()
        addUpdateListener { rotMid = it.animatedValue as Float; invalidate() }
    }
    private val aInner = ValueAnimator.ofFloat(0f, 360f).apply {
        duration = 5000; repeatCount = ValueAnimator.INFINITE
        interpolator = LinearInterpolator()
        addUpdateListener { rotInner = it.animatedValue as Float; invalidate() }
    }
    private val aPulse = ValueAnimator.ofFloat(0.90f, 1.10f).apply {
        duration = 1300; repeatCount = ValueAnimator.INFINITE; repeatMode = ValueAnimator.REVERSE
        addUpdateListener {
            pulse = it.animatedValue as Float
            glowA = 0.55f + (pulse - 0.90f) / 0.20f * 0.45f
            invalidate()
        }
    }

    init {
        aOuter.start(); aMid.start(); aInner.start(); aPulse.start()
        setLayerType(LAYER_TYPE_HARDWARE, null)
    }

    private fun applyStateParams() {
        val target = when (state) {
            HudState.IDLE -> cyanDim
            HudState.LISTENING -> cyan
            HudState.THINKING -> amber
            HudState.SPEAKING -> cyanHi
        }
        colorAnim?.cancel()
        colorAnim = ValueAnimator.ofObject(ArgbEvaluator(), accent, target).apply {
            duration = 280
            addUpdateListener { accent = it.animatedValue as Int; invalidate() }
            start()
        }
        when (state) {
            HudState.IDLE -> {
                aOuter.duration = 14000; aMid.duration = 9000; aInner.duration = 6000; aPulse.duration = 1600
            }
            HudState.LISTENING -> {
                aOuter.duration = 3000; aMid.duration = 2200; aInner.duration = 1600; aPulse.duration = 500
            }
            HudState.THINKING -> {
                aOuter.duration = 1100; aMid.duration = 800; aInner.duration = 600; aPulse.duration = 300
            }
            HudState.SPEAKING -> {
                aOuter.duration = 2000; aMid.duration = 1400; aInner.duration = 1000; aPulse.duration = 700
            }
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val cx = width / 2f
        val cy = height / 2f
        val R = min(width, height) / 2f * 0.88f

        drawBloom(canvas, cx, cy, R * 1.55f)

        // Thick metal housing
        pStroke.style = Paint.Style.STROKE
        pStroke.strokeCap = Paint.Cap.ROUND
        pStroke.color = darkMetal
        pStroke.strokeWidth = R * 0.11f
        pStroke.alpha = 255
        canvas.drawCircle(cx, cy, R * 0.94f, pStroke)

        pStroke.color = midMetal
        pStroke.strokeWidth = R * 0.022f
        canvas.drawCircle(cx, cy, R * 0.99f, pStroke)
        canvas.drawCircle(cx, cy, R * 0.88f, pStroke)

        // Outer accent rim
        pStroke.color = accent
        pStroke.strokeWidth = R * 0.010f
        pStroke.alpha = 140
        canvas.drawCircle(cx, cy, R * 0.86f, pStroke)

        // Outer segmented ring
        drawSegments(canvas, cx, cy, R * 0.80f, rotOuter, 18, R * 0.032f, 5f)

        // Solid mid ring
        pStroke.color = accent
        pStroke.strokeWidth = R * 0.014f
        pStroke.alpha = 200
        canvas.drawCircle(cx, cy, R * 0.68f, pStroke)

        // Mid segmented ring (opposite)
        drawSegments(canvas, cx, cy, R * 0.58f, rotMid, 12, R * 0.026f, 8f)

        drawTicks(canvas, cx, cy, R * 0.50f)

        // Inner solid ring
        pStroke.color = accent
        pStroke.strokeWidth = R * 0.012f
        pStroke.alpha = 220
        canvas.drawCircle(cx, cy, R * 0.40f, pStroke)

        // Inner dashed ring
        drawSegments(canvas, cx, cy, R * 0.32f, rotInner, 8, R * 0.020f, 12f)

        drawOrbitNodes(canvas, cx, cy, R * 0.58f, rotMid)

        drawCore(canvas, cx, cy, R * 0.20f)
    }

    private fun drawBloom(canvas: Canvas, cx: Float, cy: Float, r: Float) {
        pGlow.shader = RadialGradient(
            cx, cy, r,
            intArrayOf(
                Color.argb((glowA * 55).toInt(), Color.red(accent), Color.green(accent), Color.blue(accent)),
                Color.argb((glowA * 20).toInt(), Color.red(accent), Color.green(accent), Color.blue(accent)),
                Color.TRANSPARENT
            ),
            floatArrayOf(0f, 0.45f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawCircle(cx, cy, r, pGlow)
        pGlow.shader = null
    }

    private fun drawSegments(
        canvas: Canvas, cx: Float, cy: Float, radius: Float,
        rot: Float, n: Int, sw: Float, gap: Float
    ) {
        pStroke.color = accent
        pStroke.strokeWidth = sw
        pStroke.alpha = 235
        pStroke.strokeCap = Paint.Cap.BUTT
        val sweep = 360f / n - gap
        val rect = RectF(cx - radius, cy - radius, cx + radius, cy + radius)
        for (i in 0 until n) {
            canvas.drawArc(rect, rot + i * (360f / n), sweep, false, pStroke)
        }
    }

    private fun drawTicks(canvas: Canvas, cx: Float, cy: Float, radius: Float) {
        pStroke.color = accent
        pStroke.strokeWidth = 2f
        pStroke.alpha = 95
        pStroke.strokeCap = Paint.Cap.ROUND
        canvas.save()
        canvas.rotate(rotInner * 0.2f, cx, cy)
        for (i in 0 until 60) {
            val a = Math.toRadians(i * 6.0)
            val len = if (i % 5 == 0) radius * 0.10f else radius * 0.045f
            canvas.drawLine(
                cx + (radius * cos(a)).toFloat(),
                cy + (radius * sin(a)).toFloat(),
                cx + ((radius - len) * cos(a)).toFloat(),
                cy + ((radius - len) * sin(a)).toFloat(),
                pStroke
            )
        }
        canvas.restore()
    }

    private fun drawOrbitNodes(canvas: Canvas, cx: Float, cy: Float, radius: Float, rot: Float) {
        for (i in 0 until 4) {
            val a = Math.toRadians((rot + i * 90f).toDouble())
            val x = cx + (radius * cos(a)).toFloat()
            val y = cy + (radius * sin(a)).toFloat()
            pGlow.shader = RadialGradient(
                x, y, 14f,
                intArrayOf(
                    Color.argb(200, Color.red(accent), Color.green(accent), Color.blue(accent)),
                    Color.TRANSPARENT
                ),
                null, Shader.TileMode.CLAMP
            )
            canvas.drawCircle(x, y, 14f, pGlow)
            pGlow.shader = null
            pFill.color = Color.WHITE
            pFill.alpha = 240
            canvas.drawCircle(x, y, 2.8f, pFill)
        }
    }

    private fun drawCore(canvas: Canvas, cx: Float, cy: Float, base: Float) {
        val r = base * pulse

        pGlow.shader = RadialGradient(
            cx, cy, r * 3.4f,
            intArrayOf(
                Color.argb((glowA * 160).toInt(), Color.red(accent), Color.green(accent), Color.blue(accent)),
                Color.argb((glowA * 55).toInt(), Color.red(accent), Color.green(accent), Color.blue(accent)),
                Color.TRANSPARENT
            ),
            floatArrayOf(0f, 0.35f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawCircle(cx, cy, r * 3.4f, pGlow)
        pGlow.shader = null

        pFill.shader = RadialGradient(
            cx, cy, r,
            intArrayOf(Color.WHITE, cyanHi, accent),
            floatArrayOf(0f, 0.38f, 1f),
            Shader.TileMode.CLAMP
        )
        pFill.alpha = 255
        canvas.drawCircle(cx, cy, r, pFill)
        pFill.shader = null

        pStroke.color = Color.WHITE
        pStroke.strokeWidth = 2.5f
        pStroke.alpha = 230
        canvas.drawCircle(cx, cy, r, pStroke)

        pFill.color = Color.WHITE
        pFill.alpha = 255
        canvas.drawCircle(cx, cy, r * 0.30f, pFill)
    }

    override fun onDetachedFromWindow() {
        aOuter.cancel(); aMid.cancel(); aInner.cancel(); aPulse.cancel()
        colorAnim?.cancel()
        super.onDetachedFromWindow()
    }
}
