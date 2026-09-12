package com.jarvis.assistant.armor

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import kotlin.math.sin

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
        val m = mark ?: ArmorCatalog.byNumber(42) ?: return
        val cx = w / 2f
        val bob = sin(animTime * 2f * Math.PI).toFloat() * (h * 0.01f)
        canvas.save()
        canvas.translate(0f, bob)
        val glow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(
                cx, h * 0.42f, w * 0.42f,
                intArrayOf(
                    Color.argb(55, Color.red(primaryColor), Color.green(primaryColor), Color.blue(primaryColor)),
                    Color.TRANSPARENT
                ),
                null, Shader.TileMode.CLAMP
            )
        }
        canvas.drawCircle(cx, h * 0.42f, w * 0.42f, glow)
        ArmorBlueprintDrawer.draw(
            canvas, cx, h * 0.50f, h * 0.46f, m,
            ArmorBlueprintDrawer.Style.METAL,
            selected = true,
            primary = primaryColor,
            secondary = secondaryColor,
            reactor = reactorColor
        )
        canvas.restore()
    }
}
