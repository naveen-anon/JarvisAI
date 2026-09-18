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
import androidx.compose.foundation.lazy.LazyColumn
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

/* ───────── Orange Armor Hall palette (mockup style) ───────── */
private val Cyan = Color(0xFF00D9FF)
private val CyanBright = Color(0xFFB8ECFF)
private val CyanCore = Color(0xFFE8FBFF)
private val CyanDim = Color(0xFF007A99)
private val CyanSoft = Color(0xFF5A8A99)
private val Amber = Color(0xFFFFAA00)
private val Orange = Color(0xFFFF8C00)
private val OrangeBright = Color(0xFFFFB040)
private val OrangeSoft = Color(0xFFCC7A20)
private val BgDeep = Color(0xFF01040A)
private val Bg = Color(0xFF03080E)
private val Glass = Color(0x22FF8C00)
private val GlassBorder = Color(0x55FF8C00)
private val PanelDark = Color(0xCC0A0E14)

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

            // ── Armor Hall main (mockup style) ──
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Left – suit stats
                Column(
                    modifier = Modifier
                        .width(100.dp)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(14.dp))
                        .background(PanelDark)
                        .border(1.dp, Orange.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                        .padding(8.dp)
                ) {
                    Text(
                        currentSuit.mark.name.replace('_', ' '),
                        color = OrangeBright,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(Modifier.height(8.dp))
                    StatRowOrange("POWER", "100%")
                    StatRowOrange("ARMOR", "100%")
                    StatRowOrange("ENERGY", "100%")
                    StatRowOrange("STABILITY", "100%")
                    Spacer(Modifier.weight(1f))
                    Text(
                        "\"Sometimes you gotta\nrun before you can walk.\"",
                        color = OrangeSoft,
                        fontSize = 8.sp,
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 11.sp
                    )
                }

                // Center – full body suit
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(16.dp))
                        .background(PanelDark)
                        .border(1.dp, Orange.copy(alpha = 0.30f), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    // Soft glow under suit
                    Box(
                        Modifier
                            .size(140.dp)
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 24.dp)
                            .background(
                                Brush.radialGradient(
                                    listOf(Orange.copy(alpha = 0.25f), Color.Transparent)
                                ),
                                CircleShape
                            )
                    )
                    if (vectorId != 0) {
                        Image(
                            painter = painterResource(id = vectorId),
                            contentDescription = currentSuit.name,
                            modifier = Modifier
                                .fillMaxHeight(0.85f)
                                .padding(8.dp)
                        )
                    } else {
                        Text(
                            currentSuit.name,
                            color = OrangeBright,
                            fontSize = 14.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                // Right – mark list
                Column(
                    modifier = Modifier
                        .width(96.dp)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(14.dp))
                        .background(PanelDark)
                        .border(1.dp, Orange.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                        .padding(6.dp)
                ) {
                    Text(
                        "SELECT",
                        color = OrangeBright,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(suitList, key = { it.id }) { suit ->
                            val selected = suit.id == currentSuit.id
                            val r = Color(suit.arcReactorColor)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (selected) Orange.copy(alpha = 0.2f) else Color(0x22000000))
                                    .border(
                                        1.dp,
                                        if (selected) OrangeBright else Color(0x33FFFFFF),
                                        RoundedCornerShape(10.dp)
                                    )
                                    .clickable { ArmorController.equipSuit(suit) }
                                    .padding(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    Modifier
                                        .size(8.dp)
                                        .background(if (selected) OrangeBright else r, CircleShape)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    suit.mark.name.replace('_', ' ').take(10),
                                    color = if (selected) Color.White else OrangeSoft,
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

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
                "ARMOR HALL  ·  SELECT YOUR SUIT",
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
private fun StatRowOrange(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = OrangeSoft, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
        Text(value, color = OrangeBright, fontSize = 9.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
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
