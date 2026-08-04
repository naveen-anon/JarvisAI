package com.jarvis.assistant.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jarvis.assistant.ui.theme.HudBg
import com.jarvis.assistant.ui.theme.HudCore
import com.jarvis.assistant.ui.theme.HudCyan
import com.jarvis.assistant.ui.theme.HudCyanDim
import com.jarvis.assistant.ui.theme.HudPanel
import com.jarvis.assistant.ui.theme.HudTextDim
import com.jarvis.assistant.ui.theme.HudWaveform
import com.jarvis.assistant.ui.theme.HudWhite

@Composable
fun JarvisHudScreen(
    ramPercent: Int = 65,
    location: String = "NEW DELHI",
    weather: String = "UNAVAILABLE OFFLINE",
    lastInput: String = "—",
    lastResponse: String = "Standing by.",
    onSettingsClick: () -> Unit = {},
    onChatClick: () -> Unit = {},
    onCoreTap: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(HudBg)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    "J.A.R.V.I.S",
                    color = HudCyan,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    "JUST A RATHER VERY INTELLIGENT SYSTEM",
                    color = HudTextDim,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = currentTimeHHMMSS(),
                    color = HudCyan,
                    fontSize = 20.sp,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = currentDateLabel(),
                    color = HudTextDim,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        Divider(color = HudCyanDim, thickness = 1.dp, modifier = Modifier.padding(top = 12.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            HudPillText(text = "RAM: $ramPercent%")
            HudPillText(
                text = "⚙  SETTINGS",
                modifier = Modifier.weight(1f),
                onClick = onSettingsClick
            )
            HudPillText(
                text = "💬  CHAT",
                modifier = Modifier.weight(1f),
                onClick = onChatClick
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 18.dp)
        ) {
            Column(Modifier.weight(1f)) {
                Text("📍 LOCATION", color = HudTextDim, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                Text(location, color = HudCyan, fontSize = 15.sp, fontFamily = FontFamily.Monospace)
            }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                Text("WEATHER", color = HudTextDim, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                Text(weather, color = HudTextDim, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            HudCore(size = 260.dp, onTap = onCoreTap)
        }

        Text(
            "TAP CORE TO SPEAK",
            color = HudTextDim,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 14.dp)
        )

        HudWaveform(modifier = Modifier.padding(top = 12.dp))

        HudPanel(modifier = Modifier.fillMaxWidth().padding(top = 20.dp)) {
            Column {
                Text("┃ INPUT", color = HudCyan, fontSize = 13.sp, fontFamily = FontFamily.Monospace)
                Text(
                    lastInput,
                    color = HudTextDim,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
        }

        HudPanel(modifier = Modifier.fillMaxWidth().padding(top = 14.dp)) {
            Column {
                Text("┃ RESPONSE", color = HudCyan, fontSize = 13.sp, fontFamily = FontFamily.Monospace)
                Text(
                    lastResponse,
                    color = HudWhite,
                    fontSize = 15.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
        }

        Text(
            "STARK INDUSTRIES // MARK II",
            color = HudTextDim,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 20.dp, bottom = 8.dp)
        )
    }
}

@Composable
private fun HudPillText(
    text: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    OutlinedButton(
        onClick = { onClick?.invoke() },
        shape = RoundedCornerShape(20.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = HudCyan),
        border = androidx.compose.foundation.BorderStroke(1.dp, HudCyanDim),
        modifier = modifier
    ) {
        Text(text, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
    }
}

private fun currentTimeHHMMSS(): String {
    val now = java.time.LocalTime.now()
    return String.format("%02d:%02d:%02d", now.hour, now.minute, now.second)
}

private fun currentDateLabel(): String {
    val now = java.time.LocalDate.now()
    val formatter = java.time.format.DateTimeFormatter.ofPattern("EEEE, dd MMM yyyy")
    return now.format(formatter).uppercase()
}
