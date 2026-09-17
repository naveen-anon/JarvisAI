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
import androidx.compose.runtime.getValue
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
                Brush.verticalGradient(listOf(BgDeep, Bg, BgDeep))
            )
    ) {
        HexGridBackground(Modifier.fillMaxSize())
        ScanlineOverlay(Modifier.fillMaxSize())
        CornerBrackets(Modifier.fillMaxSize())

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            StarkHeader(
                clock = ui.clock,
                mode = modeFor(ui.hudState),
                network = ui.network,
                onChat = actions.onChat,
                onSettings = actions.onSettings
            )
            Spacer(Modifier.height(8.dp))
            DiagnosticChips(
                battery = ui.battery,
                network = ui.network,
                ram = ui.ram,
                location = ui.location
            )
            Spacer(Modifier.height(6.dp))

            // Center Mark-VII circular HUD with flanking side panels
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SuitDiagnosticsPanel(modifier = Modifier.padding(end = 4.dp))
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable { actions.onReactorTap() },
                    contentAlignment = Alignment.Center
                ) {
                    MarkViiReactor(
                        state = ui.hudState,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                EnergyMatrixPanel(
                    cpu = ui.ram,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }

            Spacer(Modifier.height(6.dp))
            StateStatusBar(label = ui.stateLabel, state = ui.hudState)
            Spacer(Modifier.height(4.dp))
            WaveformBar(active = ui.waveformActive)
            Spacer(Modifier.height(6.dp))
            ResponseBar(ui.response)
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
    val len = 22.dp.toPx()
    val pad = 10.dp.toPx()
    val w = 1.5f
    val c = Cyan.copy(alpha = 0.7f)
    Canvas(modifier) {
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
            .clip(RoundedCornerShape(14.dp))
            .background(
                Brush.verticalGradient(listOf(Color(0xCC0A1825), Color(0xCC050E16)))
            )
            .border(1.dp, Cyan.copy(alpha = 0.55f), RoundedCornerShape(14.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp)
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

/* ───────── Mark VII central reactor ───────── */

@Composable
private fun MarkViiReactor(
    state: HudState,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier, contentAlignment = Alignment.Center) {
        val side = min(maxWidth, maxHeight)
        val size = (side * 0.96f).coerceAtLeast(160.dp)
        val infinite = rememberInfiniteTransition(label = "mk7")
        val slow by infinite.animateFloat(
            0f, 360f,
            infiniteRepeatable(tween(28000, easing = LinearEasing)),
            label = "slow"
        )
        val med by infinite.animateFloat(
            0f, 360f,
            infiniteRepeatable(tween(14000, easing = LinearEasing)),
            label = "med"
        )
        val fast by infinite.animateFloat(
            0f, 360f,
            infiniteRepeatable(
                tween(
                    when (state) {
                        HudState.LISTENING -> 3500
                        HudState.THINKING, HudState.EXECUTING -> 2500
                        HudState.SPEAKING -> 5000
                        HudState.DONE -> 12000
                        else -> 8000
                    },
                    easing = LinearEasing
                )
            ),
            label = "fast"
        )
        val pulse by infinite.animateFloat(
            0f, 1f,
            infiniteRepeatable(
                tween(if (state == HudState.LISTENING) 600 else 2200),
                RepeatMode.Reverse
            ),
            label = "pulse"
        )
        val accent = when (state) {
            HudState.THINKING, HudState.EXECUTING -> Amber
            HudState.SPEAKING -> CyanBright
            HudState.ERROR -> Red
            HudState.DONE -> CyanBright
            else -> Cyan
        }

        Canvas(modifier = Modifier.size(size)) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val r = min(cx, cy) * 0.96f

            // outer glow halo
            drawCircle(accent.copy(alpha = 0.10f * (0.6f + 0.4f * pulse)), r * 1.10f, Offset(cx, cy))
            drawCircle(accent.copy(alpha = 0.06f), r * 1.25f, Offset(cx, cy))

            // Hexagonal outer frame
            drawHexFrame(cx, cy, r * 0.98f, accent.copy(alpha = 0.85f), r * 0.012f)
            // Inner hex frame
            drawHexFrame(cx, cy, r * 0.88f, accent.copy(alpha = 0.45f), r * 0.006f)

            // Compass tick marks around outer hex
            rotate(slow * 0.3f, Offset(cx, cy)) {
                for (i in 0 until 36) {
                    val ang = Math.toRadians((i * 10.0))
                    val isMajor = i % 3 == 0
                    val inner = r * (if (isMajor) 0.84f else 0.86f)
                    val outer = r * 0.92f
                    val x1 = cx + (inner * cos(ang)).toFloat()
                    val y1 = cy + (inner * sin(ang)).toFloat()
                    val x2 = cx + (outer * cos(ang)).toFloat()
                    val y2 = cy + (outer * sin(ang)).toFloat()
                    drawLine(
                        accent.copy(alpha = if (isMajor) 0.85f else 0.35f),
                        Offset(x1, y1), Offset(x2, y2),
                        strokeWidth = if (isMajor) r * 0.012f else r * 0.005f
                    )
                }
            }

            // N/S/E/W cardinal labels (drawn as small markers at the hex points)
            drawCardinalMarkers(cx, cy, r * 0.92f, accent)

            // Inner rotating dashed rings (Mark VII style)
            rotate(fast * 0.5f, Offset(cx, cy)) {
                drawCircle(
                    accent.copy(alpha = 0.7f),
                    r * 0.74f, Offset(cx, cy),
                    style = Stroke(
                        width = r * 0.018f,
                        cap = StrokeCap.Round,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(r * 0.10f, r * 0.04f))
                    )
                )
            }
            rotate(-med, Offset(cx, cy)) {
                drawCircle(
                    accent.copy(alpha = 0.55f),
                    r * 0.62f, Offset(cx, cy),
                    style = Stroke(
                        width = r * 0.012f,
                        cap = StrokeCap.Round,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(r * 0.04f, r * 0.06f))
                    )
                )
            }
            rotate(med * 0.7f, Offset(cx, cy)) {
                drawCircle(
                    CyanMid.copy(alpha = 0.7f),
                    r * 0.50f, Offset(cx, cy),
                    style = Stroke(
                        width = r * 0.014f,
                        cap = StrokeCap.Round,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(r * 0.02f, r * 0.05f))
                    )
                )
            }

            // solid inner ring
            drawCircle(accent.copy(alpha = 0.95f), r * 0.42f, Offset(cx, cy), style = Stroke(r * 0.025f))
            drawCircle(Color.White.copy(alpha = 0.35f), r * 0.40f, Offset(cx, cy), style = Stroke(r * 0.008f))

            // Inner suit silhouette (very stylized hex shape)
            drawHexFrame(cx, cy, r * 0.32f, accent.copy(alpha = 0.85f), r * 0.008f)

            // Reactor core triangle + glow
            val coreR = r * (0.18f + 0.04f * pulse)
            drawCircle(accent.copy(alpha = 0.5f * (0.6f + 0.4f * pulse)), coreR * 1.7f, Offset(cx, cy))
            drawCircle(accent.copy(alpha = 0.9f), coreR * 0.6f, Offset(cx, cy))
            drawCircle(CyanCore.copy(alpha = 0.95f), coreR * 0.32f, Offset(cx, cy))
            drawCircle(Color.White, coreR * 0.14f, Offset(cx, cy))

            val triR = coreR * 1.3f
            val path = Path()
            for (i in 0..2) {
                val ang = Math.toRadians((-90 + i * 120).toDouble())
                val x = cx + (triR * cos(ang)).toFloat()
                val y = cy + (triR * sin(ang)).toFloat()
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            path.close()
            drawPath(path, accent.copy(alpha = 0.95f), style = Stroke(r * 0.020f))

            // Mark-VII crosshair lines through core
            drawLine(
                accent.copy(alpha = 0.5f),
                Offset(cx - r * 0.30f, cy), Offset(cx + r * 0.30f, cy),
                strokeWidth = r * 0.006f
            )
            drawLine(
                accent.copy(alpha = 0.5f),
                Offset(cx, cy - r * 0.30f), Offset(cx, cy + r * 0.30f),
                strokeWidth = r * 0.006f
            )

            // Outer ring labels text approximation - small ticks at hex points
            for (i in 0..5) {
                val ang = Math.toRadians((60.0 * i - 30.0))
                val px = cx + (r * 0.98f * cos(ang)).toFloat()
                val py = cy + (r * 0.98f * sin(ang)).toFloat()
                drawCircle(accent, r * 0.02f, Offset(px, py))
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
