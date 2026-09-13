package com.jarvis.ai.ui.suits

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jarvis.ai.controller.ArmorController
import com.jarvis.ai.data.model.ArmorSuit
import com.jarvis.ai.data.repository.SuitRepository
import com.jarvis.ai.ui.hud.HudBootLog
import com.jarvis.ai.ui.hud.HudColors
import com.jarvis.ai.ui.hud.HudCornerFrame
import com.jarvis.ai.ui.hud.HudGridBackground
import com.jarvis.ai.ui.hud.HudProgressBar
import com.jarvis.ai.ui.hud.IconBadge
import com.jarvis.ai.ui.hud.JgButtonOutline
import com.jarvis.ai.ui.hud.JgCard
import com.jarvis.ai.ui.hud.JgCardSelected
import com.jarvis.ai.ui.hud.JgChip
import com.jarvis.ai.ui.hud.ReactorRadar

/**
 * Armor Suit window — built from the SAME card/chip/button language as the
 * rest of the app (bg_jg_card / bg_jg_chip / bg_jg_button_outline, icon-badge
 * headers). Screen-level chrome (grid + corner brackets) matches
 * MainActivity/SettingsActivity; individual cards stay clean/rounded like
 * Usage Stats, Settings, Voice Auth screens — no ticks/grid inside cards.
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
    val bootLines = remember(currentSuit.id) {
        listOf(
            "VERIFYING SUIT LINK... ${currentSuit.name.uppercase()}",
            "CALIBRATING ARC REACTOR... ${"%.2f".format(currentSuit.voicePitch)}Hz SYNC",
            "LOADING PLATE TELEMETRY... MODE ${currentSuit.systemMode}",
            "REPULSOR DIAGNOSTICS... NOMINAL"
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(listOf(Color(0xFF030A12), Color(0xFF061018), Color(0xFF030A12)))
            )
    ) {
        HudGridBackground(modifier = Modifier.fillMaxSize(), alpha = 0.18f)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp)
        ) {
            // Header: icon badge + two-tone title + subtitle, chip top-right
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconBadge(accent = glow, size = 52.dp) {
                    Text(text = "🛡", fontSize = 22.sp)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row {
                        Text(
                            text = "ARMOR ",
                            color = glow,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "ARCHIVE",
                            color = Color.White,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Text(
                        text = "Suit link & deployment system",
                        color = HudColors.TextDim,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
                JgChip(accent = glow) {
                    Text(
                        text = currentSuit.mark.name.replace('_', ' '),
                        color = glow,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Main suit card
            JgCard(modifier = Modifier.fillMaxWidth(), accent = glow) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        ReactorRadar(glow = glow, dim = 168.dp)
                        if (vectorId != 0) {
                            Image(
                                painter = painterResource(id = vectorId),
                                contentDescription = currentSuit.name,
                                modifier = Modifier
                                    .height(144.dp)
                                    .wrapContentSize()
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = currentSuit.name.uppercase(),
                        color = primary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "MODE  ${currentSuit.systemMode}",
                        color = glow,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(modifier = Modifier.size(14.dp).background(Color(currentSuit.primaryColor), CircleShape))
                        Box(modifier = Modifier.size(14.dp).background(Color(currentSuit.secondaryColor), CircleShape))
                        Box(modifier = Modifier.size(14.dp).background(glow, CircleShape))
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = currentSuit.description,
                        color = Color(0xFF9FB3C0),
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Boot-log status card (same pattern as the "Quick Insights" info card)
            JgCard(modifier = Modifier.fillMaxWidth(), accent = glow) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "SYSTEM LOG",
                        color = glow,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    HudBootLog(lines = bootLines, modifier = Modifier.fillMaxWidth(), color = glow)
                    Spacer(modifier = Modifier.height(10.dp))
                    HudProgressBar(
                        label = "SUIT LINK: ${currentSuit.name.uppercase()} — SYNCHRONIZING",
                        modifier = Modifier.fillMaxWidth(),
                        color = glow
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "SELECT MARK",
                color = HudColors.Accent,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
            )
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(horizontal = 2.dp)
            ) {
                items(suitList, key = { it.id }) { suit ->
                    SuitChip(suit = suit, selected = suit.id == currentSuit.id) {
                        ArmorController.equipSuit(suit)
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            if (onClose != null) {
                JgButtonOutline(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clickable { onClose() },
                    accent = HudColors.Accent
                ) {
                    Text(
                        text = "←  CLOSE",
                        color = HudColors.Accent,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        HudCornerFrame(modifier = Modifier.fillMaxSize(), accent = HudColors.Accent)
    }
}

@Composable
private fun SuitChip(suit: ArmorSuit, selected: Boolean, onClick: () -> Unit) {
    val context = LocalContext.current
    val id = remember(suit.vectorResName) {
        context.resources.getIdentifier(suit.vectorResName, "drawable", context.packageName)
    }
    val r = Color(suit.arcReactorColor)

    @Composable
    fun chipContent() {
        Column(
            modifier = Modifier
                .clickable { onClick() }
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (id != 0) {
                Image(
                    painter = painterResource(id = id),
                    contentDescription = suit.name,
                    modifier = Modifier.height(56.dp)
                )
            } else {
                Box(modifier = Modifier.size(12.dp).background(r, CircleShape))
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = suit.mark.name.replace('_', ' '),
                color = if (selected) Color.White else Color(0xFF8B9CAB),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                textAlign = TextAlign.Center
            )
        }
    }

    if (selected) {
        JgCardSelected(modifier = Modifier.width(100.dp), accent = r, cornerRadius = 14.dp) { chipContent() }
    } else {
        JgCard(modifier = Modifier.width(100.dp), accent = Color(0xFF30363D), borderAlpha = 0.5f, cornerRadius = 14.dp) { chipContent() }
    }
}
