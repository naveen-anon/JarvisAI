package com.jarvis.assistant.ui

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import androidx.core.content.ContextCompat
import com.jarvis.assistant.R

enum class HudState { IDLE, LISTENING, THINKING, SPEAKING }

class ArcReactorView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    var state: HudState = HudState.IDLE
        set(value) {
            field = value
            applyStateParams()
        }

    private val cyan = Color.parseColor("#00D4FF")
    private val cyanDim = Color.parseColor("#0A6E85")
    private val amber = Color.parseColor("#FFB300")
    private val red = Color.parseColor("#FF3B30")

    private var accentColor = cyan
    private var colorAnimator: ValueAnimator? = null
    private var outerRotation = 0f
    private var midRotation = 0f
    private var tickRotation = 0f
    private var bladeRotation = 0f
    private var pulseScale = 1f
    private var pulseAlpha = 1f

    private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val tickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }
    private val corePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val coreGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val bloomPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val bladePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val spokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val particlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }

    private val outerAnimator = ValueAnimator.ofFloat(0f, 360f).apply {
        duration = 8000
        repeatCount = ValueAnimator.INFINITE
        interpolator = LinearInterpolator()
        addUpdateListener { outerRotation = it.animatedValue as Float; invalidate() }
    }
    private val midAnimator = ValueAnimator.ofFloat(360f, 0f).apply {
        duration = 5000
        repeatCount = ValueAnimator.INFINITE
        interpolator = LinearInterpolator()
        addUpdateListener { midRotation = it.animatedValue as Float; invalidate() }
    }
    private val tickAnimator = ValueAnimator.ofFloat(0f, 360f).apply {
        duration = 20000
        repeatCount = ValueAnimator.INFINITE
        interpolator = LinearInterpolator()
        addUpdateListener { tickRotation = it.animatedValue as Float; invalidate() }
    }
    private val pulseAnimator = ValueAnimator.ofFloat(0.85f, 1.15f).apply {
        duration = 1200
        repeatCount = ValueAnimator.INFINITE
        repeatMode = ValueAnimator.REVERSE
        addUpdateListener {
            pulseScale = it.animatedValue as Float
            pulseAlpha = 0.6f + (pulseScale - 0.85f) / 0.30f * 0.4f
            invalidate()
        }
    }
    private val bladeAnimator = ValueAnimator.ofFloat(0f, 360f).apply {
        duration = 6000
        repeatCount = ValueAnimator.INFINITE
        interpolator = LinearInterpolator()
        addUpdateListener { bladeRotation = it.animatedValue as Float; invalidate() }
    }

    init {
        outerAnimator.start()
        midAnimator.start()
        tickAnimator.start()
        pulseAnimator.start()
        bladeAnimator.start()
    }

    private fun applyStateParams() {
        val targetColor = when (state) {
            HudState.IDLE -> cyanDim
            HudState.LISTENING -> cyan
            HudState.THINKING -> amber
            HudState.SPEAKING -> cyan
        }
        animateAccentColor(targetColor)

        when (state) {
            HudState.IDLE -> {
                outerAnimator.duration = 12000
                midAnimator.duration = 8000
                pulseAnimator.duration = 2000
            }
            HudState.LISTENING -> {
                outerAnimator.duration = 3000
                midAnimator.duration = 2200
                pulseAnimator.duration = 500
            }
            HudState.THINKING -> {
                outerAnimator.duration = 1200
                midAnimator.duration = 900
                pulseAnimator.duration = 350
            }
            HudState.SPEAKING -> {
                outerAnimator.duration = 2000
                midAnimator.duration = 1500
                pulseAnimator.duration = 700
            }
        }
    }

    /** Blends the ring/core color over ~350ms instead of snapping instantly on state change. */
    private fun animateAccentColor(target: Int) {
        colorAnimator?.cancel()
        colorAnimator = ValueAnimator.ofObject(ArgbEvaluator(), accentColor, target).apply {
            duration = 350
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
        val maxRadius = minOf(width, height) / 2f * 0.92f

        drawOuterBloom(canvas, cx, cy, maxRadius)
        drawTickRing(canvas, cx, cy, maxRadius)
        drawDashedRing(canvas, cx, cy, maxRadius * 0.78f, outerRotation, 14, 10f, 4.5f)
        drawOrbitParticles(canvas, cx, cy, maxRadius * 0.78f, outerRotation)
        drawDashedRing(canvas, cx, cy, maxRadius * 0.60f, midRotation, 9, 20f, 3.5f)
        drawSpokes(canvas, cx, cy, maxRadius * 0.34f, maxRadius * 0.58f)
        drawCore(canvas, cx, cy, maxRadius * 0.34f)
    }

    /** A large, soft halo behind everything — the "glow" a real reactor would cast. */
    private fun drawOuterBloom(canvas: Canvas, cx: Float, cy: Float, maxRadius: Float) {
        val r = maxRadius * 1.35f
        bloomPaint.shader = RadialGradient(
            cx, cy, r,
            intArrayOf(
                Color.argb((pulseAlpha * 28).toInt(), Color.red(accentColor), Color.green(accentColor), Color.blue(accentColor)),
                Color.TRANSPARENT
            ),
            null, Shader.TileMode.CLAMP
        )
        canvas.drawCircle(cx, cy, r, bloomPaint)
    }

    /** Thin turbine-like spokes connecting the core to the middle ring, rotating opposite the core blades. */
    private fun drawSpokes(canvas: Canvas, cx: Float, cy: Float, innerR: Float, outerR: Float) {
        spokePaint.color = accentColor
        spokePaint.alpha = 55
        spokePaint.strokeWidth = 1.5f
        canvas.save()
        canvas.rotate(-bladeRotation * 0.5f, cx, cy)
        for (i in 0 until 8) {
            val angle = Math.toRadians(i * 45.0)
            val x1 = cx + (innerR * Math.cos(angle)).toFloat()
            val y1 = cy + (innerR * Math.sin(angle)).toFloat()
            val x2 = cx + (outerR * Math.cos(angle)).toFloat()
            val y2 = cy + (outerR * Math.sin(angle)).toFloat()
            canvas.drawLine(x1, y1, x2, y2, spokePaint)
        }
        canvas.restore()
    }

    /** A few small glowing "energy nodes" that orbit along the outer ring. */
    private fun drawOrbitParticles(canvas: Canvas, cx: Float, cy: Float, radius: Float, rotation: Float) {
        for (i in 0 until 3) {
            val angle = Math.toRadians((rotation + i * 120f).toDouble())
            val px = cx + (radius * Math.cos(angle)).toFloat()
            val py = cy + (radius * Math.sin(angle)).toFloat()

            particlePaint.shader = RadialGradient(
                px, py, 10f,
                intArrayOf(Color.argb(180, Color.red(accentColor), Color.green(accentColor), Color.blue(accentColor)), Color.TRANSPARENT),
                null, Shader.TileMode.CLAMP
            )
            canvas.drawCircle(px, py, 10f, particlePaint)

            particlePaint.shader = null
            particlePaint.color = Color.WHITE
            particlePaint.alpha = 230
            canvas.drawCircle(px, py, 2.2f, particlePaint)
        }
    }

    private fun drawTickRing(canvas: Canvas, cx: Float, cy: Float, radius: Float) {
        tickPaint.color = accentColor
        tickPaint.alpha = 70
        tickPaint.strokeWidth = 2f
        val tickCount = 60
        canvas.save()
        canvas.rotate(tickRotation, cx, cy)
        for (i in 0 until tickCount) {
            val angle = Math.toRadians((360.0 / tickCount) * i)
            val len = if (i % 5 == 0) 14f else 7f
            val startR = radius
            val endR = radius - len
            val x1 = cx + (startR * Math.cos(angle)).toFloat()
            val y1 = cy + (startR * Math.sin(angle)).toFloat()
            val x2 = cx + (endR * Math.cos(angle)).toFloat()
            val y2 = cy + (endR * Math.sin(angle)).toFloat()
            canvas.drawLine(x1, y1, x2, y2, tickPaint)
        }
        canvas.restore()
    }

    private fun drawDashedRing(
        canvas: Canvas, cx: Float, cy: Float, radius: Float,
        rotation: Float, segments: Int, strokeWidth: Float, gapDegrees: Float
    ) {
        ringPaint.color = accentColor
        ringPaint.strokeWidth = strokeWidth
        ringPaint.alpha = 220
        val sweepPerSegment = 360f / segments - gapDegrees
        val rect = android.graphics.RectF(cx - radius, cy - radius, cx + radius, cy + radius)
        for (i in 0 until segments) {
            val startAngle = rotation + i * (360f / segments)
            canvas.drawArc(rect, startAngle, sweepPerSegment, false, ringPaint)
        }
    }

    private fun drawCore(canvas: Canvas, cx: Float, cy: Float, baseRadius: Float) {
        val r = baseRadius * pulseScale

        coreGlowPaint.shader = RadialGradient(
            cx, cy, r * 2.2f,
            intArrayOf(
                Color.argb((pulseAlpha * 90).toInt(), Color.red(accentColor), Color.green(accentColor), Color.blue(accentColor)),
                Color.TRANSPARENT
            ),
            null, Shader.TileMode.CLAMP
        )
        canvas.drawCircle(cx, cy, r * 2.2f, coreGlowPaint)

        drawCoreBlades(canvas, cx, cy, r * 0.85f)

        corePaint.style = Paint.Style.STROKE
        corePaint.strokeWidth = 3f
        corePaint.color = accentColor
        corePaint.alpha = 255
        canvas.drawCircle(cx, cy, r, corePaint)

        corePaint.style = Paint.Style.FILL
        corePaint.alpha = (pulseAlpha * 90).toInt()
        canvas.drawCircle(cx, cy, r * 0.55f, corePaint)

        // Bright center point — the "ignition point" of the reactor.
        corePaint.alpha = 255
        canvas.drawCircle(cx, cy, r * 0.14f, corePaint)
    }

    /** Three rotating turbine-style blades inside the core — the signature reactor detail. */
    private fun drawCoreBlades(canvas: Canvas, cx: Float, cy: Float, radius: Float) {
        bladePaint.color = accentColor
        bladePaint.alpha = 150
        canvas.save()
        canvas.rotate(bladeRotation, cx, cy)
        for (i in 0 until 3) {
            canvas.save()
            canvas.rotate(i * 120f, cx, cy)
            val path = android.graphics.Path().apply {
                moveTo(cx, cy)
                quadTo(cx + radius * 0.35f, cy - radius * 0.18f, cx + radius, cy)
                quadTo(cx + radius * 0.35f, cy + radius * 0.18f, cx, cy)
                close()
            }
            canvas.drawPath(path, bladePaint)
            canvas.restore()
        }
        canvas.restore()
    }

    override fun onDetachedFromWindow() {
        outerAnimator.cancel()
        midAnimator.cancel()
        tickAnimator.cancel()
        pulseAnimator.cancel()
        bladeAnimator.cancel()
        super.onDetachedFromWindow()
    }
}
