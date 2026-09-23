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
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.animation.core.animateFloatAsState
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
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

/* ───────── Stark Industries / Mark-VII palette ───────── */
private val Cyan = Color(0xFF00D9FF)
private val CyanBright = Color(0xFFB8ECFF)
private val CyanCore = Color(0xFFE8FBFF)
private val CyanDim = Color(0xFF007A99)
private val CyanMid = Color(0xFF004466)
private val CyanSoft = Color(0xFF5A8A99)
private val Amber = Color(0xFFFFAA00)
private val AmberDim = Color(0xFFB07000)
private val Orange = Color(0xFFFF8C00)
private val OrangeBright = Color(0xFFFFC866)
private val OrangeCore = Color(0xFFFFE8B0)
private val OrangeSoft = Color(0xFFCC7A20)
private val Red = Color(0xFFFF3030)
private val Bg = Color(0xFF03080E)
private val BgDeep = Color(0xFF01040A)
private val Panel = Color(0xFF0A1825)
private val PanelDim = Color(0xFF050E16)
private val GridLine = Color(0xFF1A3040)

/* ───────── State ───────── */

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

/* ───────── Screen root ───────── */

@Composable
fun JarvisHudScreen(
    ui: JarvisHudUiState,
    actions: JarvisHudActions,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(listOf(BgDeep, Bg, Color(0xFF02060C)))
            )
    ) {
        // Soft ambient glow instead of heavy grid
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(Orange.copy(alpha = 0.06f), Color.Transparent),
                        radius = 900f
                    )
                )
        )
        CornerBrackets(Modifier.fillMaxSize())

        // Boot sequence overlay (power-on once)
        var booting by remember { mutableStateOf(true) }
        var bootProgress by remember { mutableStateOf(0f) }
        LaunchedEffect(Unit) {
            for (i in 1..20) {
                bootProgress = i / 20f
                delay(40)
            }
            delay(200)
            booting = false
        }
        if (booting) {
            BootPowerOnOverlay(progress = bootProgress)
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "JARVIS AI",
                        color = OrangeBright,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        "YOUR AI ASSISTANT",
                        color = OrangeSoft,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Box(
                    Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .border(1.dp, Orange.copy(alpha = 0.5f), CircleShape)
                        .clickable { actions.onSettings() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painterResource(R.drawable.ic_settings),
                        null,
                        tint = OrangeBright,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(Modifier.weight(0.12f))

            // Home: ONLY orange energy core
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clickable { actions.onReactorTap() },
                contentAlignment = Alignment.Center
            ) {
                MarkViiReactor(
                    state = ui.hudState,
                    modifier = Modifier.fillMaxSize(0.95f)
                )
            }

            Spacer(Modifier.height(8.dp))

            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0x990A1825))
                    .border(1.dp, Orange.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Column {
                    Text(
                        "JARVIS ONLINE",
                        color = OrangeBright,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        ui.stateLabel,
                        color = OrangeSoft,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
            WaveformBar(active = ui.waveformActive)
            Spacer(Modifier.height(6.dp))
            ResponseBar(ui.response)
            Spacer(Modifier.height(8.dp))

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

/* ───────── Backgrounds ───────── */

@Composable
private fun HexGridBackground(modifier: Modifier = Modifier) {
    val infinite = rememberInfiniteTransition(label = "hex")
    val t by infinite.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(14000, easing = LinearEasing)),
        label = "hexT"
    )
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val r = 28.dp.toPx()               // hex outer radius
        val dx = r * 1.5f                  // horizontal step
        val dy = r * kotlin.math.sqrt(3f)  // vertical step
        // Faint vertical scan band
        val bandW = w * 0.6f
        val bandX = (w + bandW) * t - bandW
        val band = Brush.horizontalGradient(
            colors = listOf(Color.Transparent, Cyan.copy(alpha = 0.06f), Color.Transparent),
            startX = bandX - bandW * 0.5f,
            endX = bandX + bandW * 0.5f
        )
        drawRect(band)

        var row = 0
        var y = -dy
        while (y < h + dy) {
            val offsetX = if (row % 2 == 0) 0f else dx / 2f
            var x = -dx + offsetX
            while (x < w + dx) {
                drawHex(x, y, r, GridLine.copy(alpha = 0.55f), strokeW = 0.8f)
                x += dx
            }
            row++
            y += dy
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawHex(
    cx: Float, cy: Float, r: Float, color: Color, strokeW: Float
) {
    val path = Path()
    for (i in 0..5) {
        val a = Math.toRadians((60.0 * i - 30.0))
        val px = cx + (r * cos(a)).toFloat()
        val py = cy + (r * sin(a)).toFloat()
        if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
    }
    path.close()
    drawPath(path, color, style = Stroke(strokeW))
}

@Composable
private fun ScanlineOverlay(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val gap = 3.dp.toPx()
        var y = 0f
        while (y < h) {
            drawLine(
                Color(0x1400D9FF),
                Offset(0f, y), Offset(w, y),
                strokeWidth = 0.5f
            )
            y += gap
        }
    }
}

@Composable
private fun CornerBrackets(modifier: Modifier = Modifier) {
    val c = Cyan.copy(alpha = 0.7f)
    Canvas(modifier) {
        val len = 22.dp.toPx()
        val pad = 10.dp.toPx()
        val w = 1.5f
        // Top-left
        drawLine(c, Offset(pad, pad), Offset(pad + len, pad), w)
        drawLine(c, Offset(pad, pad), Offset(pad, pad + len), w)
        // Top-right
        drawLine(c, Offset(size.width - pad - len, pad), Offset(size.width - pad, pad), w)
        drawLine(c, Offset(size.width - pad, pad), Offset(size.width - pad, pad + len), w)
        // Bottom-left
        drawLine(c, Offset(pad, size.height - pad), Offset(pad + len, size.height - pad), w)
        drawLine(c, Offset(pad, size.height - pad - len), Offset(pad, size.height - pad), w)
        // Bottom-right
        drawLine(c, Offset(size.width - pad - len, size.height - pad), Offset(size.width - pad, size.height - pad), w)
        drawLine(c, Offset(size.width - pad, size.height - pad - len), Offset(size.width - pad, size.height - pad), w)
    }
}

/* ───────── Glass helpers ───────── */

@Composable
private fun GlassCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0x990A1825),
                        Color(0x88050E16)
                    )
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    listOf(
                        Cyan.copy(alpha = 0.35f),
                        Cyan.copy(alpha = 0.12f)
                    )
                ),
                shape = RoundedCornerShape(18.dp)
            )
    ) {
        content()
    }
}

@Composable
private fun StatusLine(label: String, value: String, fraction: Float) {
    Column(Modifier.padding(vertical = 3.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, color = CyanSoft, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
            Text(value, color = CyanBright, fontSize = 9.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(2.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(3.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Color(0xFF0A1825))
        ) {
            Box(
                Modifier
                    .fillMaxWidth(fraction.coerceIn(0f, 1f))
                    .fillMaxHeight()
                    .background(Cyan.copy(alpha = 0.8f))
            )
        }
    }
}

@Composable
private fun MiniBar(label: String, fraction: Float, color: Color) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = CyanSoft, fontSize = 8.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.width(18.dp))
        Box(
            Modifier
                .weight(1f)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Color(0xFF0A1825))
        ) {
            Box(
                Modifier
                    .fillMaxWidth(fraction.coerceIn(0f, 1f))
                    .fillMaxHeight()
                    .background(color.copy(alpha = 0.85f))
            )
        }
    }
}

/* ───────── Header ───────── */

@Composable
private fun StarkHeader(
    clock: String,
    mode: String,
    network: String,
    onChat: () -> Unit,
    onSettings: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0x990A1825),
                        Color(0x88050E16)
                    )
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    listOf(Cyan.copy(alpha = 0.40f), Cyan.copy(alpha = 0.15f))
                ),
                shape = RoundedCornerShape(18.dp)
            )
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0x3300D9FF))
                    .border(1.2.dp, Cyan, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painterResource(R.drawable.ic_stark_hex),
                    contentDescription = null,
                    tint = Color.Unspecified,
                    modifier = Modifier.size(30.dp)
                )
            }
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "STARK INDUSTRIES",
                        color = Cyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.2.sp
                    )
                    Spacer(Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(2.dp))
                            .background(Amber.copy(alpha = 0.25f))
                            .border(0.5.dp, Amber, RoundedCornerShape(2.dp))
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    ) {
                        Text(
                            "MK · VII",
                            color = Amber,
                            fontSize = 7.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Text(
                    "J.A.R.V.I.S · ONLINE · ${mode.uppercase()}",
                    color = CyanSoft,
                    fontSize = 7.sp,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    clock,
                    color = CyanBright,
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    network.uppercase(),
                    color = CyanSoft,
                    fontSize = 7.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
            Spacer(Modifier.width(4.dp))
            RoundIconBtn(R.drawable.ic_chat, onChat)
            Spacer(Modifier.width(4.dp))
            RoundIconBtn(R.drawable.ic_settings, onSettings)
        }
    }
}

@Composable
private fun RoundIconBtn(iconRes: Int, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(Color(0xCC06121C))
            .border(1.dp, Cyan.copy(alpha = 0.55f), CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(painterResource(iconRes), null, tint = Cyan, modifier = Modifier.size(15.dp))
    }
}

private fun modeFor(state: HudState): String = when (state) {
    HudState.LISTENING -> "LISTENING"
    HudState.THINKING -> "PROCESSING"
    HudState.EXECUTING -> "EXECUTING"
    HudState.SPEAKING -> "RESPONDING"
    HudState.DONE -> "COMPLETE"
    HudState.ERROR -> "ERROR"
    HudState.IDLE -> "STANDBY"
}

/* ───────── Diagnostic chips ───────── */

@Composable
private fun DiagnosticChips(
    battery: String,
    network: String,
    ram: String,
    location: String
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        DiagnosticGauge(
            modifier = Modifier.weight(1f),
            icon = R.drawable.ic_battery_bolt,
            label = "BATT",
            valueText = battery.substringAfter("BATT:").trim().ifBlank { "—" },
            percent = parsePercent(battery),
            accent = if (parsePercent(battery) < 20) Red else Cyan
        )
        DiagnosticGauge(
            modifier = Modifier.weight(1f),
            icon = R.drawable.ic_wifi,
            label = "SIG",
            valueText = network,
            percent = signalPercent(network),
            accent = Cyan
        )
        DiagnosticGauge(
            modifier = Modifier.weight(1f),
            icon = R.drawable.ic_ram_chip,
            label = "MEM",
            valueText = ram.substringAfter("RAM:").trim().ifBlank { "—" },
            percent = parsePercent(ram),
            accent = if (parsePercent(ram) > 80) Amber else Cyan
        )
        DiagnosticGauge(
            modifier = Modifier.weight(1f),
            icon = R.drawable.ic_location_pin,
            label = "GPS",
            valueText = location,
            percent = if (location == "—") 0f else 100f,
            accent = Cyan
        )
    }
}

private fun parsePercent(s: String): Float =
    Regex("(\\d+)%?").find(s)?.groupValues?.get(1)?.toFloatOrNull() ?: 0f

private fun signalPercent(s: String): Float = when {
    s.contains("5G", true) -> 100f
    s.contains("4G", true) -> 85f
    s.contains("wifi", true) -> 95f
    s.contains("3G", true) -> 55f
    s.contains("2G", true) -> 25f
    s.equals("none", true) -> 0f
    else -> 70f
}

@Composable
private fun DiagnosticGauge(
    modifier: Modifier,
    icon: Int,
    label: String,
    valueText: String,
    percent: Float,
    accent: Color
) {
    Box(
        modifier = modifier
            .height(58.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(PanelDim)
            .border(1.dp, accent.copy(alpha = 0.55f), RoundedCornerShape(10.dp))
            .padding(horizontal = 6.dp, vertical = 4.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(38.dp)) {
                val infinite = rememberInfiniteTransition(label = label)
                val rot by infinite.animateFloat(
                    0f, 360f,
                    infiniteRepeatable(tween(9000, easing = LinearEasing)),
                    label = "rot"
                )
                Canvas(Modifier.size(38.dp)) {
                    val cx = size.width / 2f
                    val cy = size.height / 2f
                    val r = min(cx, cy) * 0.92f
                    // outer ring
                    drawCircle(accent.copy(alpha = 0.35f), r, Offset(cx, cy), style = Stroke(r * 0.10f))
                    // progress arc
                    val sweep = (percent.coerceIn(0f, 100f) / 100f) * 360f
                    rotate(-90f, Offset(cx, cy)) {
                        drawArc(
                            color = accent,
                            startAngle = 0f,
                            sweepAngle = sweep,
                            useCenter = false,
                            topLeft = Offset(cx - r, cy - r),
                            size = Size(r * 2, r * 2),
                            style = Stroke(r * 0.20f, cap = StrokeCap.Round)
                        )
                    }
                    // tick marks
                    rotate(rot, Offset(cx, cy)) {
                        for (i in 0 until 12) {
                            val ang = Math.toRadians((i * 30.0))
                            val rx1 = cx + (r * 0.55f * cos(ang)).toFloat()
                            val ry1 = cy + (r * 0.55f * sin(ang)).toFloat()
                            val rx2 = cx + (r * 0.78f * cos(ang)).toFloat()
                            val ry2 = cy + (r * 0.78f * sin(ang)).toFloat()
                            drawLine(accent.copy(alpha = 0.6f), Offset(rx1, ry1), Offset(rx2, ry2), strokeWidth = 0.8f)
                        }
                    }
                    // core icon dot
                    drawCircle(accent.copy(alpha = 0.3f), r * 0.25f, Offset(cx, cy))


                }
                Icon(
                    painterResource(icon), null, tint = accent,
                    modifier = Modifier.size(14.dp)
                )
            }
            Spacer(Modifier.width(4.dp))
            Column {
                Text(
                    label,
                    color = accent,
                    fontSize = 7.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Text(
                    valueText.take(8),
                    color = CyanBright,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/* ───────── Side panels ───────── */

@Composable
private fun SuitDiagnosticsPanel(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .width(54.dp)
            .fillMaxHeight()
            .clip(RoundedCornerShape(10.dp))
            .background(PanelDim)
            .border(1.dp, Cyan.copy(alpha = 0.45f), RoundedCornerShape(10.dp))
            .padding(horizontal = 4.dp, vertical = 6.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "SUIT",
                color = Cyan,
                fontSize = 7.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Text(
                "DIAG",
                color = CyanDim,
                fontSize = 6.sp,
                fontFamily = FontFamily.Monospace
            )
            Spacer(Modifier.height(6.dp))
            VerticalBarMeter(label = "PWR", percent = 0.78f, accent = Cyan)
            Spacer(Modifier.height(4.dp))
            VerticalBarMeter(label = "ARM", percent = 0.62f, accent = Amber)
            Spacer(Modifier.height(4.dp))
            VerticalBarMeter(label = "FLT", percent = 0.45f, accent = Cyan)
            Spacer(Modifier.height(4.dp))
            VerticalBarMeter(label = "REP", percent = 0.88f, accent = Cyan)
            Spacer(Modifier.weight(1f))
            SuitIcon()
        }
    }
}

@Composable
private fun VerticalBarMeter(label: String, percent: Float, accent: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .width(14.dp)
                .height(60.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(Color(0xFF050E16))
                .border(0.5.dp, accent.copy(alpha = 0.5f), RoundedCornerShape(3.dp))
        ) {
            // ticks
            val infinite = rememberInfiniteTransition(label = label)
            val pulse by infinite.animateFloat(
                0f, 1f,
                infiniteRepeatable(tween(1500), RepeatMode.Reverse),
                label = "p"
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(percent.coerceIn(0f, 1f))
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            listOf(accent.copy(alpha = 0.9f), accent.copy(alpha = 0.4f * pulse + 0.2f))
                        )
                    )
            )
        }
        Spacer(Modifier.height(2.dp))
        Text(label, color = accent, fontSize = 6.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SuitIcon() {
    Box(
        modifier = Modifier
            .size(46.dp)
            .clip(CircleShape)
            .background(Color(0x3300D9FF))
            .border(1.dp, Cyan, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painterResource(R.drawable.ic_ironman_full),
            contentDescription = null,
            tint = Cyan,
            modifier = Modifier.size(36.dp)
        )
    }
}

@Composable
private fun EnergyMatrixPanel(
    cpu: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .width(54.dp)
            .fillMaxHeight()
            .clip(RoundedCornerShape(10.dp))
            .background(PanelDim)
            .border(1.dp, Cyan.copy(alpha = 0.45f), RoundedCornerShape(10.dp))
            .padding(horizontal = 4.dp, vertical = 6.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "ENERGY",
                color = Cyan,
                fontSize = 7.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Text(
                "MATRIX",
                color = CyanDim,
                fontSize = 6.sp,
                fontFamily = FontFamily.Monospace
            )
            Spacer(Modifier.height(6.dp))
            CircularEnergyGauge(percent = parsePercent(cpu), accent = Cyan)
            Spacer(Modifier.height(6.dp))
            listOf(
                Triple("01", 0.92f, Cyan),
                Triple("02", 0.74f, Cyan),
                Triple("03", 0.55f, Amber),
                Triple("04", 0.31f, Red)
            ).forEach { (id, p, c) ->
                CompactRow(id, p, c)
                Spacer(Modifier.height(3.dp))
            }
            Spacer(Modifier.weight(1f))
            Text(
                "v3.0.1",
                color = CyanSoft,
                fontSize = 6.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
private fun CircularEnergyGauge(percent: Float, accent: Color) {
    val infinite = rememberInfiniteTransition(label = "energy")
    val rot by infinite.animateFloat(
        0f, 360f,
        infiniteRepeatable(tween(6000, easing = LinearEasing)),
        label = "erot"
    )
    Box(
        modifier = Modifier.size(46.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val r = min(cx, cy) * 0.85f
            // outer dashed ring
            rotate(rot, Offset(cx, cy)) {
                drawCircle(
                    accent.copy(alpha = 0.55f),
                    r,
                    Offset(cx, cy),
                    style = Stroke(
                        width = r * 0.06f,
                        cap = StrokeCap.Round,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(r * 0.25f, r * 0.12f))
                    )
                )
            }
            // progress arc
            val sweep = (percent.coerceIn(0f, 100f) / 100f) * 360f
            rotate(-90f, Offset(cx, cy)) {
                drawArc(
                    color = accent,
                    startAngle = 0f,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = Offset(cx - r, cy - r),
                    size = Size(r * 2, r * 2),
                    style = Stroke(r * 0.18f, cap = StrokeCap.Round)
                )
            }
            // inner ring
            drawCircle(Color(0xFF050E16), r * 0.55f, Offset(cx, cy))
            drawCircle(accent.copy(alpha = 0.45f), r * 0.55f, Offset(cx, cy), style = Stroke(r * 0.04f))
        }
        Text(
            "${percent.toInt()}",
            color = accent,
            fontSize = 13.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun CompactRow(id: String, percent: Float, accent: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .height(10.dp)
    ) {
        Text(
            id,
            color = accent,
            fontSize = 6.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(14.dp)
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Color(0xFF050E16))
                .border(0.5.dp, accent.copy(alpha = 0.45f), RoundedCornerShape(2.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(percent.coerceIn(0f, 1f))
                    .fillMaxHeight()
                    .background(accent)
            )
        }
    }
}

/* ───────── Boot + Mark VII central reactor ───────── */

@Composable
private fun BootPowerOnOverlay(progress: Float) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0xEE01040A)),
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.size(220.dp)) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val r = min(cx, cy) * 0.9f
            val accent = Orange
            // Expanding rings based on progress
            for (i in 1..4) {
                val t = (progress * 4f - (i - 1)).coerceIn(0f, 1f)
                if (t > 0f) {
                    drawCircle(
                        accent.copy(alpha = 0.15f + 0.4f * (1f - t)),
                        r * (0.2f + t * 0.7f),
                        Offset(cx, cy),
                        style = Stroke(width = 2.5f)
                    )
                }
            }
            drawCircle(
                Brush.radialGradient(
                    listOf(OrangeCore, Orange, Orange.copy(alpha = 0.2f)),
                    center = Offset(cx, cy),
                    radius = r * 0.2f * progress.coerceAtLeast(0.15f)
                ),
                r * 0.18f * progress.coerceAtLeast(0.2f),
                Offset(cx, cy)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(200.dp))
            Text(
                "J.A.R.V.I.S. BOOT SEQUENCE",
                color = OrangeBright,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "${(progress * 100).toInt()}%",
                color = OrangeSoft,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
private fun MarkViiReactor(
    state: HudState,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier, contentAlignment = Alignment.Center) {
        val side = minOf(maxWidth, maxHeight)
        val reactorSize = (side * 0.96f).coerceAtLeast(160.dp)
        val infinite = rememberInfiniteTransition(label = "energyCore")
        val slow by infinite.animateFloat(
            0f, 360f,
            infiniteRepeatable(tween(32000, easing = LinearEasing)),
            label = "slow"
        )
        val med by infinite.animateFloat(
            0f, 360f,
            infiniteRepeatable(tween(16000, easing = LinearEasing)),
            label = "med"
        )
        val fast by infinite.animateFloat(
            0f, 360f,
            infiniteRepeatable(
                tween(
                    when (state) {
                        HudState.LISTENING -> 2800
                        HudState.THINKING, HudState.EXECUTING -> 1800
                        HudState.SPEAKING -> 4200
                        HudState.DONE -> 10000
                        else -> 7000
                    },
                    easing = LinearEasing
                )
            ),
            label = "fast"
        )
        val pulse by infinite.animateFloat(
            0f, 1f,
            infiniteRepeatable(
                tween(if (state == HudState.LISTENING) 500 else 1800),
                RepeatMode.Reverse
            ),
            label = "pulse"
        )
        // Listening / speaking ripple expand 0→1 loop
        val ripple by infinite.animateFloat(
            0f, 1f,
            infiniteRepeatable(
                tween(
                    when (state) {
                        HudState.LISTENING -> 1200
                        HudState.SPEAKING -> 1600
                        HudState.THINKING, HudState.EXECUTING -> 2000
                        else -> 3000
                    },
                    easing = LinearEasing
                ),
                RepeatMode.Restart
            ),
            label = "ripple"
        )
        // Boot-style ring draw progress (used when idle settles)
        val bootRing by infinite.animateFloat(
            0.7f, 1f,
            infiniteRepeatable(tween(4000, easing = LinearEasing), RepeatMode.Reverse),
            label = "bootRing"
        )
        // Abstract energy core – orange (user requested)
        val accent = when (state) {
            HudState.THINKING, HudState.EXECUTING -> OrangeBright
            HudState.SPEAKING -> Orange
            HudState.ERROR -> Red
            HudState.DONE -> OrangeBright
            else -> Orange
        }
        val coreAccent = OrangeCore

        Canvas(modifier = Modifier.size(reactorSize)) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val r = min(cx, cy) * 0.92f

            // Deep glow bloom
            drawCircle(accent.copy(alpha = 0.05f + 0.04f * pulse), r * 1.40f, Offset(cx, cy))
            drawCircle(accent.copy(alpha = 0.09f), r * 1.22f, Offset(cx, cy))
            drawCircle(accent.copy(alpha = 0.14f), r * 1.08f, Offset(cx, cy))

            // Dense outer energy field (radial strands)
            rotate(slow * 0.18f, Offset(cx, cy)) {
                for (i in 0 until 90) {
                    val a = Math.toRadians(i * 4.0)
                    val jitter = ((i * 17) % 10) / 10f
                    val inner = r * (0.78f + 0.08f * jitter)
                    val outer = r * (0.92f + 0.08f * ((i * 13) % 7) / 7f)
                    val major = i % 5 == 0
                    drawLine(
                        accent.copy(alpha = if (major) 0.75f else 0.22f + 0.15f * jitter),
                        Offset(cx + (inner * cos(a)).toFloat(), cy + (inner * sin(a)).toFloat()),
                        Offset(cx + (outer * cos(a)).toFloat(), cy + (outer * sin(a)).toFloat()),
                        strokeWidth = if (major) 2.0f else 0.9f,
                        cap = StrokeCap.Round
                    )
                }
            }

            // Multiple elliptical-style orbits
            for (o in 0 until 6) {
                val frac = 0.30f + o * 0.10f
                val speed = if (o % 2 == 0) med * (0.35f + o * 0.1f) else -med * (0.4f + o * 0.08f)
                rotate(speed, Offset(cx, cy)) {
                    drawCircle(
                        accent.copy(alpha = 0.30f + o * 0.05f),
                        r * frac, Offset(cx, cy),
                        style = Stroke(
                            width = r * (0.008f + (6 - o) * 0.0015f),
                            pathEffect = if (o % 2 == 1)
                                PathEffect.dashPathEffect(floatArrayOf(9f, 6f), 0f)
                            else null
                        )
                    )
                    val nodes = 6 + o * 2
                    for (n in 0 until nodes) {
                        val a = Math.toRadians(n * 360.0 / nodes + o * 8.0)
                        drawCircle(
                            accent.copy(alpha = 0.80f),
                            r * (0.010f + (o % 3) * 0.003f),
                            Offset(cx + (r * frac * cos(a)).toFloat(), cy + (r * frac * sin(a)).toFloat())
                        )
                    }
                }
            }

            // Chaotic inner energy strands
            rotate(fast * 0.55f, Offset(cx, cy)) {
                for (i in 0 until 20) {
                    val a0 = Math.toRadians(i * 18.0 + fast * 0.15)
                    val a1 = a0 + Math.toRadians(20.0 + (i % 4) * 10.0)
                    val r0 = r * 0.10f
                    val r1 = r * (0.26f + 0.06f * pulse)
                    drawLine(
                        accent.copy(alpha = 0.40f + 0.25f * pulse),
                        Offset(cx + (r0 * cos(a0)).toFloat(), cy + (r0 * sin(a0)).toFloat()),
                        Offset(cx + (r1 * cos(a1)).toFloat(), cy + (r1 * sin(a1)).toFloat()),
                        strokeWidth = 1.3f,
                        cap = StrokeCap.Round
                    )
                }
            }

            // Fast core ring
            rotate(fast * 1.3f, Offset(cx, cy)) {
                drawCircle(
                    accent.copy(alpha = 0.80f),
                    r * 0.22f, Offset(cx, cy),
                    style = Stroke(
                        width = r * 0.016f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(7f, 5f), 0f)
                    )
                )
            }

            // State ripples
            if (state == HudState.LISTENING || state == HudState.SPEAKING || state == HudState.THINKING) {
                for (k in 0 until 3) {
                    val t = ((ripple + k * 0.33f) % 1f)
                    drawCircle(
                        accent.copy(alpha = (1f - t) * 0.5f),
                        r * (0.22f + t * 0.75f),
                        Offset(cx, cy),
                        style = Stroke(width = r * 0.014f * (1f - t * 0.5f))
                    )
                }
            }

            // Hot core
            val coreR = r * 0.13f
            drawCircle(accent.copy(alpha = 0.25f + 0.2f * pulse), coreR * 2.8f, Offset(cx, cy))
            drawCircle(accent.copy(alpha = 0.45f), coreR * 1.8f, Offset(cx, cy))
            drawCircle(
                Brush.radialGradient(
                    listOf(Color.White, coreAccent, accent, accent.copy(alpha = 0.2f)),
                    center = Offset(cx, cy),
                    radius = coreR * 1.4f
                ),
                coreR, Offset(cx, cy)
            )
            drawCircle(Color.White.copy(alpha = 0.98f), coreR * 0.32f, Offset(cx, cy))

            // Micro particles around core
            rotate(fast * 1.7f, Offset(cx, cy)) {
                for (i in 0 until 12) {
                    val a = Math.toRadians(i * 30.0)
                    val pr = coreR * (1.9f + (i % 4) * 0.35f)
                    drawCircle(
                        accent.copy(alpha = 0.75f),
                        r * 0.009f,
                        Offset(cx + (pr * cos(a)).toFloat(), cy + (pr * sin(a)).toFloat())
                    )
                }
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawHexFrame(
    cx: Float, cy: Float, r: Float, color: Color, width: Float
) {
    val path = Path()
    for (i in 0..5) {
        val a = Math.toRadians((60.0 * i - 30.0))
        val px = cx + (r * cos(a)).toFloat()
        val py = cy + (r * sin(a)).toFloat()
        if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
    }
    path.close()
    drawPath(path, color, style = Stroke(width))
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawCardinalMarkers(
    cx: Float, cy: Float, r: Float, color: Color
) {
    val sides = listOf("MK7", "JARV", "FRID", "EDITH")
    for (i in sides.indices) {
        val ang = Math.toRadians((60.0 * i - 30.0).toDouble())
        val mx = cx + (r * cos(ang)).toFloat()
        val my = cy + (r * sin(ang)).toFloat()
        drawCircle(color, 2.5f, Offset(mx, my))
        // small line to outer ring
        val ex = cx + ((r + 14f) * cos(ang)).toFloat()
        val ey = cy + ((r + 14f) * sin(ang)).toFloat()
        drawLine(color.copy(alpha = 0.6f), Offset(mx, my), Offset(ex, ey), strokeWidth = 1.2f)
    }
}

/* ───────── State + waveform + response ───────── */

@Composable
private fun StateStatusBar(label: String, state: HudState) {
    val color = when (state) {
        HudState.LISTENING -> Cyan
        HudState.THINKING, HudState.EXECUTING -> Amber
        HudState.SPEAKING -> CyanBright
        HudState.ERROR -> Red
        HudState.DONE -> CyanBright
        HudState.IDLE -> CyanSoft
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(PanelDim)
            .border(1.dp, color.copy(alpha = 0.55f), RoundedCornerShape(10.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PulsingDot(color)
        Spacer(Modifier.width(6.dp))
        Text(
            "SYS",
            color = CyanSoft,
            fontSize = 8.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
        Spacer(Modifier.width(6.dp))
        Text(
            "▸ $label",
            color = color,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
        )
        Text(
            "// 0x${(System.currentTimeMillis() and 0xFFFF).toString(16).uppercase()}",
            color = CyanSoft,
            fontSize = 7.sp,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
private fun PulsingDot(color: Color) {
    val infinite = rememberInfiniteTransition(label = "dot")
    val a by infinite.animateFloat(
        0.4f, 1f,
        infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "a"
    )
    Box(
        modifier = Modifier
            .size(8.dp)
            .clip(CircleShape)
            .background(color.copy(alpha = a))
            .border(0.8.dp, color, CircleShape)
    )
}

@Composable
private fun WaveformBar(active: Boolean) {
    val infinite = rememberInfiniteTransition(label = "wf")
    val phase by infinite.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(if (active) 500 else 2200, easing = LinearEasing)),
        label = "wfp"
    )
    Canvas(
        Modifier
            .fillMaxWidth()
            .height(22.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(Color(0xFF050E16))
            .border(0.5.dp, Cyan.copy(alpha = 0.45f), RoundedCornerShape(4.dp))
    ) {
        val n = 56
        val w = size.width / n
        for (i in 0 until n) {
            val mid = n / 2f
            val dist = kotlin.math.abs(i - mid) / mid
            val h = if (active) {
                size.height * (0.10f + 0.85f *
                    (0.5f + 0.5f * sin((i * 0.4f + phase * 6.28f).toDouble()).toFloat()) *
                    (1f - dist * 0.4f))
            } else {
                size.height * (0.10f + 0.06f * sin((i * 0.3f + phase * 6.28f).toDouble()).toFloat())
            }
            val x = i * w + w / 2
            drawLine(
                (if (active) Cyan else CyanSoft).copy(alpha = 0.5f + 0.5f * (1f - dist)),
                Offset(x, size.height / 2 - h / 2),
                Offset(x, size.height / 2 + h / 2),
                strokeWidth = w * 0.45f,
                cap = StrokeCap.Round
            )
        }
    }
}

@Composable
private fun ResponseBar(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(PanelDim)
            .border(1.dp, Cyan.copy(alpha = 0.45f), RoundedCornerShape(10.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(painterResource(R.drawable.ic_robot), null, tint = Cyan, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(6.dp))
        Column(Modifier.weight(1f)) {
            Text(
                "▸ JARVIS RESPONSE",
                color = CyanSoft,
                fontSize = 6.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Text(
                text,
                color = CyanBright,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/* ───────── Quick actions + bottom nav ───────── */

@Composable
private fun QuickActions(
    onBriefing: () -> Unit,
    onSystem: () -> Unit,
    onVision: () -> Unit,
    onArmor: () -> Unit
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        ChevronAction(Modifier.weight(1f), "BRIEF", R.drawable.ic_info, onBriefing)
        ChevronAction(Modifier.weight(1f), "SYS", R.drawable.ic_cpu, onSystem)
        ChevronAction(Modifier.weight(1f), "VISN", R.drawable.ic_eye, onVision)
        ChevronAction(Modifier.weight(1f), "ARMOR", R.drawable.ic_shield, onArmor)
    }
}

@Composable
private fun ChevronAction(
    modifier: Modifier,
    label: String,
    icon: Int,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(34.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(PanelDim)
            .border(1.dp, Cyan.copy(alpha = 0.55f), RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(painterResource(icon), null, tint = Cyan, modifier = Modifier.size(13.dp))
            Spacer(Modifier.width(4.dp))
            Text(
                label,
                color = Cyan,
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
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
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(
                Brush.verticalGradient(listOf(Color(0xF0050E16), Color(0xF003080E)))
            )
            .border(1.dp, Cyan.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
            .padding(horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            NavItem(Modifier.weight(1f), R.drawable.ic_home, "HOME", onHome)
            NavItem(Modifier.weight(1f), R.drawable.ic_chat, "CHAT", onChat)
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(Color(0x4000D9FF))
                    .border(2.dp, Cyan, CircleShape)
                    .clickable(onClick = onMic),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painterResource(R.drawable.ic_stark_hex),
                    contentDescription = null,
                    tint = Color.Unspecified,
                    modifier = Modifier.size(34.dp)
                )
            }
            NavItem(Modifier.weight(1f), R.drawable.ic_person, "VISN", onVision)
            NavItem(Modifier.weight(1f), R.drawable.ic_settings, "MORE", onMore)
        }
    }
}

@Composable
private fun NavItem(modifier: Modifier, icon: Int, label: String, onClick: () -> Unit) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(painterResource(icon), null, tint = Cyan, modifier = Modifier.size(18.dp))
        Spacer(Modifier.height(2.dp))
        Text(label, color = CyanSoft, fontSize = 7.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
    }
}
