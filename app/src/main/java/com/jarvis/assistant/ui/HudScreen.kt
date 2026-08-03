package com.jarvis.assistant.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jarvis.assistant.ui.theme.*
import kotlin.math.min

@Composable
fun HudScreen(
    currentTime: String,
    location: String,
    weatherText: String,
    ramPercent: Int,
    responseText: String,
    isListening: Boolean,
    onCoreTap: () -> Unit,
    onSettingsClick: () -> Unit,
    onChatClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDeep)
            .padding(16.dp)
    ) {
        // ---------- Top bar ----------
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("J.A.R.V.I.S", color = TextMain, fontSize = 26.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                Text("JUST A RATHER VERY INTELLIGENT SYSTEM", color = TextDim, fontSize = 9.sp, letterSpacing = 1.sp)
            }
            Text(currentTime, color = Cyan, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(6.dp).background(Cyan, RoundedCornerShape(3.dp)))
                Spacer(Modifier.width(6.dp))
                Text("RAM: $ramPercent%", color = TextDim, fontSize = 11.sp)
            }
            HudChip(text = "⚙ SETTINGS", onClick = onSettingsClick)
            HudChip(text = "💬 CHAT", onClick = onChatClick)
        }

        Spacer(Modifier.height(14.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text("LOCATION", color = TextDim, fontSize = 10.sp)
                Text(location, color = TextMain, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("WEATHER", color = TextDim, fontSize = 10.sp)
                Text(weatherText, color = TextMain, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(Modifier.weight(1f))

        // ---------- Arc Reactor Core ----------
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            ArcReactorCore(isListening = isListening, onTap = onCoreTap)
        }

        Spacer(Modifier.height(14.dp))
        Text(
            "TAP CORE TO SPEAK", color = TextDim, fontSize = 12.sp, letterSpacing = 1.sp,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        Spacer(Modifier.height(16.dp))
        WaveformBar()

        Spacer(Modifier.weight(1f))

        // ---------- Response panel ----------
        HudPanel(label = "RESPONSE") {
            Text(responseText, color = Cyan, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(Modifier.height(14.dp))
        Text(
            "STARK INDUSTRIES  //  MARK II",
            color = TextDim,
            fontSize = 10.sp,
            letterSpacing = 2.sp,
            modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 4.dp, bottom = 4.dp)
        )
    }
}

@Composable
private fun HudChip(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .border(1.dp, BorderCyan, RoundedCornerShape(20.dp))
            .background(PanelBg, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(text, color = Cyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun HudPanel(label: String, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BorderCyan, RoundedCornerShape(14.dp))
            .background(PanelBg, RoundedCornerShape(14.dp))
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 8.dp)) {
            Box(Modifier.width(3.dp).height(14.dp).background(Cyan))
            Spacer(Modifier.width(8.dp))
            Text(label, color = Cyan, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        }
        content()
    }
}

@Composable
private fun ArcReactorCore(isListening: Boolean, onTap: () -> Unit) {
    val infinite = rememberInfiniteTransition(label = "reactor")
    val pulse by infinite.animateFloat(
        initialValue = if (isListening) 0.85f else 0.95f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulse"
    )

    Canvas(
        modifier = Modifier
            .size(260.dp)
            .clickable(onClick = onTap)
    ) {
        val center = Offset(size.width / 2, size.height / 2)
        val maxRadius = min(size.width, size.height) / 2 * pulse

        // outer dashed-look rings
        for (i in 1..4) {
            val r = maxRadius * (0.55f + i * 0.11f)
            drawCircle(
                color = Cyan.copy(alpha = 0.5f - i * 0.06f),
                radius = r,
                center = center,
                style = Stroke(width = 2f)
            )
        }

        // glowing core
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Cyan, Cyan.copy(alpha = 0.6f), BgDeep),
                center = center,
                radius = maxRadius * 0.45f
            ),
            radius = maxRadius * 0.45f,
            center = center
        )

        // bright center point
        drawCircle(color = Color.White, radius = maxRadius * 0.14f, center = center)
    }
}

@Composable
private fun WaveformBar() {
    Row(
        modifier = Modifier.fillMaxWidth().height(24.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val heights = listOf(6, 14, 20, 10, 18, 8, 22, 12, 16, 6, 20, 10, 14, 8, 18, 12, 6, 22, 10, 16)
        heights.forEach { h ->
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(h.dp)
                    .background(Cyan.copy(alpha = 0.7f), RoundedCornerShape(2.dp))
            )
        }
    }
}
