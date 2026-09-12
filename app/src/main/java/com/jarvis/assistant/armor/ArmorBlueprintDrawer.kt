package com.jarvis.assistant.armor

import android.graphics.*
import kotlin.math.min

/**
 * Distinct MCU / Iron Man 3 House Party Protocol silhouettes.
 * One drawer for Hall of Armor grid + detail preview — no identical stick figures.
 */
object ArmorBlueprintDrawer {

    enum class Style { HOLO, METAL }

    fun draw(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        height: Float,
        mark: ArmorMark,
        style: Style,
        selected: Boolean = false,
        primary: Int = Color.parseColor("#C41E3A"),
        secondary: Int = Color.parseColor("#F5C518"),
        reactor: Int = Color.parseColor("#00E5FF")
    ) {
        val h = height
        val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.style = Paint.Style.STROKE
            strokeJoin = Paint.Join.ROUND
            strokeCap = Paint.Cap.ROUND
            strokeWidth = if (selected) 2.8f else 2.0f
            color = when (style) {
                Style.HOLO -> if (selected) Color.parseColor("#E0FFFF") else Color.parseColor("#00E5FF")
                Style.METAL -> secondary
            }
        }
        val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.style = Paint.Style.FILL
            color = when (style) {
                Style.HOLO -> Color.parseColor("#1800E5FF")
                Style.METAL -> primary
            }
        }
        val core = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.style = Paint.Style.FILL
            color = if (style == Style.HOLO) Color.parseColor("#E8FFFF") else reactor
        }
        val accent = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.style = Paint.Style.STROKE
            strokeWidth = 1.6f
            color = if (style == Style.HOLO) Color.parseColor("#66E5FF") else secondary
        }

        when (shapeId(mark)) {
            Shape.MARK1 -> drawMark1(canvas, cx, cy, h, stroke, fill, core)
            Shape.SLIM -> drawSlim(canvas, cx, cy, h, stroke, fill, core)
            Shape.STEALTH -> drawStealth(canvas, cx, cy, h, stroke, fill, core)
            Shape.HEAVY -> drawHeavy(canvas, cx, cy, h, stroke, fill, core)
            Shape.CLAW -> drawClaw(canvas, cx, cy, h, stroke, fill, core)
            Shape.CENTURION -> drawCenturion(canvas, cx, cy, h, stroke, fill, core)
            Shape.PACK -> drawPack(canvas, cx, cy, h, stroke, fill, core)
            Shape.BONES -> drawBones(canvas, cx, cy, h, stroke, fill, core)
            Shape.NANO -> drawNano(canvas, cx, cy, h, stroke, fill, core, accent)
            Shape.HAMMER -> drawHammer(canvas, cx, cy, h, stroke, fill, core)
            else -> drawClassic(canvas, cx, cy, h, stroke, fill, core, accent, mark.number)
        }
    }

    private enum class Shape {
        MARK1, CLASSIC, SLIM, STEALTH, HEAVY, CLAW, CENTURION, PACK, BONES, NANO, HAMMER
    }

    private fun shapeId(m: ArmorMark): Shape {
        val n = m.number
        val c = m.codename.lowercase()
        return when {
            n == 1 -> Shape.MARK1
            n == 85 || n >= 50 || c.contains("nano") -> Shape.NANO
            c.contains("snapper") || n == 35 -> Shape.CLAW
            c.contains("centurion") || n == 33 || n == 30 -> Shape.CENTURION
            c.contains("starboost") || c.contains("gemini") || n == 39 -> Shape.PACK
            c.contains("bones") || n == 41 -> Shape.BONES
            c.contains("hammer") || n == 37 -> Shape.HAMMER
            c.contains("igor") || c.contains("tank") || c.contains("striker") ||
                c.contains("thumper") || n == 38 || n == 24 || n == 25 || n in 34..36 -> Shape.HEAVY
            c.contains("sneaky") || c.contains("nightclub") || n == 15 || n == 16 -> Shape.STEALTH
            n in 12..14 || n == 29 -> Shape.SLIM
            else -> Shape.CLASSIC
        }
    }

    private fun head(canvas: Canvas, cx: Float, cy: Float, r: Float, stroke: Paint, fill: Paint, boxy: Boolean = false) {
        if (boxy) {
            canvas.drawRoundRect(cx - r, cy - r, cx + r, cy + r * 1.05f, r * 0.25f, r * 0.25f, fill)
            canvas.drawRoundRect(cx - r, cy - r, cx + r, cy + r * 1.05f, r * 0.25f, r * 0.25f, stroke)
        } else {
            canvas.drawCircle(cx, cy, r, fill)
            canvas.drawCircle(cx, cy, r, stroke)
        }
        // visor slit
        canvas.drawLine(cx - r * 0.55f, cy + r * 0.05f, cx + r * 0.55f, cy + r * 0.05f, stroke)
    }

    private fun reactorCircle(canvas: Canvas, cx: Float, cy: Float, r: Float, stroke: Paint, core: Paint) {
        canvas.drawCircle(cx, cy, r, stroke)
        canvas.drawCircle(cx, cy, r * 0.45f, core)
    }

    private fun reactorTriangle(canvas: Canvas, cx: Float, cy: Float, r: Float, stroke: Paint, core: Paint) {
        val t = Path().apply {
            moveTo(cx, cy - r)
            lineTo(cx + r * 0.9f, cy + r * 0.65f)
            lineTo(cx - r * 0.9f, cy + r * 0.65f)
            close()
        }
        canvas.drawPath(t, stroke)
        canvas.drawCircle(cx, cy + r * 0.1f, r * 0.25f, core)
    }

    private fun classicTorso(
        canvas: Canvas, cx: Float, shoulderY: Float, shoulderW: Float,
        waistY: Float, waistW: Float, stroke: Paint, fill: Paint
    ) {
        val p = Path().apply {
            moveTo(cx - shoulderW / 2, shoulderY)
            lineTo(cx + shoulderW / 2, shoulderY)
            lineTo(cx + waistW / 2, waistY)
            lineTo(cx - waistW / 2, waistY)
            close()
        }
        canvas.drawPath(p, fill)
        canvas.drawPath(p, stroke)
    }

    private fun arms(
        canvas: Canvas, cx: Float, shoulderY: Float, shoulderW: Float,
        handY: Float, out: Float, stroke: Paint, thick: Boolean = false
    ) {
        canvas.drawLine(cx - shoulderW / 2, shoulderY + 2f, cx - shoulderW / 2 - out, handY, stroke)
        canvas.drawLine(cx + shoulderW / 2, shoulderY + 2f, cx + shoulderW / 2 + out, handY, stroke)
        if (thick) {
            canvas.drawCircle(cx - shoulderW / 2 - out, handY, 5f, stroke)
            canvas.drawCircle(cx + shoulderW / 2 + out, handY, 5f, stroke)
        }
        canvas.drawCircle(cx - shoulderW / 2, shoulderY, 6f, stroke)
        canvas.drawCircle(cx + shoulderW / 2, shoulderY, 6f, stroke)
    }

    private fun legs(
        canvas: Canvas, cx: Float, waistY: Float, footY: Float,
        spread: Float, stroke: Paint, boot: Float = 10f
    ) {
        canvas.drawLine(cx - spread, waistY, cx - spread * 0.9f, footY, stroke)
        canvas.drawLine(cx + spread, waistY, cx + spread * 0.9f, footY, stroke)
        canvas.drawLine(cx - spread * 0.9f - boot * 0.4f, footY, cx - spread * 0.9f + boot * 0.5f, footY, stroke)
        canvas.drawLine(cx + spread * 0.9f - boot * 0.5f, footY, cx + spread * 0.9f + boot * 0.4f, footY, stroke)
    }

    private fun drawClassic(
        canvas: Canvas, cx: Float, cy: Float, h: Float,
        stroke: Paint, fill: Paint, core: Paint, accent: Paint, number: Int
    ) {
        val headR = h * 0.12f
        val headCy = cy - h * 0.70f
        val shoulderY = cy - h * 0.50f
        val shoulderW = h * 0.54f
        val waistY = cy - h * 0.02f
        val waistW = h * 0.30f
        val chestY = cy - h * 0.30f
        val footY = cy + h * 0.68f
        head(canvas, cx, headCy, headR, stroke, fill)
        classicTorso(canvas, cx, shoulderY, shoulderW, waistY, waistW, stroke, fill)
        if (number in listOf(6, 7, 42)) reactorTriangle(canvas, cx, chestY, h * 0.06f, stroke, core)
        else reactorCircle(canvas, cx, chestY, h * 0.055f, stroke, core)
        // chest gold lines
        canvas.drawLine(cx - waistW * 0.35f, shoulderY + h * 0.08f, cx - waistW * 0.2f, waistY - h * 0.05f, accent)
        canvas.drawLine(cx + waistW * 0.35f, shoulderY + h * 0.08f, cx + waistW * 0.2f, waistY - h * 0.05f, accent)
        arms(canvas, cx, shoulderY, shoulderW, waistY + h * 0.02f, h * 0.08f, stroke)
        legs(canvas, cx, waistY, footY, waistW * 0.28f, stroke)
    }

    private fun drawMark1(
        canvas: Canvas, cx: Float, cy: Float, h: Float,
        stroke: Paint, fill: Paint, core: Paint
    ) {
        val headR = h * 0.13f
        val headCy = cy - h * 0.68f
        val shoulderY = cy - h * 0.48f
        val shoulderW = h * 0.62f
        val waistY = cy + h * 0.02f
        val waistW = h * 0.38f
        val chestY = cy - h * 0.26f
        val footY = cy + h * 0.70f
        head(canvas, cx, headCy, headR, stroke, fill, boxy = true)
        classicTorso(canvas, cx, shoulderY, shoulderW, waistY, waistW, stroke, fill)
        // crude plate bars
        canvas.drawLine(cx - waistW * 0.4f, chestY - h * 0.06f, cx + waistW * 0.4f, chestY - h * 0.06f, stroke)
        canvas.drawLine(cx - waistW * 0.4f, chestY + h * 0.05f, cx + waistW * 0.4f, chestY + h * 0.05f, stroke)
        reactorCircle(canvas, cx, chestY, h * 0.05f, stroke, core)
        arms(canvas, cx, shoulderY, shoulderW, waistY + h * 0.05f, h * 0.1f, stroke, thick = true)
        legs(canvas, cx, waistY, footY, waistW * 0.35f, stroke, boot = 14f)
    }

    private fun drawSlim(
        canvas: Canvas, cx: Float, cy: Float, h: Float,
        stroke: Paint, fill: Paint, core: Paint
    ) {
        val headR = h * 0.10f
        val headCy = cy - h * 0.72f
        val shoulderY = cy - h * 0.52f
        val shoulderW = h * 0.40f
        val waistY = cy - h * 0.04f
        val waistW = h * 0.22f
        val chestY = cy - h * 0.32f
        val footY = cy + h * 0.70f
        head(canvas, cx, headCy, headR, stroke, fill)
        classicTorso(canvas, cx, shoulderY, shoulderW, waistY, waistW, stroke, fill)
        reactorCircle(canvas, cx, chestY, h * 0.04f, stroke, core)
        arms(canvas, cx, shoulderY, shoulderW, waistY, h * 0.05f, stroke)
        legs(canvas, cx, waistY, footY, waistW * 0.22f, stroke, boot = 7f)
    }

    private fun drawStealth(
        canvas: Canvas, cx: Float, cy: Float, h: Float,
        stroke: Paint, fill: Paint, core: Paint
    ) {
        // angular stealth profile
        val headR = h * 0.11f
        val headCy = cy - h * 0.70f
        val shoulderY = cy - h * 0.50f
        val shoulderW = h * 0.48f
        val waistY = cy - h * 0.02f
        val waistW = h * 0.26f
        val chestY = cy - h * 0.30f
        val footY = cy + h * 0.68f
        head(canvas, cx, headCy, headR, stroke, fill)
        val torso = Path().apply {
            moveTo(cx - shoulderW / 2, shoulderY)
            lineTo(cx + shoulderW / 2, shoulderY)
            lineTo(cx + waistW / 2, waistY)
            lineTo(cx, waistY + h * 0.06f)
            lineTo(cx - waistW / 2, waistY)
            close()
        }
        canvas.drawPath(torso, fill)
        canvas.drawPath(torso, stroke)
        reactorCircle(canvas, cx, chestY, h * 0.045f, stroke, core)
        arms(canvas, cx, shoulderY, shoulderW, waistY, h * 0.06f, stroke)
        legs(canvas, cx, waistY + h * 0.04f, footY, waistW * 0.25f, stroke)
    }

    private fun drawHeavy(
        canvas: Canvas, cx: Float, cy: Float, h: Float,
        stroke: Paint, fill: Paint, core: Paint
    ) {
        val headR = h * 0.14f
        val headCy = cy - h * 0.66f
        val shoulderY = cy - h * 0.46f
        val shoulderW = h * 0.72f
        val waistY = cy + h * 0.04f
        val waistW = h * 0.42f
        val chestY = cy - h * 0.24f
        val footY = cy + h * 0.72f
        head(canvas, cx, headCy, headR, stroke, fill, boxy = true)
        classicTorso(canvas, cx, shoulderY, shoulderW, waistY, waistW, stroke, fill)
        reactorCircle(canvas, cx, chestY, h * 0.06f, stroke, core)
        // thick shoulder blocks
        canvas.drawRect(cx - shoulderW / 2 - h * 0.04f, shoulderY - h * 0.06f, cx - shoulderW / 2 + h * 0.06f, shoulderY + h * 0.08f, stroke)
        canvas.drawRect(cx + shoulderW / 2 - h * 0.06f, shoulderY - h * 0.06f, cx + shoulderW / 2 + h * 0.04f, shoulderY + h * 0.08f, stroke)
        arms(canvas, cx, shoulderY, shoulderW, waistY + h * 0.08f, h * 0.12f, stroke, thick = true)
        legs(canvas, cx, waistY, footY, waistW * 0.36f, stroke, boot = 16f)
    }

    private fun drawClaw(
        canvas: Canvas, cx: Float, cy: Float, h: Float,
        stroke: Paint, fill: Paint, core: Paint
    ) {
        val headR = h * 0.12f
        val headCy = cy - h * 0.68f
        val shoulderY = cy - h * 0.48f
        val shoulderW = h * 0.56f
        val waistY = cy
        val waistW = h * 0.32f
        val chestY = cy - h * 0.28f
        val footY = cy + h * 0.68f
        head(canvas, cx, headCy, headR, stroke, fill)
        classicTorso(canvas, cx, shoulderY, shoulderW, waistY, waistW, stroke, fill)
        reactorCircle(canvas, cx, chestY, h * 0.05f, stroke, core)
        val lx = cx - shoulderW / 2 - h * 0.08f
        val rx = cx + shoulderW / 2 + h * 0.08f
        val hy = waistY + h * 0.12f
        canvas.drawLine(cx - shoulderW / 2, shoulderY, lx, hy, stroke)
        canvas.drawLine(cx + shoulderW / 2, shoulderY, rx, hy, stroke)
        // pincers
        canvas.drawLine(lx, hy, lx - h * 0.07f, hy + h * 0.1f, stroke)
        canvas.drawLine(lx, hy, lx + h * 0.05f, hy + h * 0.11f, stroke)
        canvas.drawLine(rx, hy, rx + h * 0.07f, hy + h * 0.1f, stroke)
        canvas.drawLine(rx, hy, rx - h * 0.05f, hy + h * 0.11f, stroke)
        legs(canvas, cx, waistY, footY, waistW * 0.3f, stroke)
    }

    private fun drawCenturion(
        canvas: Canvas, cx: Float, cy: Float, h: Float,
        stroke: Paint, fill: Paint, core: Paint
    ) {
        val headR = h * 0.12f
        val headCy = cy - h * 0.70f
        val shoulderY = cy - h * 0.50f
        val shoulderW = h * 0.64f
        val waistY = cy - h * 0.02f
        val waistW = h * 0.30f
        val chestY = cy - h * 0.30f
        val footY = cy + h * 0.68f
        head(canvas, cx, headCy, headR, stroke, fill)
        classicTorso(canvas, cx, shoulderY, shoulderW, waistY, waistW, stroke, fill)
        reactorCircle(canvas, cx, chestY, h * 0.055f, stroke, core)
        // wide shoulder fins
        canvas.drawLine(cx - shoulderW / 2, shoulderY, cx - shoulderW / 2 - h * 0.1f, shoulderY - h * 0.08f, stroke)
        canvas.drawLine(cx + shoulderW / 2, shoulderY, cx + shoulderW / 2 + h * 0.1f, shoulderY - h * 0.08f, stroke)
        arms(canvas, cx, shoulderY, shoulderW, waistY, h * 0.09f, stroke)
        legs(canvas, cx, waistY, footY, waistW * 0.28f, stroke)
    }

    private fun drawPack(
        canvas: Canvas, cx: Float, cy: Float, h: Float,
        stroke: Paint, fill: Paint, core: Paint
    ) {
        val headR = h * 0.11f
        val headCy = cy - h * 0.70f
        val shoulderY = cy - h * 0.50f
        val shoulderW = h * 0.50f
        val waistY = cy - h * 0.02f
        val waistW = h * 0.28f
        val chestY = cy - h * 0.30f
        val footY = cy + h * 0.68f
        head(canvas, cx, headCy, headR, stroke, fill)
        classicTorso(canvas, cx, shoulderY, shoulderW, waistY, waistW, stroke, fill)
        reactorCircle(canvas, cx, chestY, h * 0.05f, stroke, core)
        // booster packs
        canvas.drawRoundRect(cx - h * 0.14f, shoulderY - h * 0.1f, cx - h * 0.04f, shoulderY + h * 0.12f, 4f, 4f, stroke)
        canvas.drawRoundRect(cx + h * 0.04f, shoulderY - h * 0.1f, cx + h * 0.14f, shoulderY + h * 0.12f, 4f, 4f, stroke)
        arms(canvas, cx, shoulderY, shoulderW, waistY, h * 0.06f, stroke)
        legs(canvas, cx, waistY, footY, waistW * 0.26f, stroke)
    }

    private fun drawBones(
        canvas: Canvas, cx: Float, cy: Float, h: Float,
        stroke: Paint, fill: Paint, core: Paint
    ) {
        val headR = h * 0.10f
        val headCy = cy - h * 0.72f
        val shoulderY = cy - h * 0.52f
        val shoulderW = h * 0.44f
        val waistY = cy - h * 0.04f
        val chestY = cy - h * 0.32f
        val footY = cy + h * 0.70f
        head(canvas, cx, headCy, headR, stroke, fill)
        // skeletal ribs instead of solid torso
        canvas.drawLine(cx - shoulderW / 2, shoulderY, cx + shoulderW / 2, shoulderY, stroke)
        for (i in 0..3) {
            val yy = shoulderY + (waistY - shoulderY) * (i + 1) / 5f
            val w = shoulderW * (0.5f - i * 0.05f)
            canvas.drawLine(cx - w / 2, yy, cx + w / 2, yy, stroke)
        }
        canvas.drawLine(cx, shoulderY, cx, waistY, stroke)
        reactorCircle(canvas, cx, chestY, h * 0.04f, stroke, core)
        arms(canvas, cx, shoulderY, shoulderW, waistY, h * 0.07f, stroke)
        legs(canvas, cx, waistY, footY, h * 0.08f, stroke, boot = 6f)
    }

    private fun drawNano(
        canvas: Canvas, cx: Float, cy: Float, h: Float,
        stroke: Paint, fill: Paint, core: Paint, accent: Paint
    ) {
        val headR = h * 0.115f
        val headCy = cy - h * 0.70f
        val shoulderY = cy - h * 0.50f
        val shoulderW = h * 0.52f
        val waistY = cy - h * 0.02f
        val waistW = h * 0.28f
        val chestY = cy - h * 0.30f
        val footY = cy + h * 0.68f
        head(canvas, cx, headCy, headR, stroke, fill)
        classicTorso(canvas, cx, shoulderY, shoulderW, waistY, waistW, stroke, fill)
        reactorTriangle(canvas, cx, chestY, h * 0.065f, stroke, core)
        // nano segment lines
        canvas.drawLine(cx - shoulderW * 0.35f, shoulderY + h * 0.1f, cx - waistW * 0.2f, waistY, accent)
        canvas.drawLine(cx + shoulderW * 0.35f, shoulderY + h * 0.1f, cx + waistW * 0.2f, waistY, accent)
        arms(canvas, cx, shoulderY, shoulderW, waistY, h * 0.07f, stroke)
        legs(canvas, cx, waistY, footY, waistW * 0.26f, stroke)
    }

    private fun drawHammer(
        canvas: Canvas, cx: Float, cy: Float, h: Float,
        stroke: Paint, fill: Paint, core: Paint
    ) {
        val headR = h * 0.15f
        val headCy = cy - h * 0.66f
        val shoulderY = cy - h * 0.46f
        val shoulderW = h * 0.60f
        val waistY = cy + h * 0.02f
        val waistW = h * 0.34f
        val chestY = cy - h * 0.26f
        val footY = cy + h * 0.70f
        // oversized helmet
        canvas.drawCircle(cx, headCy, headR, fill)
        canvas.drawCircle(cx, headCy, headR, stroke)
        canvas.drawLine(cx - headR * 0.7f, headCy, cx + headR * 0.7f, headCy, stroke)
        classicTorso(canvas, cx, shoulderY, shoulderW, waistY, waistW, stroke, fill)
        reactorCircle(canvas, cx, chestY, h * 0.05f, stroke, core)
        arms(canvas, cx, shoulderY, shoulderW, waistY + h * 0.05f, h * 0.09f, stroke, thick = true)
        legs(canvas, cx, waistY, footY, waistW * 0.32f, stroke, boot = 14f)
    }
}
