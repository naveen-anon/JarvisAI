package com.jarvis.assistant.ui

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
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
 * Iron Man–style Arc Reactor for Jarvis:
 * metal outer housing, segmented rotating rings, triangular core,
 * white-hot center, and a soft chest-glow bloom.
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
    private val cyanBright = Color.parseColor("#B8F6FF")
    private val cyanDim = Color.parseColor("#0A6E85")
    private val amber = Color.parseColor("#FFB300")
    private val metal = Color.parseColor("#1A3A4A")
    private val metalLight = Color.parseColor("#2A5A6A")

    private var accentColor = cyan
    private var colorAnimator: ValueAnimator? = null

    private var outerRotation = 0f
    private var midRotation = 0f
    private var innerRotation = 0f
    private var pulseScale = 1f
    private var pulseAlpha = 1f

    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val glow = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }

    private val outerAnimator = ValueAnimator.ofFloat(0f, 360f).apply {
        duration = 10000
        repeatCount = ValueAnimator.INFINITE
        interpolator = LinearInterpolator()
        addUpdateListener { outerRotation = it.animatedValue as Float; invalidate() }
    }
    private val midAnimator = ValueAnimator.ofFloat(360f, 0f).apply {
        duration = 7000
        repeatCount = ValueAnimator.INFINITE
        interpolator = LinearInterpolator()
        addUpdateListener { midRotation = it.animatedValue as Float; invalidate() }
    }
    private val innerAnimator = ValueAnimator.ofFloat(0f, 360f).apply {
        duration = 4000
        repeatCount = ValueAnimator.INFINITE
        interpolator = LinearInterpolator()
        addUpdateListener { innerRotation = it.animatedValue as Float; invalidate() }
    }
    private val pulseAnimator = ValueAnimator.ofFloat(0.92f, 1.08f).apply {
        duration = 1400
        repeatCount = ValueAnimator.INFINITE
        repeatMode = ValueAnimator.REVERSE
        addUpdateListener {
            pulseScale = it.animatedValue as Float
            pulseAlpha = 0.55f + (pulseScale - 0.92f) / 0.16f * 0.45f
            invalidate()
        }
    }

    init {
        outerAnimator.start()
        midAnimator.start()
        innerAnimator.start()
        pulseAnimator.start()
        setLayerType(LAYER_TYPE_HARDWARE, null)
    }

    private fun applyStateParams() {
        val target = when (state) {
            HudState.IDLE -> cyanDim
            HudState.LISTENING -> cyan
            HudState.THINKING -> amber
            HudState.SPEAKING -> cyanBright
        }
        animateAccentColor(target)

        when (state) {
            HudState.IDLE -> {
                outerAnimator.duration = 14000
                midAnimator.duration = 9000
                innerAnimator.duration = 6000
                pulseAnimator.duration = 1800
            }
            HudState.LISTENING -> {
                outerAnimator.duration = 3500
                midAnimator.duration = 2500
                innerAnimator.duration = 1800
                pulseAnimator.duration = 600
            }
            HudState.THINKING -> {
                outerAnimator.duration = 1400
                midAnimator.duration = 1000
                innerAnimator.duration = 700
                pulseAnimator.duration = 320
            }
            HudState.SPEAKING -> {
                outerAnimator.duration = 2200
                midAnimator.duration = 1600
                innerAnimator.duration = 1100
                pulseAnimator.duration = 750
            }
        }
    }

    private fun animateAccentColor(target: Int) {
        colorAnimator?.cancel()
        colorAnimator = ValueAnimator.ofObject(ArgbEvaluator(), accentColor, target).apply {
            duration = 320
            addUpdateListener {
                accentColor = it.animatedValue as Int
                invalidate()
            }
            start()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val cx = width / 2f
        val cy = height / 2f
        val R = min(width, height) / 2f * 0.90f

        drawBloom(canvas, cx, cy, R)
        drawOuterHousing(canvas, cx, cy, R)
        drawSegmentedRing(canvas, cx, cy, R * 0.82f, outerRotation, 12, 5.5f, 8f)
        drawSegmentedRing(canvas, cx, cy, R * 0.68f, midRotation, 8, 4f, 14f)
        drawInnerTicks(canvas, cx, cy, R * 0.58f)
        drawTriangleCore(canvas, cx, cy, R * 0.48f)
        drawEnergyCore(canvas, cx, cy, R * 0.22f)
    }

    /** Soft chest-glow behind the reactor. */
    private fun drawBloom(canvas: Canvas, cx: Float, cy: Float, R: Float) {
        val r = R * 1.45f
        glow.shader = RadialGradient(
            cx, cy, r,
            intArrayOf(
                Color.argb(
                    (pulseAlpha * 36).toInt(),
                    Color.red(accentColor),
                    Color.green(accentColor),
                    Color.blue(accentColor)
                ),
                Color.argb(
                    (pulseAlpha * 12).toInt(),
                    Color.red(accentColor),
                    Color.green(accentColor),
                    Color.blue(accentColor)
                ),
                Color.TRANSPARENT
            ),
            floatArrayOf(0f, 0.45f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawCircle(cx, cy, r, glow)
        glow.shader = null
    }

    /** Thick outer metal ring + thin accent rim (housing). */
    private fun drawOuterHousing(canvas: Canvas, cx: Float, cy: Float, R: Float) {
        stroke.style = Paint.Style.STROKE
        stroke.strokeCap = Paint.Cap.ROUND

        stroke.color = metal
        stroke.strokeWidth = R * 0.085f
        stroke.alpha = 255
        canvas.drawCircle(cx, cy, R * 0.94f, stroke)

        stroke.color = metalLight
        stroke.strokeWidth = R * 0.018f
        stroke.alpha = 200
        canvas.drawCircle(cx, cy, R * 0.98f, stroke)
        canvas.drawCircle(cx, cy, R * 0.90f, stroke)

        stroke.color = accentColor
        stroke.strokeWidth = R * 0.012f
        stroke.alpha = 160
        canvas.drawCircle(cx, cy, R * 0.88f, stroke)
    }

    private fun drawSegmentedRing(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        radius: Float,
        rotation: Float,
        segments: Int,
        strokeWidth: Float,
        gapDeg: Float
    ) {
        stroke.color = accentColor
        stroke.strokeWidth = strokeWidth
        stroke.alpha = 210
        stroke.strokeCap = Paint.Cap.BUTT
        val sweep = 360f / segments - gapDeg
        val rect = RectF(cx - radius, cy - radius, cx + radius, cy + radius)
        for (i in 0 until segments) {
            val start = rotation + i * (360f / segments)
            canvas.drawArc(rect, start, sweep, false, stroke)
        }
    }

    private fun drawInnerTicks(canvas: Canvas, cx: Float, cy: Float, radius: Float) {
        stroke.color = accentColor
        stroke.strokeWidth = 2f
        stroke.alpha = 90
        stroke.strokeCap = Paint.Cap.ROUND
        canvas.save()
        canvas.rotate(innerRotation * 0.25f, cx, cy)
        val count = 36
        for (i in 0 until count) {
            val a = Math.toRadians((360.0 / count) * i)
            val len = if (i % 3 == 0) radius * 0.10f else radius * 0.05f
            val x1 = cx + (radius * cos(a)).toFloat()
            val y1 = cy + (radius * sin(a)).toFloat()
            val x2 = cx + ((radius - len) * cos(a)).toFloat()
            val y2 = cy + ((radius - len) * sin(a)).toFloat()
            canvas.drawLine(x1, y1, x2, y2, stroke)
        }
        canvas.restore()
    }

    /**
     * Classic Arc Reactor triangle: three curved arms + triangular frame —
     * the signature Iron Man look.
     */
    private fun drawTriangleCore(canvas: Canvas, cx: Float, cy: Float, radius: Float) {
        canvas.save()
        canvas.rotate(innerRotation * 0.15f, cx, cy)

        stroke.color = accentColor
        stroke.strokeWidth = 3.5f
        stroke.alpha = 200
        stroke.strokeJoin = Paint.Join.ROUND
        val tri = Path()
        for (i in 0 until 3) {
            val a = Math.toRadians(-90.0 + i * 120.0)
            val x = cx + (radius * cos(a)).toFloat()
            val y = cy + (radius * sin(a)).toFloat()
            if (i == 0) tri.moveTo(x, y) else tri.lineTo(x, y)
        }
        tri.close()
        canvas.drawPath(tri, stroke)

        fill.color = accentColor
        for (i in 0 until 3) {
            canvas.save()
            canvas.rotate(i * 120f - 90f, cx, cy)
            val blade = Path().apply {
                moveTo(cx, cy)
                quadTo(cx + radius * 0.28f, cy - radius * 0.12f, cx + radius * 0.72f, cy)
                quadTo(cx + radius * 0.28f, cy + radius * 0.12f, cx, cy)
                close()
            }
            fill.alpha = 70
            canvas.drawPath(blade, fill)
            stroke.strokeWidth = 1.8f
            stroke.alpha = 160
            canvas.drawPath(blade, stroke)
            canvas.restore()
        }

        fill.color = accentColor
        fill.alpha = 220
        for (i in 0 until 3) {
            val a = Math.toRadians(-90.0 + i * 120.0)
            val x = cx + (radius * cos(a)).toFloat()
            val y = cy + (radius * sin(a)).toFloat()
            canvas.drawCircle(x, y, radius * 0.06f, fill)
        }

        canvas.restore()
    }

    /** White-hot center core with pulsing cyan glow. */
    private fun drawEnergyCore(canvas: Canvas, cx: Float, cy: Float, baseR: Float) {
        val r = baseR * pulseScale

        glow.shader = RadialGradient(
            cx, cy, r * 2.8f,
            intArrayOf(
                Color.argb(
                    (pulseAlpha * 110).toInt(),
                    Color.red(accentColor),
                    Color.green(accentColor),
                    Color.blue(accentColor)
                ),
                Color.argb(
                    (pulseAlpha * 40).toInt(),
                    Color.red(accentColor),
                    Color.green(accentColor),
                    Color.blue(accentColor)
                ),
                Color.TRANSPARENT
            ),
            floatArrayOf(0f, 0.4f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawCircle(cx, cy, r * 2.8f, glow)
        glow.shader = null

        fill.shader = RadialGradient(
            cx, cy, r,
            intArrayOf(Color.WHITE, cyanBright, accentColor),
            floatArrayOf(0f, 0.35f, 1f),
            Shader.TileMode.CLAMP
        )
        fill.alpha = 255
        canvas.drawCircle(cx, cy, r, fill)
        fill.shader = null

        stroke.color = Color.WHITE
        stroke.strokeWidth = 2f
        stroke.alpha = 180
        canvas.drawCircle(cx, cy, r, stroke)

        fill.color = Color.WHITE
        fill.alpha = 255
        canvas.drawCircle(cx, cy, r * 0.28f, fill)
    }

    override fun onDetachedFromWindow() {
        outerAnimator.cancel()
        midAnimator.cancel()
        innerAnimator.cancel()
        pulseAnimator.cancel()
        colorAnimator?.cancel()
        super.onDetachedFromWindow()
    }
}
