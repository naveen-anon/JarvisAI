package com.jarvis.assistant.armor

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.view.ViewConfiguration
import kotlin.math.max
import kotlin.math.min

/**
 * Interactive Iron Man 3–style Hall of Armor.
 * Each mark draws a distinct cyan blueprint silhouette (heavy / claws / slim / pack…),
 * inspired by the official IM3 holographic archive poster.
 */
class HoloArmorGridView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    interface Listener {
        fun onSuitSelected(mark: ArmorMark)
        fun onSuitActivated(mark: ArmorMark) {}
    }

    var listener: Listener? = null

    private val marks: List<ArmorMark> = ArmorCatalog.all
    private var selectedIndex = 0

    private var scale = 1f
    private var offsetX = 0f
    private var offsetY = 0f
    private val minScale = 0.5f
    private val maxScale = 3f

    private var draggingIndex = -1
    private val suitOffsets = HashMap<Int, PointF>()

    private val cols = 7
    private val cellW = 118f
    private val cellH = 158f
    private val pad = 28f

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#020B14") }
    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#0A3A4A"); strokeWidth = 1f; style = Paint.Style.STROKE
    }
    private val framePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#C41E3A"); strokeWidth = 4f; style = Paint.Style.STROKE
    }
    private val cyanStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#00E5FF"); strokeWidth = 2f; style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND; strokeCap = Paint.Cap.ROUND
    }
    private val cyanFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#2200E5FF"); style = Paint.Style.FILL
    }
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#00E5FF"); style = Paint.Style.STROKE; strokeWidth = 3f
        maskFilter = BlurMaskFilter(10f, BlurMaskFilter.Blur.OUTER)
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#7AD4E8"); textAlign = Paint.Align.CENTER
        textSize = 11f; typeface = Typeface.MONOSPACE
    }
    private val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#00E5FF"); textAlign = Paint.Align.CENTER
        textSize = 26f; typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        letterSpacing = 0.1f
    }
    private val selectPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#3500E5FF"); style = Paint.Style.FILL
    }
    private val corePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#E8FFFF"); style = Paint.Style.FILL
    }

    private val scaleDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            val old = scale
            scale = (scale * detector.scaleFactor).coerceIn(minScale, maxScale)
            offsetX = detector.focusX - (detector.focusX - offsetX) * (scale / old)
            offsetY = detector.focusY - (detector.focusY - offsetY) * (scale / old)
            invalidate(); return true
        }
    })

    private val gesture = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onDown(e: MotionEvent) = true
        override fun onSingleTapUp(e: MotionEvent): Boolean {
            val idx = hitTest(e.x, e.y)
            if (idx >= 0) { selectedIndex = idx; listener?.onSuitSelected(marks[idx]); invalidate(); return true }
            return false
        }
        override fun onDoubleTap(e: MotionEvent): Boolean {
            val idx = hitTest(e.x, e.y)
            if (idx >= 0) { selectedIndex = idx; listener?.onSuitActivated(marks[idx]); return true }
            scale = 1f; offsetX = 0f; offsetY = 0f; invalidate(); return true
        }
        override fun onLongPress(e: MotionEvent) {
            val idx = hitTest(e.x, e.y)
            if (idx >= 0) { draggingIndex = idx; lastPanX = e.x; lastPanY = e.y; parent.requestDisallowInterceptTouchEvent(true); invalidate() }
        }
    })

    private var lastPanX = 0f
    private var lastPanY = 0f
    private var panning = false
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop

    init { setLayerType(LAYER_TYPE_SOFTWARE, null) }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val rows = (marks.size + cols - 1) / cols
        val contentW = cols * cellW + pad * 2
        val contentH = rows * cellH + pad * 2 + 80f
        offsetX = (w - contentW * scale) / 2f
        offsetY = (h - contentH * scale) / 2f + 16f
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat(); val h = height.toFloat()
        canvas.drawRect(0f, 0f, w, h, bgPaint)
        var gx = offsetX % (36f * scale)
        while (gx < w) { canvas.drawLine(gx, 0f, gx, h, gridPaint); gx += 36f * scale }
        var gy = offsetY % (36f * scale)
        while (gy < h) { canvas.drawLine(0f, gy, w, gy, gridPaint); gy += 36f * scale }

        canvas.save()
        canvas.translate(offsetX, offsetY)
        canvas.scale(scale, scale)

        val rows = (marks.size + cols - 1) / cols
        val contentW = cols * cellW + pad * 2
        val contentH = rows * cellH + pad * 2 + 48f
        canvas.drawRoundRect(8f, 8f, contentW - 8f, contentH - 8f, 16f, 16f, framePaint)
        cyanStroke.strokeWidth = 1.4f
        canvas.drawRoundRect(14f, 14f, contentW - 14f, contentH - 14f, 12f, 12f, cyanStroke)
        canvas.drawText("IRON MAN 3  ·  HALL OF ARMOR", contentW / 2f, 40f, titlePaint)

        marks.forEachIndexed { i, mark ->
            val col = i % cols; val row = i / cols
            var cx = pad + col * cellW + cellW / 2f
            var cy = pad + 52f + row * cellH + cellH / 2f
            suitOffsets[i]?.let { cx += it.x; cy += it.y }

            if (i == selectedIndex) {
                canvas.drawRoundRect(cx - cellW * 0.42f, cy - cellH * 0.42f, cx + cellW * 0.42f, cy + cellH * 0.42f, 10f, 10f, selectPaint)
                canvas.drawRoundRect(cx - cellW * 0.42f, cy - cellH * 0.42f, cx + cellW * 0.42f, cy + cellH * 0.42f, 10f, 10f, glowPaint)
            }
            drawMarkSilhouette(canvas, cx, cy, cellH * 0.36f, mark, i == selectedIndex)
            textPaint.color = if (i == selectedIndex) Color.parseColor("#E0FFFF") else Color.parseColor("#5AA8B8")
            canvas.drawText(mark.roman, cx, cy + cellH * 0.40f, textPaint)
        }
        canvas.restore()

        textPaint.textSize = 12f; textPaint.color = Color.parseColor("#7AD4E8")
        canvas.drawText("PINCH zoom · DRAG pan · LONG-PRESS move · TAP select · DOUBLE-TAP open", w / 2f, h - 22f, textPaint)
        if (selectedIndex in marks.indices) {
            titlePaint.textSize = 15f
            val m = marks[selectedIndex]
            canvas.drawText("MARK ${m.roman}  ·  ${m.codename}", w / 2f, h - 44f, titlePaint)
        }
    }

    private enum class Body {
        STANDARD, CRUDE, SLIM, HEAVY, CLAW, WIDE, PACK, HAMMER, BONES, NANO
    }

    private fun bodyFor(mark: ArmorMark): Body {
        val n = mark.number
        val c = mark.codename.lowercase()
        return when {
            n == 1 -> Body.CRUDE
            n == 44 || c.contains("hulk") || c.contains("igor") || c.contains("tank") -> Body.HEAVY
            c.contains("snapper") || c.contains("claw") -> Body.CLAW
            c.contains("centurion") || n == 33 -> Body.WIDE
            c.contains("starboost") || c.contains("pack") -> Body.PACK
            c.contains("hammer") -> Body.HAMMER
            c.contains("bones") || c.contains("sneaky") || n == 15 || n == 41 -> Body.SLIM
            n >= 50 || c.contains("nano") || n == 85 -> Body.NANO
            n in 12..14 -> Body.SLIM
            n in 24..26 -> Body.HEAVY
            n in 34..38 -> Body.HEAVY
            else -> Body.STANDARD
        }
    }

    private fun drawMarkSilhouette(canvas: Canvas, cx: Float, cy: Float, h: Float, mark: ArmorMark, selected: Boolean) {
        val p = if (selected) glowPaint else cyanStroke
        p.strokeWidth = if (selected) 2.6f else 1.9f
        val body = bodyFor(mark)

        val bulk = when (body) {
            Body.HEAVY, Body.HAMMER -> 1.28f
            Body.CRUDE -> 1.15f
            Body.WIDE -> 1.12f
            Body.SLIM, Body.BONES -> 0.82f
            Body.NANO -> 0.95f
            else -> 1f
        }
        val shoulderW = h * 0.52f * bulk
        val waistW = h * 0.30f * (if (body == Body.SLIM) 0.85f else 1f)
        val headR = h * (if (body == Body.HEAVY) 0.14f else 0.115f)
        val headCy = cy - h * 0.70f
        val shoulderY = cy - h * 0.50f
        val chestY = cy - h * 0.30f
        val waistY = cy - h * 0.02f
        val footY = cy + h * 0.68f

        // Head / faceplate
        when (body) {
            Body.CRUDE -> {
                canvas.drawRoundRect(cx - headR, headCy - headR, cx + headR, headCy + headR * 1.1f, 4f, 4f, p)
            }
            Body.HAMMER -> {
                canvas.drawCircle(cx, headCy, headR * 1.15f, p)
                canvas.drawLine(cx - headR * 1.1f, headCy - headR * 0.2f, cx + headR * 1.1f, headCy - headR * 0.2f, p)
            }
            else -> canvas.drawCircle(cx, headCy, headR, p)
        }
        // Visor slit
        canvas.drawLine(cx - headR * 0.55f, headCy + headR * 0.05f, cx + headR * 0.55f, headCy + headR * 0.05f, p)

        // Torso path
        val torso = Path().apply {
            moveTo(cx - shoulderW / 2, shoulderY)
            lineTo(cx + shoulderW / 2, shoulderY)
            lineTo(cx + waistW / 2, waistY)
            lineTo(cx - waistW / 2, waistY)
            close()
        }
        canvas.drawPath(torso, cyanFill)
        canvas.drawPath(torso, p)

        // Arc reactor (circle or triangle for nano/classic late marks)
        if (body == Body.NANO || mark.number in listOf(6, 7, 42, 50, 85)) {
            val t = h * 0.055f
            val tri = Path().apply {
                moveTo(cx, chestY - t)
                lineTo(cx + t * 0.9f, chestY + t * 0.65f)
                lineTo(cx - t * 0.9f, chestY + t * 0.65f)
                close()
            }
            canvas.drawPath(tri, p)
            canvas.drawCircle(cx, chestY, h * 0.02f, corePaint)
        } else {
            canvas.drawCircle(cx, chestY, h * 0.055f, p)
            canvas.drawCircle(cx, chestY, h * 0.025f, corePaint)
        }

        // Shoulder pods
        val podR = h * 0.065f * bulk
        canvas.drawCircle(cx - shoulderW / 2, shoulderY, podR, p)
        canvas.drawCircle(cx + shoulderW / 2, shoulderY, podR, p)

        // Arms
        when (body) {
            Body.CLAW -> {
                // claw / pincer ends
                val lx = cx - shoulderW / 2 - h * 0.06f
                val rx = cx + shoulderW / 2 + h * 0.06f
                val hy = waistY + h * 0.08f
                canvas.drawLine(cx - shoulderW / 2, shoulderY + 2f, lx, hy, p)
                canvas.drawLine(cx + shoulderW / 2, shoulderY + 2f, rx, hy, p)
                // pincers
                canvas.drawLine(lx, hy, lx - h * 0.06f, hy + h * 0.08f, p)
                canvas.drawLine(lx, hy, lx + h * 0.04f, hy + h * 0.09f, p)
                canvas.drawLine(rx, hy, rx + h * 0.06f, hy + h * 0.08f, p)
                canvas.drawLine(rx, hy, rx - h * 0.04f, hy + h * 0.09f, p)
            }
            Body.HEAVY, Body.HAMMER -> {
                canvas.drawLine(cx - shoulderW / 2, shoulderY + 4f, cx - shoulderW / 2 - h * 0.12f, waistY + h * 0.05f, p)
                canvas.drawLine(cx + shoulderW / 2, shoulderY + 4f, cx + shoulderW / 2 + h * 0.12f, waistY + h * 0.05f, p)
                // thick gauntlets
                canvas.drawCircle(cx - shoulderW / 2 - h * 0.12f, waistY + h * 0.05f, h * 0.05f, p)
                canvas.drawCircle(cx + shoulderW / 2 + h * 0.12f, waistY + h * 0.05f, h * 0.05f, p)
            }
            Body.PACK -> {
                canvas.drawLine(cx - shoulderW / 2, shoulderY + 4f, cx - shoulderW / 2 - h * 0.05f, waistY, p)
                canvas.drawLine(cx + shoulderW / 2, shoulderY + 4f, cx + shoulderW / 2 + h * 0.05f, waistY, p)
                // back boosters
                canvas.drawRect(cx - h * 0.12f, shoulderY - h * 0.08f, cx - h * 0.04f, shoulderY + h * 0.1f, p)
                canvas.drawRect(cx + h * 0.04f, shoulderY - h * 0.08f, cx + h * 0.12f, shoulderY + h * 0.1f, p)
            }
            else -> {
                canvas.drawLine(cx - shoulderW / 2, shoulderY + 4f, cx - shoulderW / 2 - h * 0.07f, waistY, p)
                canvas.drawLine(cx + shoulderW / 2, shoulderY + 4f, cx + shoulderW / 2 + h * 0.07f, waistY, p)
            }
        }

        // Legs
        val legSpread = when (body) {
            Body.HEAVY, Body.CRUDE -> waistW * 0.38f
            Body.SLIM -> waistW * 0.22f
            else -> waistW * 0.28f
        }
        canvas.drawLine(cx - legSpread, waistY + 2f, cx - legSpread * 0.85f, footY, p)
        canvas.drawLine(cx + legSpread, waistY + 2f, cx + legSpread * 0.85f, footY, p)
        // boots
        val bootW = if (body == Body.HEAVY) h * 0.12f else h * 0.09f
        canvas.drawLine(cx - legSpread * 0.85f - bootW * 0.3f, footY, cx - legSpread * 0.85f + bootW * 0.5f, footY, p)
        canvas.drawLine(cx + legSpread * 0.85f - bootW * 0.5f, footY, cx + legSpread * 0.85f + bootW * 0.3f, footY, p)

        // Extra: Mark I chest bar / crude plates
        if (body == Body.CRUDE) {
            canvas.drawLine(cx - waistW * 0.35f, chestY - h * 0.06f, cx + waistW * 0.35f, chestY - h * 0.06f, p)
            canvas.drawLine(cx - waistW * 0.35f, chestY + h * 0.06f, cx + waistW * 0.35f, chestY + h * 0.06f, p)
        }
    }

    private fun contentPoint(sx: Float, sy: Float) = PointF((sx - offsetX) / scale, (sy - offsetY) / scale)

    private fun hitTest(sx: Float, sy: Float): Int {
        val pt = contentPoint(sx, sy)
        marks.forEachIndexed { i, _ ->
            val col = i % cols; val row = i / cols
            var cx = pad + col * cellW + cellW / 2f
            var cy = pad + 52f + row * cellH + cellH / 2f
            suitOffsets[i]?.let { cx += it.x; cy += it.y }
            if (kotlin.math.abs(pt.x - cx) < cellW * 0.4f && kotlin.math.abs(pt.y - cy) < cellH * 0.4f) return i
        }
        return -1
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaleDetector.onTouchEvent(event)
        gesture.onTouchEvent(event)
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> { lastPanX = event.x; lastPanY = event.y; panning = false }
            MotionEvent.ACTION_MOVE -> {
                if (draggingIndex >= 0) {
                    val dx = (event.x - lastPanX) / scale
                    val dy = (event.y - lastPanY) / scale
                    val prev = suitOffsets[draggingIndex] ?: PointF(0f, 0f)
                    suitOffsets[draggingIndex] = PointF(prev.x + dx, prev.y + dy)
                    lastPanX = event.x; lastPanY = event.y; invalidate(); return true
                }
                if (!scaleDetector.isInProgress) {
                    val dx = event.x - lastPanX; val dy = event.y - lastPanY
                    if (!panning && dx * dx + dy * dy > touchSlop * touchSlop) {
                        panning = true; parent.requestDisallowInterceptTouchEvent(true)
                    }
                    if (panning) {
                        offsetX += dx; offsetY += dy
                        lastPanX = event.x; lastPanY = event.y; invalidate()
                    }
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                draggingIndex = -1; panning = false
                parent.requestDisallowInterceptTouchEvent(false); invalidate()
            }
        }
        return true
    }
}
