package com.jarvis.ai.ui.hud

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.random.Random

/**
 * Shared J.A.R.V.I.S HUD design language for Compose screens — the cyan
 * corner-bracket / grid / radar look already used natively (CornerFrameView,
 * HudOverlayView, ArcReactorView) across MainActivity, SettingsActivity etc.
 * Use these on any Compose screen (e.g. the Armor Suit window) so it matches
 * the rest of the app instead of drifting into its own style.
 */
object HudColors {
    val Accent = Color(0xFF00E5FF)
    val AccentDim = Color(0xFF0B7A94)
    val Background = Color(0xFF020810)
    val PanelTop = Color(0xFF0D1117)
    val PanelBottom = Color(0xFF071018)
    val TextDim = Color(0xFF7AB8C8)
    val TextBright = Color(0xFFB8ECFF)
}

/** Faint scanning grid + drifting particles, matching HudOverlayView. */
@Composable
fun HudGridBackground(modifier: Modifier = Modifier, alpha: Float = 0.22f) {
    val infinite = rememberInfiniteTransition(label = "hudGridTime")
    val time by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 4000f,
        animationSpec = infiniteRepeatable(tween(60000, easing = LinearEasing)),
        label = "hudGridDrift"
    )
    val particles = remember {
        List(18) { Triple(Random.nextFloat(), Random.nextFloat(), 0.3f + Random.nextFloat() * 0.7f) }
    }
    Canvas(modifier = modifier) {
        val step = 80.dp.toPx()
        var x = 0f
        while (x < size.width) {
            drawLine(
                HudColors.AccentDim.copy(alpha = 0.16f * alpha / 0.22f),
                Offset(x, 0f),
                Offset(x, size.height),
                strokeWidth = 1f
            )
            x += step
        }
        var y = 0f
        while (y < size.height) {
            drawLine(
                HudColors.AccentDim.copy(alpha = 0.16f * alpha / 0.22f),
                Offset(0f, y),
                Offset(size.width, y),
                strokeWidth = 1f
            )
            y += step
        }
        particles.forEach { (px, py, speed) ->
            val travelled = (py * size.height - time * speed).let { raw ->
                val h = size.height
                ((raw % h) + h) % h
            }
            drawCircle(
                HudColors.Accent.copy(alpha = 0.35f * alpha / 0.22f),
                radius = 1.6.dp.toPx(),
                center = Offset(px * size.width, travelled)
            )
        }
    }
}

/** Angular corner brackets + circuit-trace edge ticks, matching CornerFrameView. */
@Composable
fun HudCornerFrame(
    modifier: Modifier = Modifier,
    accent: Color = HudColors.Accent,
    accentDim: Color = HudColors.AccentDim
) {
    Canvas(modifier = modifier) {
        val bracket = 30.dp.toPx()
        val inset = 5.dp.toPx()
        val strokeW = 2.6.dp.toPx()
        val w = size.width
        val h = size.height

        fun corner(cx: Float, cy: Float, dirX: Float, dirY: Float) {
            drawLine(accent, Offset(cx, cy), Offset(cx + bracket * dirX, cy), strokeWidth = strokeW, cap = StrokeCap.Round)
            drawLine(accent, Offset(cx, cy), Offset(cx, cy + bracket * dirY), strokeWidth = strokeW, cap = StrokeCap.Round)
            drawCircle(accent, radius = 2.2.dp.toPx(), center = Offset(cx, cy))
        }
        corner(inset, inset, 1f, 1f)
        corner(w - inset, inset, -1f, 1f)
        corner(inset, h - inset, 1f, -1f)
        corner(w - inset, h - inset, -1f, -1f)

        // circuit-trace ticks along the top edge
        var x = 46.dp.toPx()
        var toggle = true
        val maxX = w - 46.dp.toPx()
        while (x < maxX) {
            val seg = if (toggle) 11.dp.toPx() else 5.dp.toPx()
            drawLine(accentDim, Offset(x, 1.5.dp.toPx()), Offset(x + seg, 1.5.dp.toPx()), strokeWidth = 1.8.dp.toPx())
            x += seg + 6.dp.toPx()
            toggle = !toggle
        }
    }
}

/** Targeting-reticle style arc-reactor core, matching ArcReactorView / bg_reactor_*. */
@Composable
fun ReactorRadar(modifier: Modifier = Modifier, glow: Color = HudColors.Accent, dim: Dp = 160.dp) {
    val infinite = rememberInfiniteTransition(label = "reactorRadar")
    val pulse by infinite.animateFloat(
        initialValue = 0.82f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1400, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "reactorPulse"
    )
    val spin by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(16000, easing = LinearEasing)),
        label = "reactorSpin"
    )
    Canvas(modifier = modifier.size(dim)) {
        val radius = size.minDimension / 2f
        val c = center

        drawCircle(glow.copy(alpha = 0.12f), radius = radius * 0.98f, center = c, style = Stroke(1.dp.toPx()))

        rotate(spin, pivot = c) {
            drawCircle(
                glow.copy(alpha = 0.55f),
                radius = radius * 0.76f,
                center = c,
                style = Stroke(width = 1.4.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f)))
            )
        }
        drawCircle(glow.copy(alpha = 0.32f), radius = radius * 0.54f, center = c, style = Stroke(1.2.dp.toPx()))

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color.White, glow, glow.copy(alpha = 0f)),
                center = c,
                radius = radius * 0.36f * pulse
            ),
            radius = radius * 0.36f * pulse,
            center = c
        )

        val tick = radius * 0.08f
        listOf(0f, 90f, 180f, 270f).forEach { angle ->
            rotate(angle, pivot = c) {
                drawLine(
                    glow.copy(alpha = 0.5f),
                    Offset(c.x, c.y - radius * 0.97f),
                    Offset(c.x, c.y - radius * 0.97f + tick),
                    strokeWidth = 1.6.dp.toPx()
                )
            }
        }
    }
}

/** Boot-console log lines that reveal one at a time, like the J.A.R.V.I.S splash. */
@Composable
fun HudBootLog(lines: List<String>, modifier: Modifier = Modifier, color: Color = HudColors.Accent) {
    var revealed by remember(lines) { mutableStateOf(0) }
    LaunchedEffect(lines) {
        revealed = 0
        while (revealed < lines.size) {
            delay(260)
            revealed++
        }
    }
    Column(modifier = modifier) {
        lines.take(revealed).forEach { line ->
            androidx.compose.material3.Text(
                text = line,
                color = color,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                lineHeight = 14.sp
            )
        }
    }
}

/** Thin glowing progress bar with a caption label, like the splash's boot bar. */
@Composable
fun HudProgressBar(label: String, modifier: Modifier = Modifier, color: Color = HudColors.Accent) {
    val infinite = rememberInfiniteTransition(label = "hudProgress")
    val progress by infinite.animateFloat(
        initialValue = 0.18f,
        targetValue = 0.94f,
        animationSpec = infiniteRepeatable(tween(2200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "hudProgressVal"
    )
    Column(modifier = modifier) {
        androidx.compose.material3.Text(
            text = label,
            color = color.copy(alpha = 0.85f),
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Canvas(modifier = Modifier.fillMaxWidth().height(5.dp)) {
            drawRoundRect(
                color = color.copy(alpha = 0.15f),
                cornerRadius = CornerRadius(3.dp.toPx())
            )
            drawRoundRect(
                color = color,
                size = Size(size.width * progress, size.height),
                cornerRadius = CornerRadius(3.dp.toPx())
            )
        }
    }
}

/**
 * Full HUD-framed panel: dark gradient body, cyan border, faint grid and
 * corner-bracket overlay — drop any screen content inside to make it look
 * like the rest of Jarvis (used by ArmorSuitWindow; reuse anywhere else
 * you want the same window chrome).
 */
@Composable
fun HudPanel(
    modifier: Modifier = Modifier,
    accent: Color = HudColors.Accent,
    cornerRadius: Dp = 6.dp,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .background(
                Brush.verticalGradient(listOf(HudColors.PanelTop, HudColors.PanelBottom)),
                RoundedCornerShape(cornerRadius)
            )
            .border(1.dp, accent.copy(alpha = 0.55f), RoundedCornerShape(cornerRadius))
    ) {
        HudGridBackground(modifier = Modifier.matchParentSize(), alpha = 0.35f)
        HudCornerFrame(modifier = Modifier.matchParentSize(), accent = accent)
        content()
    }
}
