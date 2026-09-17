package com.jarvis.ai.ui.suits

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jarvis.ai.controller.ArmorController
import com.jarvis.ai.data.model.ArmorSuit
import com.jarvis.ai.data.repository.SuitRepository
import com.jarvis.ai.ui.hud.HudColors
import com.jarvis.ai.ui.hud.HudCornerFrame
import com.jarvis.ai.ui.hud.HudGridBackground
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/* ───────── Liquid Glass / Mark-VII palette ───────── */
private val Cyan = Color(0xFF00D9FF)
private val CyanBright = Color(0xFFB8ECFF)
private val CyanCore = Color(0xFFE8FBFF)
private val CyanDim = Color(0xFF007A99)
private val CyanSoft = Color(0xFF5A8A99)
private val Amber = Color(0xFFFFAA00)
private val BgDeep = Color(0xFF01040A)
private val Bg = Color(0xFF03080E)
private val Glass = Color(0x2200D9FF)
private val GlassBorder = Color(0x5500D9FF)
private val PanelDark = Color(0xCC050E16)

/**
 * Armor Suit window — redesigned as a full Mark-VII diagnostic HUD
 * with liquid-glass translucent panels and central Iron Man silhouette.
 * Keeps existing suit selection + ArmorController integration.
 */
@Composable
fun ArmorSuitWindow(onClose: (() -> Unit)? = null) {
    val context = LocalContext.current
    val currentSuit by ArmorController.currentSuit.collectAsState()
    val suitList = remember { SuitRepository().getAllSuits() }
    val glow by animateColorAsState(
        targetValue = Color(currentSuit.arcReactorColor),
        animationSpec = tween(700),
        label = "reactorGlow"
    )
    val primary = Color(currentSuit.primaryColor)
    val vectorId = remember(currentSuit.vectorResName) {
        context.resources.getIdentifier(
            currentSuit.vectorResName, "drawable", context.packageName
        )
    }

    val infinite = rememberInfiniteTransition(label = "armorHud")
    val pulse by infinite.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(2200, easing = LinearEasing), RepeatMode.Reverse),
        label = "pulse"
    )
    val slowSpin by infinite.animateFloat(
        0f, 360f,
        infiniteRepeatable(tween(28000, easing = LinearEasing)),
        label = "slow"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(listOf(BgDeep, Bg, BgDeep))
            )
    ) {
        HudGridBackground(modifier = Modifier.fillMaxSize(), alpha = 0.14f)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp, vertical = 8.dp)
        ) {
            // ── Top status bar ──
            LiquidTopBar(
                suitName = currentSuit.name,
                mark = currentSuit.mark.name.replace('_', ' '),
                glow = glow
            )

            Spacer(Modifier.height(8.dp))

            // ── Main diagnostic area ──
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left glass panel
                LiquidSidePanel(
                    modifier = Modifier
                        .width(78.dp)
                        .fillMaxHeight()
                        .padding(end = 6.dp),
                    glow = glow
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(6.dp)
                    ) {
                        Text("SUIT", color = glow, fontSize = 8.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                        Text("DIAG", color = CyanSoft, fontSize = 7.sp, fontFamily = FontFamily.Monospace)
                        Spacer(Modifier.height(8.dp))
                        MiniGauge("PWR", 0.92f, glow)
                        Spacer(Modifier.height(6.dp))
                        MiniGauge("REP", 0.78f, glow)
                        Spacer(Modifier.height(6.dp))
                        MiniGauge("ARM", 0.85f, Amber)
                        Spacer(Modifier.height(6.dp))
                        MiniGauge("SYS", 0.96f, glow)
                        Spacer(Modifier.weight(1f))
                        Text(
                            currentSuit.systemMode.take(8),
                            color = CyanSoft,
                            fontSize = 7.sp,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Center — Iron Man + Arc Reactor
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.Center
                ) {
                    // Outer rotating ring
                    Canvas(Modifier.fillMaxSize()) {
                        val cx = size.width / 2f
                        val cy = size.height / 2f
                        val r = min(cx, cy) * 0.92f
                        drawCircle(
                            glow.copy(alpha = 0.08f + 0.04f * pulse),
                            r * 1.05f,
                            Offset(cx, cy)
                        )
                        drawCircle(
                            glow.copy(alpha = 0.25f),
                            r,
                            Offset(cx, cy),
                            style = Stroke(width = 1.5f)
                        )
                        // dashed outer
                        val dashCount = 48
                        for (i in 0 until dashCount) {
                            val a = Math.toRadians((i * 360.0 / dashCount) + slowSpin.toDouble())
                            val inner = r * 0.96f
                            val outer = r * 1.02f
                            drawLine(
                                glow.copy(alpha = if (i % 4 == 0) 0.7f else 0.25f),
                                Offset(cx + (inner * cos(a)).toFloat(), cy + (inner * sin(a)).toFloat()),
                                Offset(cx + (outer * cos(a)).toFloat(), cy + (outer * sin(a)).toFloat()),
                                strokeWidth = if (i % 4 == 0) 2f else 1f,
                                cap = StrokeCap.Round
                            )
                        }
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(contentAlignment = Alignment.Center) {
                            // Soft glow behind suit
                            Box(
                                modifier = Modifier
                                    .size(190.dp)
                                    .background(
                                        Brush.radialGradient(
                                            listOf(
                                                glow.copy(alpha = 0.22f * pulse),
                                                glow.copy(alpha = 0.06f),
                                                Color.Transparent
                                            )
                                        ),
                                        CircleShape
                                    )
                            )
                            if (vectorId != 0) {
                                Image(
                                    painter = painterResource(id = vectorId),
                                    contentDescription = currentSuit.name,
                                    modifier = Modifier
                                        .height(168.dp)
                                        .wrapContentSize()
                                )
                            }
                            // Arc reactor core overlay
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .align(Alignment.Center)
                                    .background(
                                        Brush.radialGradient(
                                            listOf(CyanCore, glow, glow.copy(alpha = 0.3f))
                                        ),
                                        CircleShape
                                    )
                                    .border(1.5.dp, CyanBright.copy(alpha = 0.8f), CircleShape)
                            )
                        }

                        Spacer(Modifier.height(6.dp))
                        Text(
                            currentSuit.name.uppercase(),
                            color = primary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            "MARK ${currentSuit.mark.name.replace('_', ' ')}  ·  ONLINE",
                            color = glow,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Current power level is at 100 percent and holding steady.",
                            color = CyanSoft,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                    }
                }

                // Right glass panel
                LiquidSidePanel(
                    modifier = Modifier
                        .width(78.dp)
                        .fillMaxHeight()
                        .padding(start = 6.dp),
                    glow = glow
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(6.dp)
                    ) {
                        Text("ENERGY", color = glow, fontSize = 8.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                        Text("MATRIX", color = CyanSoft, fontSize = 7.sp, fontFamily = FontFamily.Monospace)
                        Spacer(Modifier.height(8.dp))
                        CircularMiniGauge(0.88f, glow)
                        Spacer(Modifier.height(8.dp))
                        StatusRow("CPU", "34%", glow)
                        StatusRow("RAM", "61%", glow)
                        StatusRow("NET", "OK", glow)
                        StatusRow("GPS", "LOCK", glow)
                        Spacer(Modifier.weight(1f))
                        Text("v3.0.1", color = CyanSoft, fontSize = 7.sp, fontFamily = FontFamily.Monospace)
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // ── Bottom status + suit selector ──
            LiquidGlassCard(modifier = Modifier.fillMaxWidth(), glow = glow) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "SELECT MARK",
                            color = glow,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.sp
                        )
                        Text(
                            "${suitList.size} SUITS AVAILABLE",
                            color = CyanSoft,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(horizontal = 2.dp)
                    ) {
                        items(suitList, key = { it.id }) { suit ->
                            SuitChipLiquid(
                                suit = suit,
                                selected = suit.id == currentSuit.id,
                                onClick = { ArmorController.equipSuit(suit) }
                            )
                        }
                    }
                }
            }

            if (onClose != null) {
                Spacer(Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(PanelDark)
                        .border(1.dp, GlassBorder, RoundedCornerShape(12.dp))
                        .clickable { onClose() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "CLOSE ARCHIVE",
                        color = glow,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                }
            }
        }

        HudCornerFrame(modifier = Modifier.fillMaxSize(), accent = glow.copy(alpha = 0.7f))
    }
}

/* ───────── Liquid Glass components ───────── */

@Composable
private fun LiquidTopBar(suitName: String, mark: String, glow: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(
                Brush.verticalGradient(listOf(Color(0xCC0A1825), Color(0xCC050E16)))
            )
            .border(1.dp, glow.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(glow.copy(alpha = 0.15f))
                .border(1.dp, glow.copy(alpha = 0.6f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text("🛡", fontSize = 16.sp)
        }
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "STARK INDUSTRIES  ·  ARMOR ARCHIVE",
                color = glow,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 0.8.sp
            )
            Text(
                "$suitName  ·  $mark",
                color = CyanSoft,
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Text(
            "ONLINE",
            color = glow,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
private fun LiquidSidePanel(
    modifier: Modifier = Modifier,
    glow: Color,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(PanelDark)
            .border(1.dp, glow.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
    ) {
        content()
    }
}

@Composable
private fun LiquidGlassCard(
    modifier: Modifier = Modifier,
    glow: Color,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(PanelDark)
            .border(1.dp, glow.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
    ) {
        content()
    }
}

@Composable
private fun MiniGauge(label: String, value: Float, accent: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .width(12.dp)
                .height(42.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(Color(0xFF050E16))
                .border(0.5.dp, accent.copy(alpha = 0.5f), RoundedCornerShape(3.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(value.coerceIn(0f, 1f))
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            listOf(accent.copy(alpha = 0.9f), accent.copy(alpha = 0.35f))
                        )
                    )
            )
        }
        Spacer(Modifier.height(2.dp))
        Text(label, color = accent, fontSize = 7.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun CircularMiniGauge(value: Float, accent: Color) {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(48.dp)) {
        Canvas(Modifier.size(48.dp)) {
            val stroke = 4.dp.toPx()
            val r = (size.minDimension - stroke) / 2f
            drawCircle(
                accent.copy(alpha = 0.2f),
                r,
                style = Stroke(stroke)
            )
            drawArc(
                color = accent,
                startAngle = -90f,
                sweepAngle = 360f * value.coerceIn(0f, 1f),
                useCenter = false,
                style = Stroke(stroke, cap = StrokeCap.Round)
            )
        }
        Text(
            "${(value * 100).toInt()}%",
            color = accent,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
private fun StatusRow(label: String, value: String, accent: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = CyanSoft, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
        Text(value, color = accent, fontSize = 8.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SuitChipLiquid(suit: ArmorSuit, selected: Boolean, onClick: () -> Unit) {
    val context = LocalContext.current
    val id = remember(suit.vectorResName) {
        context.resources.getIdentifier(suit.vectorResName, "drawable", context.packageName)
    }
    val r = Color(suit.arcReactorColor)

    Box(
        modifier = Modifier
            .width(92.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) r.copy(alpha = 0.18f) else PanelDark)
            .border(
                1.dp,
                if (selected) r.copy(alpha = 0.85f) else Color(0x33FFFFFF),
                RoundedCornerShape(12.dp)
            )
            .clickable { onClick() }
            .padding(8.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (id != 0) {
                Image(
                    painter = painterResource(id = id),
                    contentDescription = suit.name,
                    modifier = Modifier.height(48.dp)
                )
            } else {
                Box(modifier = Modifier.size(12.dp).background(r, CircleShape))
            }
            Spacer(Modifier.height(4.dp))
            Text(
                suit.mark.name.replace('_', ' '),
                color = if (selected) Color.White else Color(0xFF8B9CAB),
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
