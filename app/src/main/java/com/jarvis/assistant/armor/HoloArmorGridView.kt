package com.jarvis.assistant.armor

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.view.ViewConfiguration

/** Interactive Hall of Armor — each mark uses a unique blueprint silhouette. */
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
    private var draggingIndex = -1
    private val suitOffsets = HashMap<Int, PointF>()
    private val cols = 7
    private val cellW = 118f
    private val cellH = 158f
    private val pad = 28f

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#01060C") }
    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#0A2A38"); strokeWidth = 1f; style = Paint.Style.STROKE
    }
    private val framePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#00D9FF"); strokeWidth = 2.5f; style = Paint.Style.STROKE
    }
    private val cyanStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#00E5FF"); strokeWidth = 1.2f; style = Paint.Style.STROKE
    }
    private val cellBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#0A1520"); style = Paint.Style.FILL
    }
    private val cellBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#1A4A5A"); strokeWidth = 1.5f; style = Paint.Style.STROKE
    }
    private val selectPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#2800E5FF"); style = Paint.Style.FILL
    }
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#00E5FF"); style = Paint.Style.STROKE; strokeWidth = 2.8f
        maskFilter = BlurMaskFilter(12f, BlurMaskFilter.Blur.OUTER)
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#7AD4E8"); textAlign = Paint.Align.CENTER
        textSize = 11f; typeface = Typeface.MONOSPACE
    }
    private val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#00E5FF"); textAlign = Paint.Align.CENTER
        textSize = 22f; typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
    }
    private val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#5AA8B8"); textAlign = Paint.Align.CENTER
        textSize = 11f; typeface = Typeface.MONOSPACE
    }

    private val scaleDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            val old = scale
            scale = (scale * detector.scaleFactor).coerceIn(0.5f, 3f)
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
            if (idx >= 0) { draggingIndex = idx; lastPanX = e.x; lastPanY = e.y; parent.requestDisallowInterceptTouchEvent(true) }
        }
    })
    private var lastPanX = 0f; private var lastPanY = 0f; private var panning = false
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
        canvas.drawRoundRect(14f, 14f, contentW - 14f, contentH - 14f, 12f, 12f, cyanStroke)

        // Title bar
        titlePaint.textSize = 20f
        canvas.drawText("IRON MAN", contentW / 2f, 36f, titlePaint)
        subtitlePaint.textSize = 10f
        canvas.drawText("ALL ARMOR  ·  STARK INDUSTRIES", contentW / 2f, 52f, subtitlePaint)

        marks.forEachIndexed { i, mark ->
            val col = i % cols; val row = i / cols
            var cx = pad + col * cellW + cellW / 2f
            var cy = pad + 64f + row * cellH + cellH / 2f
            suitOffsets[i]?.let { cx += it.x; cy += it.y }

            val left = cx - cellW * 0.44f
            val top = cy - cellH * 0.44f
            val right = cx + cellW * 0.44f
            val bottom = cy + cellH * 0.44f

            // Cell background card
            canvas.drawRoundRect(left, top, right, bottom, 10f, 10f, cellBgPaint)
            canvas.drawRoundRect(left, top, right, bottom, 10f, 10f, cellBorderPaint)

            if (i == selectedIndex) {
                canvas.drawRoundRect(left, top, right, bottom, 10f, 10f, selectPaint)
                canvas.drawRoundRect(left, top, right, bottom, 10f, 10f, glowPaint)
            }

            ArmorBlueprintDrawer.draw(
                canvas, cx, cy - 6f, cellH * 0.36f, mark,
                ArmorBlueprintDrawer.Style.HOLO, selected = i == selectedIndex
            )
            textPaint.color = if (i == selectedIndex) Color.parseColor("#E0FFFF") else Color.parseColor("#5AA8B8")
            textPaint.textSize = 10f
            canvas.drawText("MARK ${mark.roman}", cx, cy + cellH * 0.38f, textPaint)
        }
        canvas.restore()
        textPaint.textSize = 11f; textPaint.color = Color.parseColor("#5AA8B8")
        canvas.drawText("TAP select  ·  DOUBLE-TAP open  ·  PINCH zoom", w / 2f, h - 18f, textPaint)
        if (selectedIndex in marks.indices) {
            titlePaint.textSize = 13f
            val m = marks[selectedIndex]
            canvas.drawText("MARK ${m.roman}  ·  ${m.codename}", w / 2f, h - 38f, titlePaint)
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
        scaleDetector.onTouchEvent(event); gesture.onTouchEvent(event)
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> { lastPanX = event.x; lastPanY = event.y; panning = false }
            MotionEvent.ACTION_MOVE -> {
                if (draggingIndex >= 0) {
                    val dx = (event.x - lastPanX) / scale; val dy = (event.y - lastPanY) / scale
                    val prev = suitOffsets[draggingIndex] ?: PointF(0f, 0f)
                    suitOffsets[draggingIndex] = PointF(prev.x + dx, prev.y + dy)
                    lastPanX = event.x; lastPanY = event.y; invalidate(); return true
                }
                if (!scaleDetector.isInProgress) {
                    val dx = event.x - lastPanX; val dy = event.y - lastPanY
                    if (!panning && dx * dx + dy * dy > touchSlop * touchSlop) {
                        panning = true; parent.requestDisallowInterceptTouchEvent(true)
                    }
                    if (panning) { offsetX += dx; offsetY += dy; lastPanX = event.x; lastPanY = event.y; invalidate() }
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                draggingIndex = -1; panning = false; parent.requestDisallowInterceptTouchEvent(false); invalidate()
            }
        }
        return true
    }
}
