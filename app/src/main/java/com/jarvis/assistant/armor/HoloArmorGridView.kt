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
 * Interactive Iron Man 3–style holographic armor archive.
 * Cyan blueprint silhouettes on a dark grid — pan / pinch-zoom / drag suits.
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

    // Camera
    private var scale = 1f
    private var offsetX = 0f
    private var offsetY = 0f
    private val minScale = 0.55f
    private val maxScale = 2.8f

    // Drag single suit
    private var draggingIndex = -1
    private var dragX = 0f
    private var dragY = 0f
    private val suitOffsets = HashMap<Int, PointF>()

    private val cols = 7
    private val cellW = 120f
    private val cellH = 160f
    private val pad = 28f

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#020B14") }
    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#0A3A4A")
        strokeWidth = 1f
        style = Paint.Style.STROKE
    }
    private val framePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#C41E3A")
        strokeWidth = 4f
        style = Paint.Style.STROKE
    }
    private val cyanPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#00E5FF")
        strokeWidth = 2.2f
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
    }
    private val cyanFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#3300E5FF")
        style = Paint.Style.FILL
    }
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#00E5FF")
        style = Paint.Style.STROKE
        strokeWidth = 3.5f
        maskFilter = BlurMaskFilter(12f, BlurMaskFilter.Blur.OUTER)
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#7AD4E8")
        textAlign = Paint.Align.CENTER
        textSize = 18f
        typeface = Typeface.MONOSPACE
    }
    private val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#00E5FF")
        textAlign = Paint.Align.CENTER
        textSize = 28f
        typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        letterSpacing = 0.12f
    }
    private val selectPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#4000E5FF")
        style = Paint.Style.FILL
    }
    private val reactorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#E0FFFF")
        style = Paint.Style.FILL
    }

    private val scaleDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            val old = scale
            scale = (scale * detector.scaleFactor).coerceIn(minScale, maxScale)
            // zoom around focus
            val fx = detector.focusX
            val fy = detector.focusY
            offsetX = fx - (fx - offsetX) * (scale / old)
            offsetY = fy - (fy - offsetY) * (scale / old)
            invalidate()
            return true
        }
    })

    private val gesture = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onDown(e: MotionEvent): Boolean = true
        override fun onSingleTapUp(e: MotionEvent): Boolean {
            val idx = hitTest(e.x, e.y)
            if (idx >= 0) {
                selectedIndex = idx
                listener?.onSuitSelected(marks[idx])
                invalidate()
                return true
            }
            return false
        }
        override fun onDoubleTap(e: MotionEvent): Boolean {
            val idx = hitTest(e.x, e.y)
            if (idx >= 0) {
                selectedIndex = idx
                listener?.onSuitActivated(marks[idx])
                return true
            }
            // reset camera
            scale = 1f
            offsetX = 0f
            offsetY = 0f
            invalidate()
            return true
        }
        override fun onLongPress(e: MotionEvent) {
            val idx = hitTest(e.x, e.y)
            if (idx >= 0) {
                draggingIndex = idx
                dragX = e.x
                dragY = e.y
                parent.requestDisallowInterceptTouchEvent(true)
                invalidate()
            }
        }
    })

    private var lastPanX = 0f
    private var lastPanY = 0f
    private var panning = false
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop

    init {
        setLayerType(LAYER_TYPE_SOFTWARE, null) // for BlurMaskFilter
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        // Center content initially
        val contentW = cols * cellW + pad * 2
        val rows = (marks.size + cols - 1) / cols
        val contentH = rows * cellH + pad * 2 + 80f
        offsetX = (w - contentW * scale) / 2f
        offsetY = (h - contentH * scale) / 2f + 20f
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        canvas.drawRect(0f, 0f, w, h, bgPaint)

        // Background grid (screen space)
        gridPaint.strokeWidth = 1f
        var x = (offsetX % (40f * scale))
        while (x < w) {
            canvas.drawLine(x, 0f, x, h, gridPaint)
            x += 40f * scale
        }
        var y = (offsetY % (40f * scale))
        while (y < h) {
            canvas.drawLine(0f, y, w, y, gridPaint)
            y += 40f * scale
        }

        canvas.save()
        canvas.translate(offsetX, offsetY)
        canvas.scale(scale, scale)

        // Outer holoprojector frame
        val rows = (marks.size + cols - 1) / cols
        val contentW = cols * cellW + pad * 2
        val contentH = rows * cellH + pad * 2 + 50f
        framePaint.strokeWidth = 3f
        canvas.drawRoundRect(8f, 8f, contentW - 8f, contentH - 8f, 18f, 18f, framePaint)
        // inner cyan frame
        cyanPaint.strokeWidth = 1.5f
        canvas.drawRoundRect(16f, 16f, contentW - 16f, contentH - 16f, 14f, 14f, cyanPaint)

        canvas.drawText("IRON MAN 3  ·  HALL OF ARMOR", contentW / 2f, 42f, titlePaint)

        marks.forEachIndexed { i, mark ->
            val col = i % cols
            val row = i / cols
            var cx = pad + col * cellW + cellW / 2f
            var cy = pad + 55f + row * cellH + cellH / 2f

            suitOffsets[i]?.let {
                cx += it.x
                cy += it.y
            }
            if (i == draggingIndex) {
                // convert screen drag delta into content space roughly
                cx += (dragX - lastPanX) / scale
                cy += (dragY - lastPanY) / scale
            }

            if (i == selectedIndex) {
                canvas.drawRoundRect(
                    cx - cellW * 0.42f, cy - cellH * 0.42f,
                    cx + cellW * 0.42f, cy + cellH * 0.42f,
                    12f, 12f, selectPaint
                )
                glowPaint.strokeWidth = 2.5f
                canvas.drawRoundRect(
                    cx - cellW * 0.42f, cy - cellH * 0.42f,
                    cx + cellW * 0.42f, cy + cellH * 0.42f,
                    12f, 12f, glowPaint
                )
            }

            drawBlueprintSuit(canvas, cx, cy, cellH * 0.38f, i == selectedIndex)
            textPaint.textSize = 11f
            textPaint.color = if (i == selectedIndex) Color.parseColor("#E0FFFF") else Color.parseColor("#5AA8B8")
            canvas.drawText(mark.roman, cx, cy + cellH * 0.40f, textPaint)
        }

        canvas.restore()

        // HUD footer
        textPaint.textSize = 13f
        textPaint.color = Color.parseColor("#7AD4E8")
        canvas.drawText("PINCH zoom  ·  DRAG pan  ·  LONG-PRESS move suit  ·  TAP select", w / 2f, h - 24f, textPaint)
        if (selectedIndex in marks.indices) {
            titlePaint.textSize = 16f
            canvas.drawText(
                "MARK ${marks[selectedIndex].roman}  ·  ${marks[selectedIndex].codename}",
                w / 2f, h - 48f, titlePaint
            )
        }
    }

    /** Wireframe Iron Man blueprint figure */
    private fun drawBlueprintSuit(canvas: Canvas, cx: Float, cy: Float, h: Float, selected: Boolean) {
        val p = if (selected) glowPaint else cyanPaint
        p.strokeWidth = if (selected) 2.8f else 2.0f
        val fill = cyanFill

        val headR = h * 0.12f
        val headCy = cy - h * 0.72f
        // head
        canvas.drawCircle(cx, headCy, headR, p)
        // visor
        canvas.drawLine(cx - headR * 0.55f, headCy, cx + headR * 0.55f, headCy, p)
        // torso
        val shoulderY = cy - h * 0.52f
        val shoulderW = h * 0.55f
        val waistY = cy - h * 0.05f
        val waistW = h * 0.32f
        val torso = Path().apply {
            moveTo(cx - shoulderW / 2, shoulderY)
            lineTo(cx + shoulderW / 2, shoulderY)
            lineTo(cx + waistW / 2, waistY)
            lineTo(cx - waistW / 2, waistY)
            close()
        }
        canvas.drawPath(torso, fill)
        canvas.drawPath(torso, p)
        // arc reactor
        canvas.drawCircle(cx, cy - h * 0.32f, h * 0.06f, p)
        reactorPaint.alpha = if (selected) 230 else 160
        canvas.drawCircle(cx, cy - h * 0.32f, h * 0.03f, reactorPaint)
        // arms
        canvas.drawLine(cx - shoulderW / 2, shoulderY + 4f, cx - shoulderW / 2 - h * 0.08f, waistY - h * 0.05f, p)
        canvas.drawLine(cx + shoulderW / 2, shoulderY + 4f, cx + shoulderW / 2 + h * 0.08f, waistY - h * 0.05f, p)
        // shoulders pods
        canvas.drawCircle(cx - shoulderW / 2, shoulderY, h * 0.07f, p)
        canvas.drawCircle(cx + shoulderW / 2, shoulderY, h * 0.07f, p)
        // legs
        val hipY = waistY + 4f
        val footY = cy + h * 0.70f
        canvas.drawLine(cx - waistW * 0.28f, hipY, cx - waistW * 0.22f, footY, p)
        canvas.drawLine(cx + waistW * 0.28f, hipY, cx + waistW * 0.22f, footY, p)
        // boots
        canvas.drawLine(cx - waistW * 0.32f, footY, cx - waistW * 0.12f, footY, p)
        canvas.drawLine(cx + waistW * 0.12f, footY, cx + waistW * 0.32f, footY, p)
    }

    private fun contentPoint(screenX: Float, screenY: Float): PointF {
        return PointF((screenX - offsetX) / scale, (screenY - offsetY) / scale)
    }

    private fun hitTest(screenX: Float, screenY: Float): Int {
        val pt = contentPoint(screenX, screenY)
        marks.forEachIndexed { i, _ ->
            val col = i % cols
            val row = i / cols
            var cx = pad + col * cellW + cellW / 2f
            var cy = pad + 55f + row * cellH + cellH / 2f
            suitOffsets[i]?.let {
                cx += it.x
                cy += it.y
            }
            if (kotlin.math.abs(pt.x - cx) < cellW * 0.4f && kotlin.math.abs(pt.y - cy) < cellH * 0.4f) {
                return i
            }
        }
        return -1
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaleDetector.onTouchEvent(event)
        gesture.onTouchEvent(event)

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                lastPanX = event.x
                lastPanY = event.y
                panning = false
            }
            MotionEvent.ACTION_MOVE -> {
                if (draggingIndex >= 0) {
                    val dx = (event.x - lastPanX) / scale
                    val dy = (event.y - lastPanY) / scale
                    val prev = suitOffsets[draggingIndex] ?: PointF(0f, 0f)
                    suitOffsets[draggingIndex] = PointF(prev.x + dx, prev.y + dy)
                    lastPanX = event.x
                    lastPanY = event.y
                    dragX = event.x
                    dragY = event.y
                    invalidate()
                    return true
                }
                if (!scaleDetector.isInProgress) {
                    val dx = event.x - lastPanX
                    val dy = event.y - lastPanY
                    if (!panning && (dx * dx + dy * dy) > touchSlop * touchSlop) {
                        panning = true
                        parent.requestDisallowInterceptTouchEvent(true)
                    }
                    if (panning) {
                        offsetX += dx
                        offsetY += dy
                        lastPanX = event.x
                        lastPanY = event.y
                        invalidate()
                    }
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                draggingIndex = -1
                panning = false
                parent.requestDisallowInterceptTouchEvent(false)
                invalidate()
            }
        }
        return true
    }

    fun selectMarkNumber(number: Int) {
        val idx = marks.indexOfFirst { it.number == number }
        if (idx >= 0) {
            selectedIndex = idx
            invalidate()
        }
    }
}
