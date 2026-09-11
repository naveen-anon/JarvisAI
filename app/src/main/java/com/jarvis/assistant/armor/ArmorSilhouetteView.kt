package com.jarvis.assistant.armor

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * MCU-style metallic armor silhouette drawn with layered Canvas paths.
 * Not a full GLTF 3D model (copyright / size), but reads as a lit Iron Man suit:
 * helmet faceplate, eye slits, shoulder pods, chest RT, plate seams, boot jets.
 */
class ArmorSilhouetteView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    enum class SuitStyle {
        CLASSIC,    // Mark III / 42 / 46 / 50 / 85
        PROTOTYPE,  // Mark I crude
        SILVER,     // Mark II / Silver Centurion
        HEAVY,      // Hulkbuster
        NANO,       // nano shimmer edges
        RESCUE      // blue support
    }

    var primaryColor: Int = Color.parseColor("#C41E3A")
        set(value) { field = value; invalidate() }
    var secondaryColor: Int = Color.parseColor("#F5C518")
        set(value) { field = value; invalidate() }
    var reactorColor: Int = Color.parseColor("#00E5FF")
        set(value) { field = value; invalidate() }
    var suitStyle: SuitStyle = SuitStyle.CLASSIC
        set(value) { field = value; invalidate() }

    private var animTime = 0f

    private val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val darkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val edgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2.5f
        strokeJoin = Paint.Join.ROUND
    }
    private val goldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val visorPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val highlightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.5f
    }
    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private val animator = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = 3600
        repeatCount = ValueAnimator.INFINITE
        interpolator = LinearInterpolator()
        addUpdateListener {
            animTime = it.animatedValue as Float
            invalidate()
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        animator.start()
    }

    override fun onDetachedFromWindow() {
        animator.cancel()
        super.onDetachedFromWindow()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return

        val scale = min(w / 200f, h / 300f)
        val cx = w / 2f
        val bob = sin(animTime * 2f * Math.PI).toFloat() * (6f * scale)
        val sway = sin(animTime * 2f * Math.PI + 0.8f).toFloat() * (3f * scale)
        val pulse = 0.55f + 0.45f * ((sin(animTime * 4f * Math.PI.toFloat()) + 1f) / 2f)

        // Style-driven bulk
        val bulk = when (suitStyle) {
            SuitStyle.HEAVY -> 1.22f
            SuitStyle.PROTOTYPE -> 1.12f
            SuitStyle.NANO -> 0.96f
            else -> 1f
        }

        canvas.save()
        canvas.translate(sway, bob)

        // Ambient back glow
        glowPaint.shader = RadialGradient(
            cx, h * 0.42f, w * 0.48f,
            intArrayOf(withAlpha(primaryColor, 55), Color.TRANSPARENT),
            null, Shader.TileMode.CLAMP
        )
        canvas.drawCircle(cx, h * 0.42f, w * 0.48f, glowPaint)

        val headCy = h * 0.13f
        val headR = w * 0.105f * bulk
        val shoulderY = h * 0.23f
        val shoulderW = w * 0.36f * bulk
        val chestY = h * 0.38f
        val waistY = h * 0.55f
        val hipY = h * 0.60f
        val kneeY = h * 0.78f
        val footY = h * 0.96f

        // ===== HELMET =====
        drawHelmet(canvas, cx, headCy, headR, pulse)

        // ===== TORSO =====
        drawTorso(canvas, cx, shoulderY, shoulderW, chestY, waistY, bulk)

        // ===== SHOULDERS + ARMS =====
        drawShoulderAndArm(canvas, cx, shoulderY, shoulderW, waistY, -1, bulk)
        drawShoulderAndArm(canvas, cx, shoulderY, shoulderW, waistY, 1, bulk)

        // ===== WAIST + LEGS =====
        drawLegs(canvas, cx, hipY, kneeY, footY, shoulderW * 0.55f, bulk)

        // ===== ARC REACTOR =====
        drawReactor(canvas, cx, chestY, w * 0.055f * bulk, pulse)

        // Soft floor shadow
        shadowPaint.color = withAlpha(Color.BLACK, 50)
        canvas.drawOval(cx - w * 0.18f, h * 0.97f, cx + w * 0.18f, h * 0.995f, shadowPaint)

        canvas.restore()
    }

    private fun drawHelmet(canvas: Canvas, cx: Float, cy: Float, r: Float, pulse: Float) {
        // Dome
        bodyPaint.shader = verticalMetal(cx, cy - r, cy + r * 1.1f, primaryColor)
        val helm = Path().apply {
            addRoundRect(cx - r * 0.95f, cy - r * 0.95f, cx + r * 0.95f, cy + r * 1.05f, r * 0.55f, r * 0.55f, Path.Direction.CW)
        }
        canvas.drawPath(helm, bodyPaint)

        // Gold faceplate frame
        goldPaint.color = secondaryColor
        val face = Path().apply {
            moveTo(cx - r * 0.72f, cy - r * 0.15f)
            lineTo(cx + r * 0.72f, cy - r * 0.15f)
            lineTo(cx + r * 0.62f, cy + r * 0.75f)
            lineTo(cx - r * 0.62f, cy + r * 0.75f)
            close()
        }
        canvas.drawPath(face, goldPaint)

        // Dark chin
        darkPaint.color = darken(primaryColor, 0.55f)
        canvas.drawRoundRect(cx - r * 0.45f, cy + r * 0.35f, cx + r * 0.45f, cy + r * 0.85f, 8f, 8f, darkPaint)

        // Eye slits (MCU classic)
        val eyeY = cy + r * 0.05f
        val eyeH = r * 0.16f
        val eyeW = r * 0.28f
        val eyeGap = r * 0.12f
        visorPaint.shader = LinearGradient(
            cx, eyeY, cx, eyeY + eyeH,
            intArrayOf(Color.WHITE, reactorColor, withAlpha(reactorColor, 180)),
            null, Shader.TileMode.CLAMP
        )
        // left eye
        canvas.drawRoundRect(cx - eyeGap - eyeW, eyeY, cx - eyeGap, eyeY + eyeH, 4f, 4f, visorPaint)
        // right eye
        canvas.drawRoundRect(cx + eyeGap, eyeY, cx + eyeGap + eyeW, eyeY + eyeH, 4f, 4f, visorPaint)

        // Eye glow
        glowPaint.shader = RadialGradient(
            cx, eyeY + eyeH / 2f, r * 0.9f,
            intArrayOf(withAlpha(reactorColor, (90 * pulse).toInt()), Color.TRANSPARENT),
            null, Shader.TileMode.CLAMP
        )
        canvas.drawCircle(cx, eyeY + eyeH / 2f, r * 0.85f, glowPaint)

        // Forehead seam
        edgePaint.color = withAlpha(secondaryColor, 200)
        edgePaint.strokeWidth = 2f
        canvas.drawLine(cx - r * 0.5f, cy - r * 0.35f, cx + r * 0.5f, cy - r * 0.35f, edgePaint)

        // Outline
        edgePaint.color = withAlpha(secondaryColor, 220)
        edgePaint.strokeWidth = 2.5f
        canvas.drawPath(helm, edgePaint)
        bodyPaint.shader = null
    }

    private fun drawTorso(
        canvas: Canvas, cx: Float, shoulderY: Float, shoulderW: Float,
        chestY: Float, waistY: Float, bulk: Float
    ) {
        // Main chest plate
        val torso = Path().apply {
            moveTo(cx - shoulderW / 2f, shoulderY)
            lineTo(cx + shoulderW / 2f, shoulderY)
            lineTo(cx + shoulderW * 0.38f, waistY)
            lineTo(cx - shoulderW * 0.38f, waistY)
            close()
        }
        bodyPaint.shader = verticalMetal(cx, shoulderY, waistY, primaryColor)
        canvas.drawPath(torso, bodyPaint)
        bodyPaint.shader = null

        // Gold collar / trapezoid
        goldPaint.color = secondaryColor
        val collar = Path().apply {
            moveTo(cx - shoulderW * 0.22f, shoulderY + 4f)
            lineTo(cx + shoulderW * 0.22f, shoulderY + 4f)
            lineTo(cx + shoulderW * 0.18f, shoulderY + (chestY - shoulderY) * 0.45f)
            lineTo(cx - shoulderW * 0.18f, shoulderY + (chestY - shoulderY) * 0.45f)
            close()
        }
        canvas.drawPath(collar, goldPaint)

        // Side plate seams
        edgePaint.color = withAlpha(secondaryColor, 180)
        edgePaint.strokeWidth = 2f
        canvas.drawLine(cx - shoulderW * 0.32f, shoulderY + 12f, cx - shoulderW * 0.28f, waistY - 8f, edgePaint)
        canvas.drawLine(cx + shoulderW * 0.32f, shoulderY + 12f, cx + shoulderW * 0.28f, waistY - 8f, edgePaint)

        // Ab section
        darkPaint.color = darken(primaryColor, 0.75f)
        canvas.drawRoundRect(
            cx - shoulderW * 0.28f, waistY - (waistY - chestY) * 0.35f,
            cx + shoulderW * 0.28f, waistY + 6f,
            10f, 10f, darkPaint
        )

        // Gold belt
        goldPaint.color = secondaryColor
        canvas.drawRoundRect(
            cx - shoulderW * 0.34f, waistY - 4f,
            cx + shoulderW * 0.34f, waistY + 14f,
            6f, 6f, goldPaint
        )

        edgePaint.color = withAlpha(secondaryColor, 230)
        edgePaint.strokeWidth = 2.5f
        canvas.drawPath(torso, edgePaint)

        // Specular highlight (fake 3D light)
        highlightPaint.color = withAlpha(Color.WHITE, 55)
        canvas.drawLine(
            cx - shoulderW * 0.25f, shoulderY + 18f,
            cx - shoulderW * 0.18f, waistY - 20f,
            highlightPaint
        )
    }

    private fun drawShoulderAndArm(
        canvas: Canvas, cx: Float, shoulderY: Float, shoulderW: Float,
        waistY: Float, side: Int, bulk: Float
    ) {
        val s = side.toFloat()
        val sx = cx + s * (shoulderW / 2f)

        // Shoulder pod
        val podR = shoulderW * 0.14f * bulk
        bodyPaint.shader = radialMetal(sx, shoulderY, podR * 1.3f, primaryColor)
        canvas.drawCircle(sx, shoulderY + 6f, podR, bodyPaint)
        bodyPaint.shader = null
        goldPaint.color = secondaryColor
        canvas.drawCircle(sx, shoulderY + 6f, podR * 0.55f, goldPaint)
        edgePaint.color = withAlpha(secondaryColor, 220)
        canvas.drawCircle(sx, shoulderY + 6f, podR, edgePaint)

        // Upper arm
        val armTopX = sx + s * podR * 0.3f
        val armPath = Path().apply {
            moveTo(sx - s * podR * 0.35f, shoulderY + podR * 0.6f)
            lineTo(sx + s * podR * 0.9f, shoulderY + podR * 0.4f)
            lineTo(sx + s * podR * 0.75f, waistY - 10f)
            lineTo(sx - s * podR * 0.15f, waistY - 6f)
            close()
        }
        bodyPaint.color = primaryColor
        canvas.drawPath(armPath, bodyPaint)
        edgePaint.strokeWidth = 2f
        canvas.drawPath(armPath, edgePaint)

        // Gauntlet gold band
        goldPaint.color = secondaryColor
        val gy = waistY - 28f
        canvas.drawRoundRect(
            min(sx - s * podR * 0.05f, sx + s * podR * 0.7f),
            gy,
            maxOf(sx - s * podR * 0.05f, sx + s * podR * 0.7f),
            gy + 12f,
            4f, 4f, goldPaint
        )

        // Repulsor glow at hand
        val hx = sx + s * podR * 0.45f
        val hy = waistY - 4f
        glowPaint.shader = RadialGradient(
            hx, hy, podR * 0.9f,
            intArrayOf(withAlpha(reactorColor, 140), Color.TRANSPARENT),
            null, Shader.TileMode.CLAMP
        )
        canvas.drawCircle(hx, hy, podR * 0.55f, glowPaint)
        bodyPaint.color = Color.WHITE
        canvas.drawCircle(hx, hy, podR * 0.18f, bodyPaint)
    }

    private fun drawLegs(
        canvas: Canvas, cx: Float, hipY: Float, kneeY: Float, footY: Float,
        hipW: Float, bulk: Float
    ) {
        val gap = hipW * 0.08f
        listOf(-1, 1).forEach { side ->
            val s = side.toFloat()
            val outer = cx + s * (hipW / 2f)
            val inner = cx + s * gap

            val leg = Path().apply {
                moveTo(min(outer, inner), hipY)
                lineTo(maxOf(outer, inner), hipY)
                lineTo(maxOf(outer * 0.92f + cx * 0.08f, inner), footY - 8f)
                lineTo(min(outer * 0.92f + cx * 0.08f, inner), footY - 8f)
                close()
            }
            // fix orientation simply with rects for stability
            val left = if (side < 0) outer else inner
            val right = if (side < 0) inner else outer
            bodyPaint.shader = verticalMetal(cx, hipY, footY, primaryColor)
            canvas.drawRoundRect(left, hipY, right, footY - 6f, 12f, 12f, bodyPaint)
            bodyPaint.shader = null

            // Knee gold
            goldPaint.color = secondaryColor
            val ky = kneeY
            canvas.drawRoundRect(left + 4f, ky - 10f, right - 4f, ky + 12f, 6f, 6f, goldPaint)

            // Shin seam
            edgePaint.color = withAlpha(secondaryColor, 160)
            edgePaint.strokeWidth = 1.8f
            canvas.drawLine((left + right) / 2f, ky + 14f, (left + right) / 2f, footY - 18f, edgePaint)

            // Boot
            goldPaint.color = secondaryColor
            canvas.drawRoundRect(left - 4f, footY - 18f, right + 4f, footY + 4f, 6f, 6f, goldPaint)

            // Boot jet glow
            glowPaint.shader = RadialGradient(
                (left + right) / 2f, footY + 2f, 18f,
                intArrayOf(withAlpha(reactorColor, 120), Color.TRANSPARENT),
                null, Shader.TileMode.CLAMP
            )
            canvas.drawCircle((left + right) / 2f, footY + 2f, 14f, glowPaint)

            edgePaint.color = withAlpha(secondaryColor, 200)
            canvas.drawRoundRect(left, hipY, right, footY - 6f, 12f, 12f, edgePaint)
        }
    }

    private fun drawReactor(canvas: Canvas, cx: Float, cy: Float, r: Float, pulse: Float) {
        val glowR = r * (2.4f + pulse * 0.8f)
        glowPaint.shader = RadialGradient(
            cx, cy, glowR,
            intArrayOf(withAlpha(reactorColor, (200 * pulse).toInt()), withAlpha(reactorColor, 40), Color.TRANSPARENT),
            floatArrayOf(0f, 0.45f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawCircle(cx, cy, glowR, glowPaint)

        // Outer ring
        bodyPaint.color = Color.WHITE
        canvas.drawCircle(cx, cy, r * 1.05f, bodyPaint)
        bodyPaint.color = reactorColor
        canvas.drawCircle(cx, cy, r * 0.92f, bodyPaint)

        // Triangle RT for Mark VI style when NANO/CLASSIC slightly variant
        if (suitStyle == SuitStyle.NANO || suitStyle == SuitStyle.CLASSIC) {
            val tri = Path().apply {
                val t = r * 0.55f
                moveTo(cx, cy - t)
                lineTo(cx + t * 0.9f, cy + t * 0.65f)
                lineTo(cx - t * 0.9f, cy + t * 0.65f)
                close()
            }
            bodyPaint.color = Color.WHITE
            canvas.drawPath(tri, bodyPaint)
        } else {
            bodyPaint.color = Color.WHITE
            canvas.drawCircle(cx, cy, r * 0.4f, bodyPaint)
        }

        // Spinning highlight ring
        edgePaint.color = withAlpha(Color.WHITE, (140 + 80 * pulse).toInt())
        edgePaint.strokeWidth = 2f
        val a = animTime * 360f
        canvas.save()
        canvas.rotate(a, cx, cy)
        canvas.drawArc(
            cx - r * 1.15f, cy - r * 1.15f, cx + r * 1.15f, cy + r * 1.15f,
            -30f, 80f, false, edgePaint
        )
        canvas.restore()
    }

    private fun verticalMetal(cx: Float, top: Float, bottom: Float, base: Int): Shader {
        return LinearGradient(
            cx, top, cx, bottom,
            intArrayOf(lighten(base, 0.25f), base, darken(base, 0.35f)),
            floatArrayOf(0f, 0.45f, 1f),
            Shader.TileMode.CLAMP
        )
    }

    private fun radialMetal(cx: Float, cy: Float, r: Float, base: Int): Shader {
        return RadialGradient(
            cx - r * 0.25f, cy - r * 0.25f, r,
            intArrayOf(lighten(base, 0.35f), base, darken(base, 0.25f)),
            null, Shader.TileMode.CLAMP
        )
    }

    private fun withAlpha(color: Int, alpha: Int): Int =
        Color.argb(alpha.coerceIn(0, 255), Color.red(color), Color.green(color), Color.blue(color))

    private fun darken(color: Int, factor: Float): Int {
        val f = factor.coerceIn(0f, 1f)
        return Color.rgb(
            (Color.red(color) * f).toInt().coerceIn(0, 255),
            (Color.green(color) * f).toInt().coerceIn(0, 255),
            (Color.blue(color) * f).toInt().coerceIn(0, 255)
        )
    }

    private fun lighten(color: Int, amount: Float): Int {
        val a = amount.coerceIn(0f, 1f)
        return Color.rgb(
            (Color.red(color) + (255 - Color.red(color)) * a).toInt().coerceIn(0, 255),
            (Color.green(color) + (255 - Color.green(color)) * a).toInt().coerceIn(0, 255),
            (Color.blue(color) + (255 - Color.blue(color)) * a).toInt().coerceIn(0, 255)
        )
    }
}
