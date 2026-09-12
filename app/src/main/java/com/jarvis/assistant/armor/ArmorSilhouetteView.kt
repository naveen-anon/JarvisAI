package com.jarvis.assistant.armor

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import kotlin.math.sin

/**
 * Detail preview — uses the same distinct per-mark shapes as Hall of Armor.
 * Style.METAL for colored armor look (not cartoon robot).
 */
class ArmorSilhouetteView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    var primaryColor: Int = Color.parseColor("#C41E3A")
        set(value) { field = value; invalidate() }
    var secondaryColor: Int = Color.parseColor("#F5C518")
        set(value) { field = value; invalidate() }
    var reactorColor: Int = Color.parseColor("#00E5FF")
        set(value) { field = value; invalidate() }
    var mark: ArmorMark? = null
        set(value) { field = value; invalidate() }

    private var animTime = 0f
    private val animator = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = 4000
        repeatCount = ValueAnimator.INFINITE
        interpolator = LinearInterpolator()
        addUpdateListener { animTime = it.animatedValue as Float; invalidate() }
    }

    override fun onAttachedToWindow() { super.onAttachedToWindow(); animator.start() }
    override fun onDetachedFromWindow() { animator.cancel(); super.onDetachedFromWindow() }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat(); val h = height.toFloat()
        if (w <= 0 || h <= 0) return
        val cx = w / 2f
        val bob = sin(animTime * 2f * Math.PI).toFloat() * (h * 0.012f)
        val m = mark ?: ArmorCatalog.byNumber(42) ?: return
        canvas.save()
        canvas.translate(0f, bob)
        // soft glow
        val glow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(cx, h * 0.45f, w * 0.45f,
                intArrayOf(Color.argb(50, Color.red(primaryColor), Color.green(primaryColor), Color.blue(primaryColor)), Color.TRANSPARENT),
                null, Shader.TileMode.CLAMP)
        }
        canvas.drawCircle(cx, h * 0.45f, w * 0.45f, glow)
        ArmorBlueprintDrawer.draw(
            canvas, cx, h * 0.48f, h * 0.42f, m,
            ArmorBlueprintDrawer.Style.METAL,
            selected = true,
            primary = primaryColor,
            secondary = secondaryColor,
            reactor = reactorColor
        )
        canvas.restore()
    }
}
