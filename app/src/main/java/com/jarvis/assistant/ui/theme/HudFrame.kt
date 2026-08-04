package com.jarvis.assistant.ui.theme

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.min
import kotlin.random.Random

@Composable
fun HudPanel(
    modifier: Modifier = Modifier,
    borderColor: Color = HudCyanDim,
    fillColor: Color = HudCyanFaint.copy(alpha = 0.35f),
    cornerRadius: Dp = 10.dp,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(fillColor)
            .border(1.dp, borderColor, RoundedCornerShape(cornerRadius))
            .padding(16.dp)
    ) {
        content()
    }
}

@Composable
fun HudCore(
    modifier: Modifier = Modifier,
    size: Dp = 260.dp,
    pulsing: Boolean = true,
    onTap: () -> Unit = {}
) {
    val infiniteTransition = rememberInfiniteTransition(label = "core-pulse")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Canvas(
        modifier = modifier
            .size(size)
            .clickable(onClick = onTap)
    ) {
        val center = Offset(this.size.width / 2f, this.size.height / 2f)
        val maxRadius = min(this.size.width, this.size.height) / 2f

        drawCircle(
            color = HudCyanDim,
            radius = maxRadius,
            center = center,
            style = Stroke(width = 1.dp.toPx())
        )
        listOf(0.78f, 0.60f, 0.42f).forEach { fraction ->
            drawCircle(
                color = HudCyan.copy(alpha = 0.8f),
                radius = maxRadius * fraction,
                center = center,
                style = Stroke(
                    width = 3.dp.toPx(),
                    pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(
                        floatArrayOf(24f, 14f)
                    )
                )
            )
        }
        drawCircle(
            color = HudCyan,
            radius = maxRadius * 0.28f,
            center = center,
            style = Stroke(width = 2.dp.toPx())
        )
        val glowRadius = maxRadius * 0.16f * (if (pulsing) pulse else 1f)
        drawCircle(
            brush = androidx.compose.ui.graphics.Brush.radialGradient(
                colors = listOf(HudWhite, HudCyan, HudCyan.copy(alpha = 0f)),
                center = center,
                radius = glowRadius * 1.8f
            ),
            radius = glowRadius * 1.8f,
            center = center
        )
        drawCircle(color = HudWhite, radius = glowRadius, center = center)
    }
}

@Composable
fun HudWaveform(
    modifier: Modifier = Modifier,
    levels: List<Float> = List(28) { Random.nextFloat() * 0.6f + 0.15f },
    barColor: Color = HudCyan
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(20.dp),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        levels.forEach { level ->
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height((20.dp.value * level.coerceIn(0.1f, 1f)).dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(barColor)
            )
        }
    }
}
