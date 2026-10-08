package com.jarvis.assistant.ui.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jarvis.assistant.util.BriefingHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun BriefingScreen(
    onBack: () -> Unit,
    onSpeak: (String) -> Unit
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf("Compiling…") }
    var body by remember { mutableStateOf("Compiling status report, sir…") }
    var score by remember { mutableIntStateOf(0) }
    var pending by remember { mutableIntStateOf(0) }
    var loading by remember { mutableStateOf(true) }
    var locationChip by remember { mutableStateOf("—") }
    var weatherChip by remember { mutableStateOf("—") }
    var tick by remember { mutableIntStateOf(0) }

    LaunchedEffect(tick) {
        loading = true
        body = "Compiling status report, sir…"
        val result = withContext(Dispatchers.IO) {
            try {
                BriefingHelper(context).build(includeWeather = true)
            } catch (e: Exception) {
                BriefingHelper.Briefing(
                    text = "Briefing unavailable: ${e.message}",
                    parts = emptyList()
                )
            }
        }
        title = result.parts.firstOrNull() ?: "Briefing"
        body = result.text
        score = (60 + result.parts.size * 8).coerceAtMost(99)
        pending = 0
        // crude chips from text
        val loc = Regex("""in ([A-Za-z .]+):""").find(result.text)?.groupValues?.getOrNull(1)?.trim()
        locationChip = loc ?: "Local"
        weatherChip = when {
            "Clear" in result.text -> "Clear"
            "Cloud" in result.text -> "Clouds"
            "Rain" in result.text -> "Rain"
            else -> "Weather"
        }
        loading = false
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(bgBrush())
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Header
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .border(1.5.dp, JColors.Cyan.copy(alpha = 0.7f), CircleShape)
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center
            ) {
                Text("←", color = JColors.Cyan, fontSize = 18.sp, fontFamily = FontFamily.Monospace)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row {
                    Text("TODAY'S ", color = JColors.Cyan, fontSize = 18.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    Text("BRIEFING", color = JColors.Text, fontSize = 18.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }
                Text("Your summary for today", color = JColors.CyanSoft, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            }
            GlassCard(corner = 12.dp) {
                Column(
                    Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("✦", color = JColors.Cyan, fontSize = 14.sp)
                    Text("AI Powered", color = JColors.CyanSoft, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // Companion chip
        GlassCard(modifier = Modifier.fillMaxWidth(), borderColor = JColors.Green.copy(alpha = 0.55f)) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("●  COMPANION ONLINE", color = JColors.Green, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, modifier = Modifier.weight(1f))
                Text("›", color = JColors.Green, fontSize = 16.sp)
            }
        }

        Spacer(Modifier.height(12.dp))

        // Main briefing card
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(18.dp)) {
                Text(
                    title.ifBlank { "Good day, sir." },
                    color = JColors.Text,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    body,
                    color = JColors.CyanBright.copy(alpha = 0.92f),
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    lineHeight = 18.sp
                )
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Chip(locationChip, "📍")
                    Chip(weatherChip, "☁")
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // Pending + Score
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            GlassCard(modifier = Modifier.weight(1f)) {
                Column(Modifier.padding(14.dp)) {
                    Text("◷", color = Color(0xFFA78BFA), fontSize = 18.sp)
                    Spacer(Modifier.height(8.dp))
                    Text("PENDING", color = JColors.Cyan, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    Text("$pending", color = JColors.Text, fontSize = 28.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    Text("tasks / reminders", color = JColors.CyanSoft, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                }
            }
            GlassCard(modifier = Modifier.weight(1f)) {
                Column(Modifier.padding(14.dp)) {
                    Text("◎", color = JColors.Green, fontSize = 18.sp)
                    Spacer(Modifier.height(8.dp))
                    Text("SCORE", color = JColors.Cyan, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    Text(if (loading) "…" else "$score%", color = JColors.Text, fontSize = 28.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    Text("system health", color = JColors.CyanSoft, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        GlassButton(
            text = if (loading) "COMPILING…" else "⚡  GENERATE BRIEFING  ›",
            onClick = { if (!loading) tick++ },
            filled = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(10.dp))
        GlassButton(
            text = "🔊  READ ALOUD  ›",
            onClick = {
                if (body.isNotBlank() && !loading) onSpeak(body)
                else if (!loading) {
                    tick++
                }
            },
            filled = false,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(18.dp))
        Text(
            "—  STAY INFORMED  ·  STAY AHEAD  —",
            color = JColors.Muted,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
    }
}

@Composable
private fun Chip(text: String, icon: String) {
    val shape = RoundedCornerShape(20.dp)
    Row(
        Modifier
            .clip(shape)
            .border(1.dp, JColors.Cyan.copy(alpha = 0.45f), shape)
            .background(JColors.Panel.copy(alpha = 0.5f))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("$icon  $text", color = JColors.CyanBright, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
    }
}
