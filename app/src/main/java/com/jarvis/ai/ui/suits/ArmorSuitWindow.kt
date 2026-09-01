package com.jarvis.ai.ui.suits

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jarvis.ai.controller.ArmorController
import com.jarvis.ai.data.repository.SuitRepository

@Composable
fun ArmorSuitWindow(onClose: (() -> Unit)? = null) {
    val currentSuit by ArmorController.currentSuit.collectAsState()
    val suitList = remember { SuitRepository().getAllSuits() }

    val glow by animateColorAsState(
        targetValue = Color(currentSuit.arcReactorColor),
        animationSpec = tween(700),
        label = "reactorGlow"
    )
    val primary = Color(currentSuit.primaryColor)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF020810))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "J.A.R.V.I.S  ·  SUIT SELECT",
            color = glow,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 2.sp
        )
        Spacer(modifier = Modifier.height(16.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        listOf(Color(0xFF0D1117), Color(0xFF161B22))
                    ),
                    shape = RoundedCornerShape(16.dp)
                )
                .border(2.dp, glow, RoundedCornerShape(16.dp))
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = currentSuit.name.uppercase(),
                color = primary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "MODE  ${currentSuit.systemMode}",
                color = glow,
                fontSize = 13.sp,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .background(Color(currentSuit.primaryColor), CircleShape)
                )
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .background(Color(currentSuit.secondaryColor), CircleShape)
                )
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .background(glow, CircleShape)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = currentSuit.description,
                color = Color(0xFF9FB3C0),
                fontSize = 13.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "VOICE PITCH  ${"%.2f".format(currentSuit.voicePitch)}",
                color = Color(0xFF5A7A8A),
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = "AVAILABLE MARKS",
            color = Color(0xFF00E5FF),
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.fillMaxWidth().padding(start = 4.dp, bottom = 8.dp)
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(horizontal = 2.dp)
        ) {
            items(suitList, key = { it.id }) { suit ->
                val selected = suit.id == currentSuit.id
                val r = Color(suit.arcReactorColor)
                val p = Color(suit.primaryColor)
                Column(
                    modifier = Modifier
                        .width(110.dp)
                        .background(
                            if (selected) p.copy(alpha = 0.35f) else Color(0xFF21262D),
                            RoundedCornerShape(10.dp)
                        )
                        .border(
                            if (selected) 2.dp else 1.dp,
                            if (selected) r else Color(0xFF30363D),
                            RoundedCornerShape(10.dp)
                        )
                        .clickable { ArmorController.equipSuit(suit) }
                        .padding(vertical = 12.dp, horizontal = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(r, CircleShape)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = suit.mark.name.replace('_', ' '),
                        color = if (selected) Color.White else Color(0xFF8B9CAB),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))
        if (onClose != null) {
            Text(
                text = "←  CLOSE",
                color = Color(0xFF00E5FF),
                fontSize = 15.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.clickable { onClose() }.padding(12.dp)
            )
        }
    }
}
