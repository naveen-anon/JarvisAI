package com.jarvis.assistant.armor

import android.graphics.*

/**
 * Iron Man 3 Hall-of-Armor style silhouettes.
 * Reads as armored suits (helmet faceplate + plates + RT), not stick robots.
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
        val isHolo = style == Style.HOLO
        val line = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.style = Paint.Style.STROKE
            strokeJoin = Paint.Join.ROUND
            strokeCap = Paint.Cap.ROUND
            strokeWidth = if (selected) 2.6f else 2.1f
            color = when {
                isHolo && selected -> Color.parseColor("#E8FFFF")
                isHolo -> Color.parseColor("#00E5FF")
                else -> secondary
            }
        }
        val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.style = Paint.Style.FILL
            color = if (isHolo) Color.parseColor("#1400E5FF") else primary
        }
        val plate = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.style = Paint.Style.FILL
            color = if (isHolo) Color.parseColor("#1A00E5FF") else Color.argb(
                255,
                (Color.red(primary) * 0.75f).toInt().coerceIn(0, 255),
                (Color.green(primary) * 0.75f).toInt().coerceIn(0, 255),
                (Color.blue(primary) * 0.75f).toInt().coerceIn(0, 255)
            )
        }
        val core = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.style = Paint.Style.FILL
            color = if (isHolo) Color.parseColor("#F0FFFF") else reactor
        }
        val trim = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.style = Paint.Style.STROKE
            strokeWidth = 1.5f
            color = if (isHolo) Color.parseColor("#66F0FF") else secondary
        }

        when (family(mark)) {
            Family.MARK1 -> suit(canvas, cx, cy, h, line, fill, plate, core, trim, bulk = 1.2f, boxHead = true, crude = true)
            Family.SLIM -> suit(canvas, cx, cy, h, line, fill, plate, core, trim, bulk = 0.82f, slim = true)
            Family.STEALTH -> suit(canvas, cx, cy, h, line, fill, plate, core, trim, bulk = 0.9f, angular = true)
            Family.HEAVY -> suit(canvas, cx, cy, h, line, fill, plate, core, trim, bulk = 1.35f, boxHead = true, heavy = true)
            Family.CLAW -> suit(canvas, cx, cy, h, line, fill, plate, core, trim, bulk = 1.05f, claws = true)
            Family.CENTURION -> suit(canvas, cx, cy, h, line, fill, plate, core, trim, bulk = 1.15f, fins = true)
            Family.PACK -> suit(canvas, cx, cy, h, line, fill, plate, core, trim, bulk = 1.0f, pack = true)
            Family.BONES -> bones(canvas, cx, cy, h, line, core)
            Family.HAMMER -> suit(canvas, cx, cy, h, line, fill, plate, core, trim, bulk = 1.25f, boxHead = true, bigHelm = true)
            Family.NANO -> suit(canvas, cx, cy, h, line, fill, plate, core, trim, bulk = 0.98f, triangleRt = true, nano = true)
            Family.CLASSIC -> suit(canvas, cx, cy, h, line, fill, plate, core, trim, bulk = 1.0f, triangleRt = mark.number in listOf(6, 7, 42))
        }
    }

    private enum class Family {
        MARK1, CLASSIC, SLIM, STEALTH, HEAVY, CLAW, CENTURION, PACK, BONES, NANO, HAMMER
    }

    private fun family(m: ArmorMark): Family {
        val n = m.number
        val c = m.codename.lowercase()
        return when {
            n == 1 -> Family.MARK1
            n >= 50 || n == 85 || c.contains("nano") -> Family.NANO
            n == 35 || c.contains("snapper") -> Family.CLAW
            n == 33 || n == 30 || c.contains("centurion") -> Family.CENTURION
            n == 39 || c.contains("starboost") || c.contains("gemini") -> Family.PACK
            n == 41 || c.contains("bones") -> Family.BONES
            n == 37 || c.contains("hammer") -> Family.HAMMER
            n == 38 || n == 24 || n == 25 || c.contains("igor") || c.contains("tank") ||
                c.contains("striker") || n in 34..36 -> Family.HEAVY
            n == 15 || n == 16 || c.contains("sneaky") || c.contains("nightclub") -> Family.STEALTH
            n in 12..14 || n == 29 -> Family.SLIM
            else -> Family.CLASSIC
        }
    }

    private fun suit(
        canvas: Canvas, cx: Float, cy: Float, h: Float,
        line: Paint, fill: Paint, plate: Paint, core: Paint, trim: Paint,
        bulk: Float = 1f,
        boxHead: Boolean = false,
        bigHelm: Boolean = false,
        crude: Boolean = false,
        slim: Boolean = false,
        angular: Boolean = false,
        heavy: Boolean = false,
        claws: Boolean = false,
        fins: Boolean = false,
        pack: Boolean = false,
        triangleRt: Boolean = false,
        nano: Boolean = false
    ) {
        val headR = h * (if (bigHelm) 0.145f else if (slim) 0.105f else 0.12f) * (if (heavy) 1.05f else 1f)
        val headCy = cy - h * 0.72f
        val shoulderY = cy - h * 0.52f
        val shoulderW = h * 0.56f * bulk
        val chestY = cy - h * 0.32f
        val waistY = cy - h * 0.02f
        val waistW = h * (if (slim) 0.24f else 0.32f) * bulk
        val hipY = cy + h * 0.06f
        val kneeY = cy + h * 0.38f
        val footY = cy + h * 0.72f

        val helm = Path()
        if (boxHead) {
            helm.addRoundRect(
                cx - headR * 1.05f, headCy - headR * 0.95f,
                cx + headR * 1.05f, headCy + headR * 1.15f,
                headR * 0.2f, headR * 0.2f, Path.Direction.CW
            )
        } else {
            helm.moveTo(cx - headR * 0.85f, headCy - headR * 0.2f)
            helm.quadTo(cx - headR * 0.9f, headCy - headR * 1.05f, cx, headCy - headR * 1.1f)
            helm.quadTo(cx + headR * 0.9f, headCy - headR * 1.05f, cx + headR * 0.85f, headCy - headR * 0.2f)
            helm.lineTo(cx + headR * 0.7f, headCy + headR * 0.95f)
            helm.quadTo(cx, headCy + headR * 1.15f, cx - headR * 0.7f, headCy + headR * 0.95f)
            helm.close()
        }
        canvas.drawPath(helm, fill)
        canvas.drawPath(helm, line)
        val visorY = headCy + headR * 0.05f
        canvas.drawRoundRect(
            cx - headR * 0.62f, visorY - headR * 0.12f,
            cx + headR * 0.62f, visorY + headR * 0.18f,
            4f, 4f, line
        )
        canvas.drawLine(cx - headR * 0.48f, visorY, cx - headR * 0.12f, visorY, trim)
        canvas.drawLine(cx + headR * 0.12f, visorY, cx + headR * 0.48f, visorY, trim)

        val torso = Path()
        if (angular) {
            torso.moveTo(cx - shoulderW / 2, shoulderY)
            torso.lineTo(cx + shoulderW / 2, shoulderY)
            torso.lineTo(cx + waistW / 2, waistY)
            torso.lineTo(cx, hipY)
            torso.lineTo(cx - waistW / 2, waistY)
            torso.close()
        } else {
            torso.moveTo(cx - shoulderW / 2, shoulderY)
            torso.lineTo(cx + shoulderW / 2, shoulderY)
            torso.lineTo(cx + waistW / 2, waistY)
            torso.lineTo(cx - waistW / 2, waistY)
            torso.close()
        }
        canvas.drawPath(torso, fill)
        canvas.drawPath(torso, line)

        val chest = Path().apply {
            moveTo(cx - shoulderW * 0.28f, shoulderY + h * 0.04f)
            lineTo(cx + shoulderW * 0.28f, shoulderY + h * 0.04f)
            lineTo(cx + waistW * 0.28f, chestY + h * 0.08f)
            lineTo(cx - waistW * 0.28f, chestY + h * 0.08f)
            close()
        }
        canvas.drawPath(chest, plate)
        canvas.drawPath(chest, trim)

        if (triangleRt) {
            val r = h * 0.055f
            val tri = Path().apply {
                moveTo(cx, chestY - r)
                lineTo(cx + r * 0.95f, chestY + r * 0.7f)
                lineTo(cx - r * 0.95f, chestY + r * 0.7f)
                close()
            }
            canvas.drawPath(tri, line)
            canvas.drawCircle(cx, chestY + r * 0.15f, r * 0.28f, core)
        } else {
            val r = h * 0.05f
            canvas.drawCircle(cx, chestY, r * 1.15f, line)
            canvas.drawCircle(cx, chestY, r * 0.55f, core)
        }

        if (nano) {
            canvas.drawLine(cx - shoulderW * 0.3f, shoulderY + h * 0.1f, cx - waistW * 0.15f, waistY - 4f, trim)
            canvas.drawLine(cx + shoulderW * 0.3f, shoulderY + h * 0.1f, cx + waistW * 0.15f, waistY - 4f, trim)
        }
        if (crude) {
            canvas.drawLine(cx - waistW * 0.4f, chestY - h * 0.05f, cx + waistW * 0.4f, chestY - h * 0.05f, line)
            canvas.drawLine(cx - waistW * 0.4f, chestY + h * 0.08f, cx + waistW * 0.4f, chestY + h * 0.08f, line)
        }

        canvas.drawRoundRect(cx - waistW * 0.55f, waistY - 3f, cx + waistW * 0.55f, waistY + 10f, 3f, 3f, line)

        val podR = h * 0.07f * bulk
        canvas.drawCircle(cx - shoulderW / 2, shoulderY + 2f, podR, fill)
        canvas.drawCircle(cx + shoulderW / 2, shoulderY + 2f, podR, fill)
        canvas.drawCircle(cx - shoulderW / 2, shoulderY + 2f, podR, line)
        canvas.drawCircle(cx + shoulderW / 2, shoulderY + 2f, podR, line)
        if (fins) {
            canvas.drawLine(cx - shoulderW / 2, shoulderY, cx - shoulderW / 2 - h * 0.12f, shoulderY - h * 0.1f, line)
            canvas.drawLine(cx + shoulderW / 2, shoulderY, cx + shoulderW / 2 + h * 0.12f, shoulderY - h * 0.1f, line)
        }
        if (pack) {
            canvas.drawRoundRect(cx - h * 0.16f, shoulderY - h * 0.12f, cx - h * 0.05f, shoulderY + h * 0.14f, 5f, 5f, line)
            canvas.drawRoundRect(cx + h * 0.05f, shoulderY - h * 0.12f, cx + h * 0.16f, shoulderY + h * 0.14f, 5f, 5f, line)
        }
        if (heavy) {
            canvas.drawRect(cx - shoulderW / 2 - h * 0.05f, shoulderY - h * 0.08f, cx - shoulderW / 2 + h * 0.05f, shoulderY + h * 0.1f, line)
            canvas.drawRect(cx + shoulderW / 2 - h * 0.05f, shoulderY - h * 0.08f, cx + shoulderW / 2 + h * 0.05f, shoulderY + h * 0.1f, line)
        }

        val armOut = h * (if (heavy) 0.11f else 0.08f)
        val handY = waistY + h * 0.06f
        val armW = h * (if (heavy) 0.055f else 0.04f)
        fun armoredArm(side: Float) {
            val sx = cx + side * (shoulderW / 2)
            val hx = cx + side * (shoulderW / 2 + armOut)
            val path = Path().apply {
                moveTo(sx - side * armW * 0.2f, shoulderY + podR * 0.5f)
                lineTo(sx + side * armW, shoulderY + podR * 0.3f)
                lineTo(hx + side * armW * 0.8f, handY)
                lineTo(hx - side * armW * 0.5f, handY + h * 0.02f)
                close()
            }
            canvas.drawPath(path, fill)
            canvas.drawPath(path, line)
            if (claws) {
                canvas.drawLine(hx, handY, hx + side * h * 0.07f, handY + h * 0.1f, line)
                canvas.drawLine(hx, handY, hx + side * h * 0.02f, handY + h * 0.12f, line)
            } else {
                canvas.drawCircle(hx, handY, armW * 1.1f, line)
            }
        }
        armoredArm(-1f)
        armoredArm(1f)

        val spread = waistW * (if (heavy) 0.42f else if (slim) 0.28f else 0.35f)
        fun armoredLeg(side: Float) {
            val topX = cx + side * spread
            val botX = cx + side * spread * 0.9f
            val leg = Path().apply {
                moveTo(topX - side * h * 0.03f, hipY)
                lineTo(topX + side * h * 0.04f, hipY)
                lineTo(botX + side * h * 0.035f, footY - h * 0.04f)
                lineTo(botX - side * h * 0.04f, footY - h * 0.04f)
                close()
            }
            canvas.drawPath(leg, fill)
            canvas.drawPath(leg, line)
            canvas.drawCircle(botX * 0.3f + topX * 0.7f, kneeY, h * 0.03f, line)
            val bootW = h * (if (heavy) 0.1f else 0.07f)
            canvas.drawRoundRect(
                botX - bootW * 0.6f, footY - h * 0.05f,
                botX + bootW * 0.55f, footY + h * 0.02f,
                3f, 3f, line
            )
        }
        armoredLeg(-1f)
        armoredLeg(1f)
    }

    private fun bones(canvas: Canvas, cx: Float, cy: Float, h: Float, line: Paint, core: Paint) {
        val headR = h * 0.1f
        val headCy = cy - h * 0.72f
        val shoulderY = cy - h * 0.52f
        val shoulderW = h * 0.46f
        val waistY = cy - h * 0.02f
        val chestY = cy - h * 0.32f
        val footY = cy + h * 0.72f
        canvas.drawCircle(cx, headCy, headR, line)
        canvas.drawLine(cx - headR * 0.5f, headCy, cx + headR * 0.5f, headCy, line)
        canvas.drawLine(cx - shoulderW / 2, shoulderY, cx + shoulderW / 2, shoulderY, line)
        canvas.drawLine(cx, shoulderY, cx, waistY, line)
        for (i in 1..4) {
            val yy = shoulderY + (waistY - shoulderY) * i / 5f
            val w = shoulderW * (0.55f - i * 0.05f)
            canvas.drawLine(cx - w / 2, yy, cx + w / 2, yy, line)
        }
        canvas.drawCircle(cx, chestY, h * 0.035f, core)
        canvas.drawCircle(cx, chestY, h * 0.045f, line)
        canvas.drawLine(cx - shoulderW / 2, shoulderY, cx - shoulderW / 2 - h * 0.06f, waistY, line)
        canvas.drawLine(cx + shoulderW / 2, shoulderY, cx + shoulderW / 2 + h * 0.06f, waistY, line)
        canvas.drawLine(cx - h * 0.06f, waistY, cx - h * 0.05f, footY, line)
        canvas.drawLine(cx + h * 0.06f, waistY, cx + h * 0.05f, footY, line)
    }
}
