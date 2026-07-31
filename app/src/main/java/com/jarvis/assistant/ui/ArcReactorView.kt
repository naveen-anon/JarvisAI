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
 * Iron Man Arc Reactor — Mark II style.
 * Bright white core, large triangular chamber, thick metal rings, strong cyan glow.
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
    private var rotTri = 0f
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
    private val aTri = ValueAnimator.ofFloat(0f, 360f).apply {
        duration = 20000; repeatCount = ValueAnimator.INFINITE
        interpolator = LinearInterpolator()
        addUpdateListener { rotTri = it.animatedValue as Float; invalidate() }
    }
    private val aPulse = ValueAnimator.ofFloat(0.88f, 1.12f).apply {
        duration = 1300; repeatCount = ValueAnimator.INFINITE; repeatMode = ValueAnimator.REVERSE
        addUpdateListener {
            pulse = it.animatedValue as Float
            glowA = 0.5f + (pulse - 0.88f) / 0.24f * 0.5f
            invalidate()
        }
    }

    init {
        aOuter.start(); aMid.start(); aTri.start(); aPulse.start()
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
                aOuter.duration = 14000; aMid.duration = 9000; aPulse.duration = 1600
            }
            HudState.LISTENING -> {
                aOuter.duration = 3000; aMid.duration = 2200; aPulse.duration = 500
            }
            HudState.THINKING -> {
                aOuter.duration = 1100; aMid.duration = 800; aPulse.duration = 300
            }
            HudState.SPEAKING -> {
                aOuter.duration = 2000; aMid.duration = 1400; aPulse.duration = 700
            }
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val cx = width / 2f
        val cy = height / 2f
        val R = min(width, height) / 2f * 0.88f

        drawBloom(canvas, cx, cy, R * 1.5f)

        // thick dark metal housing
        pStroke.style = Paint.Style.STROKE
        pStroke.strokeCap = Paint.Cap.ROUND
        pStroke.color = darkMetal
        pStroke.strokeWidth = R * 0.10f
        pStroke.alpha = 255
        canvas.drawCircle(cx, cy, R * 0.95f, pStroke)

        pStroke.color = midMetal
        pStroke.strokeWidth = R * 0.025f
        canvas.drawCircle(cx, cy, R * 0.99f, pStroke)
        canvas.drawCircle(cx, cy, R * 0.90f, pStroke)

        // outer segmented ring
        drawSegments(canvas, cx, cy, R * 0.84f, rotOuter, 16, R * 0.035f, 6f)

        // mid thin ring
        pStroke.color = accent
        pStroke.strokeWidth = R * 0.012f
        pStroke.alpha = 180
        canvas.drawCircle(cx, cy, R * 0.72f, pStroke)

        // mid segmented ring (opposite direction)
        drawSegments(canvas, cx, cy, R * 0.64f, rotMid, 10, R * 0.028f, 10f)

        drawTicks(canvas, cx, cy, R * 0.56f)

        // TRIANGLE — classic Arc Reactor signature
        drawTriangle(canvas, cx, cy, R * 0.46f)

        // white-hot core
        drawCore(canvas, cx, cy, R * 0.18f)
    }

    private fun drawBloom(canvas: Canvas, cx: Float, cy: Float, r: Float) {
        pGlow.shader = RadialGradient(
            cx, cy, r,
            intArrayOf(
                Color.argb((glowA * 50).toInt(), Color.red(accent), Color.green(accent), Color.blue(accent)),
                Color.argb((glowA * 18).toInt(), Color.red(accent), Color.green(accent), Color.blue(accent)),
                Color.TRANSPARENT
            ),
            floatArrayOf(0f, 0.5f, 1f),
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
        pStroke.alpha = 230
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
        pStroke.alpha = 100
        pStroke.strokeCap = Paint.Cap.ROUND
        canvas.save()
        canvas.rotate(rotMid * 0.3f, cx, cy)
        for (i in 0 until 48) {
            val a = Math.toRadians(i * 7.5)
            val len = if (i % 4 == 0) radius * 0.09f else radius * 0.04f
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

    /** Large visible triangle + channels — Iron Man signature. */
    private fun drawTriangle(canvas: Canvas, cx: Float, cy: Float, radius: Float) {
        canvas.save()
        canvas.rotate(rotTri * 0.08f, cx, cy)

        val plate = Path()
        for (i in 0 until 3) {
            val a = Math.toRadians(-90.0 + i * 120.0)
            val x = cx + (radius * cos(a)).toFloat()
            val y = cy + (radius * sin(a)).toFloat()
            if (i == 0) plate.moveTo(x, y) else plate.lineTo(x, y)
        }
        plate.close()

        pFill.color = Color.argb(45, Color.red(accent), Color.green(accent), Color.blue(accent))
        canvas.drawPath(plate, pFill)

        pStroke.color = accent
        pStroke.strokeWidth = 4.5f
        pStroke.alpha = 250
        pStroke.strokeJoin = Paint.Join.ROUND
        pStroke.strokeCap = Paint.Cap.ROUND
        canvas.drawPath(plate, pStroke)

        val inner = Path()
        val r2 = radius * 0.55f
        for (i in 0 until 3) {
            val a = Math.toRadians(-90.0 + i * 120.0)
            val x = cx + (r2 * cos(a)).toFloat()
            val y = cy + (r2 * sin(a)).toFloat()
            if (i == 0) inner.moveTo(x, y) else inner.lineTo(x, y)
        }
        inner.close()
        pStroke.strokeWidth = 2.5f
        pStroke.alpha = 190
        canvas.drawPath(inner, pStroke)

        // channels center → vertices
        pStroke.strokeWidth = 2.5f
        pStroke.alpha = 170
        for (i in 0 until 3) {
            val a = Math.toRadians(-90.0 + i * 120.0)
            canvas.drawLine(
                cx, cy,
                cx + (radius * 0.92f * cos(a)).toFloat(),
                cy + (radius * 0.92f * sin(a)).toFloat(),
                pStroke
            )
        }

        // glowing vertex nodes
        for (i in 0 until 3) {
            val a = Math.toRadians(-90.0 + i * 120.0)
            val x = cx + (radius * cos(a)).toFloat()
            val y = cy + (radius * sin(a)).toFloat()
            pGlow.shader = RadialGradient(
                x, y, radius * 0.14f,
                intArrayOf(Color.argb(220, 255, 255, 255), Color.TRANSPARENT),
                null, Shader.TileMode.CLAMP
            )
            canvas.drawCircle(x, y, radius * 0.14f, pGlow)
            pGlow.shader = null
            pFill.color = Color.WHITE
            pFill.alpha = 255
            canvas.drawCircle(x, y, radius * 0.05f, pFill)
        }

        canvas.restore()
    }

    private fun drawCore(canvas: Canvas, cx: Float, cy: Float, base: Float) {
        val r = base * pulse

        pGlow.shader = RadialGradient(
            cx, cy, r * 3.2f,
            intArrayOf(
                Color.argb((glowA * 150).toInt(), Color.red(accent), Color.green(accent), Color.blue(accent)),
                Color.argb((glowA * 55).toInt(), Color.red(accent), Color.green(accent), Color.blue(accent)),
                Color.TRANSPARENT
            ),
            floatArrayOf(0f, 0.35f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawCircle(cx, cy, r * 3.2f, pGlow)
        pGlow.shader = null

        pFill.shader = RadialGradient(
            cx, cy, r,
            intArrayOf(Color.WHITE, cyanHi, accent),
            floatArrayOf(0f, 0.4f, 1f),
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
        canvas.drawCircle(cx, cy, r * 0.32f, pFill)
    }

    override fun onDetachedFromWindow() {
        aOuter.cancel(); aMid.cancel(); aTri.cancel(); aPulse.cancel()
        colorAnim?.cancel()
        super.onDetachedFromWindow()
    }
}
