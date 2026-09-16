package com.jarvis.assistant.ui.compose

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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jarvis.assistant.R
import com.jarvis.assistant.ui.HudState
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

// Navy-cyan liquid glass palette (reference)
private val Cyan = Color(0xFF00E5FF)
private val CyanDim = Color(0xFF0A7A96)
private val CyanSoft = Color(0xFF7AB8C8)
private val Bg = Color(0xFF020810)
private val Glass = Color(0xB0051528)
private val GlassBorder = Color(0x9900E5FF)
private val PurpleEdge = Color(0xAA7B2FFF)

data class JarvisHudUiState(
    val stateLabel: String = "STANDING BY",
    val hudState: HudState = HudState.IDLE,
    val clock: String = "00:00:00",
    val battery: String = "BATT: —%",
    val network: String = "WiFi",
    val ram: String = "RAM: —%",
    val location: String = "—",
    val weather: String = "—",
    val response: String = "Online.",
    val waveformActive: Boolean = false
)

data class JarvisHudActions(
    val onTalk: () -> Unit = {},
    val onReactorTap: () -> Unit = {},
    val onChat: () -> Unit = {},
    val onSettings: () -> Unit = {},
    val onBriefing: () -> Unit = {},
    val onSystem: () -> Unit = {},
    val onVision: () -> Unit = {},
    val onArmor: () -> Unit = {},
    val onNavHome: () -> Unit = {},
    val onNavChat: () -> Unit = {},
    val onNavMic: () -> Unit = {},
    val onNavVision: () -> Unit = {},
    val onNavMore: () -> Unit = {}
)

@Composable
fun JarvisHudScreen(
    ui: JarvisHudUiState,
    actions: JarvisHudActions,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Bg)
    ) {
        // Circuit + grid overlay
        CircuitBackground(Modifier.fillMaxSize())

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            HeaderBar(
                clock = ui.clock,
                onChat = actions.onChat,
                onSettings = actions.onSettings
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = ui.stateLabel.uppercase(),
                color = Cyan,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.5.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.height(6.dp))
            StatusChips(ui.battery, ui.network, ui.ram)
            Spacer(Modifier.height(4.dp))
            LocationWeatherRow(ui.location, ui.weather)
            Spacer(Modifier.height(4.dp))

            // Mid: labels + reactor + AI badge
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SideLabels(Modifier.padding(end = 4.dp))
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable { actions.onReactorTap() },
                    contentAlignment = Alignment.Center
                ) {
                    ArcReactorCompose(
                        state = ui.hudState,
                        modifier = Modifier.size(200.dp)
                    )
                }
                AiBadge(Modifier.padding(start = 4.dp))
            }

            Text(
                text = ui.stateLabel.replaceFirstChar { it.lowercase() }.replace("...", ""),
                color = CyanDim,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            Spacer(Modifier.height(4.dp))
            WaveformBar(active = ui.waveformActive)
            Spacer(Modifier.height(8.dp))
            TalkButton(onClick = actions.onTalk)
            Spacer(Modifier.height(6.dp))
            ResponseBar(ui.response)
            Spacer(Modifier.height(5.dp))
            InputBarPlaceholder()
            Spacer(Modifier.height(6.dp))
            QuickActions(
                onBriefing = actions.onBriefing,
                onSystem = actions.onSystem,
                onVision = actions.onVision,
                onArmor = actions.onArmor
            )
            Spacer(Modifier.height(6.dp))
            BottomNav(
                onHome = actions.onNavHome,
                onChat = actions.onNavChat,
                onMic = actions.onNavMic,
                onVision = actions.onNavVision,
                onMore = actions.onNavMore
            )
        }
    }
}

@Composable
private fun CircuitBackground(modifier: Modifier = Modifier) {
    val infinite = rememberInfiniteTransition(label = "circuit")
    val t by infinite.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(8000, easing = LinearEasing)),
        label = "ct"
    )
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val c = CyanDim.copy(alpha = 0.18f)
        // grid
        val step = 48.dp.toPx()
        var x = 0f
        while (x < w) {
            drawLine(c, Offset(x, 0f), Offset(x, h), 1f)
            x += step
        }
        var y = 0f
        while (y < h) {
            drawLine(c, Offset(0f, y), Offset(w, y), 1f)
            y += step
        }
        // circuit traces near center
        val cx = w / 2f
        val cy = h * 0.38f
        val trace = Cyan.copy(alpha = 0.22f + 0.08f * t)
        drawLine(trace, Offset(cx - 140.dp.toPx(), cy), Offset(cx - 90.dp.toPx(), cy), 1.5f)
        drawLine(trace, Offset(cx + 90.dp.toPx(), cy), Offset(cx + 140.dp.toPx(), cy), 1.5f)
        drawLine(trace, Offset(cx - 120.dp.toPx(), cy - 40.dp.toPx()), Offset(cx - 120.dp.toPx(), cy + 40.dp.toPx()), 1.2f)
        drawLine(trace, Offset(cx + 120.dp.toPx(), cy - 40.dp.toPx()), Offset(cx + 120.dp.toPx(), cy + 40.dp.toPx()), 1.2f)
    }
}

@Composable
private fun GlassBox(
    modifier: Modifier = Modifier,
    corner: Int = 16,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(corner.dp))
            .background(
                Brush.verticalGradient(
                    listOf(Color(0x3500E5FF), Glass, Color(0xE0020810))
                )
            )
            .border(1.2.dp, GlassBorder, RoundedCornerShape(corner.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) { content() }
}

@Composable
private fun HeaderBar(clock: String, onChat: () -> Unit, onSettings: () -> Unit) {
    GlassBox(Modifier.fillMaxWidth(), corner = 14) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            // Robot avatar
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Color(0x2200E5FF))
                    .border(2.dp, Cyan, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painterResource(R.drawable.ic_robot),
                    contentDescription = null,
                    tint = Cyan,
                    modifier = Modifier.size(26.dp)
                )
            }
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    "J.A.R.V.I.S",
                    color = Cyan,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.2.sp,
                    maxLines = 1
                )
                Text(
                    "JUST · A · REAL · VIRTUAL · INTELLIGENT · SYSTEM",
                    color = CyanDim,
                    fontSize = 7.sp,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            GlassChip {
                Text(clock, color = Cyan, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            }
            Spacer(Modifier.width(4.dp))
            RoundIconBtn(R.drawable.ic_chat, onChat)
            Spacer(Modifier.width(4.dp))
            RoundIconBtn(R.drawable.ic_settings, onSettings)
        }
    }
}

@Composable
private fun GlassChip(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xCC061830))
            .border(1.dp, GlassBorder, RoundedCornerShape(14.dp))
            .padding(horizontal = 8.dp, vertical = 5.dp)
    ) { content() }
}

@Composable
private fun RoundIconBtn(iconRes: Int, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(Color(0xCC061830))
            .border(1.dp, GlassBorder, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(painterResource(iconRes), null, tint = Cyan, modifier = Modifier.size(16.dp))
    }
}

@Composable
private fun StatusChips(battery: String, network: String, ram: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        StatusChip(Modifier.weight(1f), R.drawable.ic_battery_bolt, battery)
        StatusChip(Modifier.weight(1f), R.drawable.ic_wifi, network)
        StatusChip(Modifier.weight(1f), R.drawable.ic_ram_chip, ram)
    }
}

@Composable
private fun StatusChip(modifier: Modifier, icon: Int, text: String) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xB0051525))
            .border(1.2.dp, GlassBorder, RoundedCornerShape(18.dp))
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(painterResource(icon), null, tint = Cyan, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(4.dp))
        Text(text, color = Cyan, fontSize = 10.sp, fontFamily = FontFamily.Monospace, maxLines = 1)
    }
}

@Composable
private fun LocationWeatherRow(loc: String, weather: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Icon(painterResource(R.drawable.ic_location_pin), null, tint = CyanSoft, modifier = Modifier.size(11.dp))
        Spacer(Modifier.width(3.dp))
        Text(loc, color = CyanSoft, fontSize = 10.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.weight(1f))
        Icon(painterResource(R.drawable.ic_cloud), null, tint = CyanSoft, modifier = Modifier.size(11.dp))
        Spacer(Modifier.width(3.dp))
        Text(weather, color = CyanSoft, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
    }
}

@Composable
private fun SideLabels(modifier: Modifier = Modifier) {
    Column(modifier) {
        listOf("ANALYZE", "ASSIST", "EXECUTE", "LEARN").forEach {
            Text(
                "☰ $it",
                color = CyanDim,
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(vertical = 3.dp)
            )
        }
    }
}

@Composable
private fun AiBadge(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .width(56.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xCC061830))
            .border(1.2.dp, GlassBorder, RoundedCornerShape(12.dp))
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("A.I.", color = Cyan, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
        Text("POWERED", color = CyanDim, fontSize = 7.sp, fontFamily = FontFamily.Monospace)
        Spacer(Modifier.height(4.dp))
        Icon(painterResource(R.drawable.ic_brain), null, tint = Cyan, modifier = Modifier.size(18.dp))
    }
}

@Composable
fun ArcReactorCompose(state: HudState, modifier: Modifier = Modifier) {
    val infinite = rememberInfiniteTransition(label = "reactor")
    val rot by infinite.animateFloat(
        0f, 360f,
        infiniteRepeatable(
            tween(
                when (state) {
                    HudState.LISTENING -> 4500
                    HudState.THINKING, HudState.EXECUTING -> 3200
                    HudState.SPEAKING -> 7000
                    else -> 18000
                },
                easing = LinearEasing
            )
        ),
        label = "rot"
    )
    val pulse by infinite.animateFloat(
        0f, 1f,
        infiniteRepeatable(
            tween(if (state == HudState.LISTENING) 700 else 2400),
            RepeatMode.Reverse
        ),
        label = "pulse"
    )
    val base = when (state) {
        HudState.THINKING, HudState.EXECUTING -> Color(0xFFFFB020)
        HudState.SPEAKING -> Color(0xFFB8F4FF)
        HudState.ERROR -> Color(0xFFFF5252)
        else -> Cyan
    }

    Canvas(modifier) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val r = min(cx, cy) * 0.92f
        val a = if (state == HudState.IDLE) 0.92f else 1f

        // glow disc
        drawCircle(base.copy(alpha = 0.15f * a), r * 1.05f, Offset(cx, cy))
        drawCircle(base.copy(alpha = 0.08f * a), r * 1.2f, Offset(cx, cy))

        // solid outer
        drawCircle(base.copy(alpha = 0.9f * a), r * 0.96f, Offset(cx, cy), style = Stroke(r * 0.04f))
        drawCircle(Color.White.copy(alpha = 0.35f * a), r * 0.93f, Offset(cx, cy), style = Stroke(r * 0.01f))

        fun dashedRing(radius: Float, dash: Float, gap: Float, width: Float, color: Color, angle: Float) {
            rotate(angle, Offset(cx, cy)) {
                drawCircle(
                    color,
                    radius,
                    Offset(cx, cy),
                    style = Stroke(
                        width = width,
                        cap = StrokeCap.Round,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(dash, gap))
                    )
                )
            }
        }

        dashedRing(r * 0.82f, r * 0.11f, r * 0.04f, r * 0.028f, base.copy(alpha = 0.85f * a), rot * 0.35f)
        dashedRing(r * 0.70f, r * 0.08f, r * 0.035f, r * 0.022f, base.copy(alpha = 0.75f * a), rot)
        dashedRing(r * 0.58f, r * 0.03f, r * 0.04f, r * 0.016f, Color(0xFF0090B8).copy(alpha = 0.6f * a), -rot * 0.5f)
        dashedRing(r * 0.46f, r * 0.05f, r * 0.025f, r * 0.024f, base.copy(alpha = 0.9f * a), rot * 1.6f)
        drawCircle(Color.White.copy(alpha = 0.5f * a), r * 0.36f, Offset(cx, cy), style = Stroke(r * 0.012f))

        // core
        val coreR = r * (0.20f + 0.03f * pulse)
        drawCircle(base.copy(alpha = 0.5f), coreR * 1.6f, Offset(cx, cy))
        drawCircle(Color(0xFFE8FDFF), coreR * 0.5f, Offset(cx, cy))
        drawCircle(Color.White, coreR * 0.2f, Offset(cx, cy))

        // triangle
        val triR = coreR * 1.2f
        val path = Path()
        for (i in 0..2) {
            val ang = Math.toRadians((-90 + i * 120).toDouble())
            val x = cx + (triR * cos(ang)).toFloat()
            val y = cy + (triR * sin(ang)).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        drawPath(path, base.copy(alpha = 0.95f * a), style = Stroke(r * 0.026f))
    }
}

@Composable
private fun WaveformBar(active: Boolean) {
    val infinite = rememberInfiniteTransition(label = "wf")
    val phase by infinite.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(if (active) 600 else 2000, easing = LinearEasing)),
        label = "wfp"
    )
    Canvas(
        Modifier
            .fillMaxWidth()
            .height(28.dp)
    ) {
        val n = 48
        val w = size.width / n
        for (i in 0 until n) {
            val mid = n / 2f
            val dist = kotlin.math.abs(i - mid) / mid
            val h = if (active) {
                size.height * (0.15f + 0.7f * (0.5f + 0.5f * sin((i * 0.4f + phase * 6.28f).toDouble()).toFloat()) * (1f - dist * 0.5f))
            } else {
                size.height * (0.12f + 0.08f * sin((i * 0.3f).toDouble()).toFloat())
            }
            val x = i * w + w / 2
            drawLine(
                Cyan.copy(alpha = 0.5f + 0.5f * (1f - dist)),
                Offset(x, size.height / 2 - h / 2),
                Offset(x, size.height / 2 + h / 2),
                strokeWidth = w * 0.45f,
                cap = StrokeCap.Round
            )
        }
    }
}

@Composable
private fun TalkButton(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(
                Brush.verticalGradient(listOf(Color(0x4000E5FF), Color(0xD0082030)))
            )
            .border(1.8.dp, Cyan, RoundedCornerShape(28.dp))
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(painterResource(R.drawable.ic_mic), null, tint = Cyan, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text("Talk", color = Cyan, fontSize = 16.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
    }
}

@Composable
private fun ResponseBar(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xCC061830))
            .border(1.dp, GlassBorder.copy(alpha = 0.6f), RoundedCornerShape(18.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(painterResource(R.drawable.ic_chat), null, tint = Cyan, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(8.dp))
        Text(text, color = Cyan, fontSize = 12.sp, fontFamily = FontFamily.Monospace, maxLines = 3)
    }
}

@Composable
private fun InputBarPlaceholder() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xCC041018))
            .border(1.dp, Color(0x4400E5FF), RoundedCornerShape(18.dp))
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(">>", color = CyanDim, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
        Spacer(Modifier.width(8.dp))
        Text("Type your message...", color = Color(0xFF4A6A7A), fontSize = 12.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.weight(1f))
        Text("➤", color = Cyan, fontSize = 14.sp)
    }
}

@Composable
private fun QuickActions(
    onBriefing: () -> Unit,
    onSystem: () -> Unit,
    onVision: () -> Unit,
    onArmor: () -> Unit
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        listOf(
            "📋 Briefing" to onBriefing,
            "▣ System" to onSystem,
            "👁 Vision" to onVision,
            "🛡 Armor" to onArmor
        ).forEach { (label, action) ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(36.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xE0081A28))
                    .border(1.2.dp, PurpleEdge, RoundedCornerShape(12.dp))
                    .clickable(onClick = action),
                contentAlignment = Alignment.Center
            ) {
                Text(label, color = Cyan, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
            }
        }
    }
}

@Composable
private fun BottomNav(
    onHome: () -> Unit,
    onChat: () -> Unit,
    onMic: () -> Unit,
    onVision: () -> Unit,
    onMore: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xF0051018))
            .border(1.2.dp, Color(0x5500E5FF), RoundedCornerShape(20.dp))
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        NavItem(Modifier.weight(1f), R.drawable.ic_home, "Home", true, onHome)
        NavItem(Modifier.weight(1f), R.drawable.ic_chat, "Chat", false, onChat)
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(Color(0x1A082030))
                .border(2.5.dp, Cyan, CircleShape)
                .clickable(onClick = onMic),
            contentAlignment = Alignment.Center
        ) {
            Icon(painterResource(R.drawable.ic_robot), null, tint = Cyan, modifier = Modifier.size(26.dp))
        }
        NavItem(Modifier.weight(1f), R.drawable.ic_person, "Vision", false, onVision)
        NavItem(Modifier.weight(1f), R.drawable.ic_shield, "More", false, onMore)
    }
}

@Composable
private fun NavItem(modifier: Modifier, icon: Int, label: String, active: Boolean, onClick: () -> Unit) {
    val c = if (active) Cyan else CyanSoft
    Column(
        modifier = modifier
            .fillMaxHeight()
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(painterResource(icon), null, tint = c, modifier = Modifier.size(22.dp))
        Text(label, color = c, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
    }
}
