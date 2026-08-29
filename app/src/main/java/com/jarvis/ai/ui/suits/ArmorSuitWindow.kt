package com.jarvis.ai.ui.suits

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jarvis.ai.controller.ArmorController
import com.jarvis.ai.data.repository.SuitRepository

@Composable
fun ArmorSuitWindow() {
    val currentSuit by ArmorController.currentSuit.collectAsState()
    val suitList = remember { SuitRepository().getAllSuits() }

    val animatedBorderColor by animateColorAsState(
        targetValue = currentSuit.arcReactorColor,
        animationSpec = tween(durationMillis = 800),
        label = "SuitGlow"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF0D1117), Color(0xFF161B22))
                ),
                shape = RoundedCornerShape(16.dp)
            )
            .border(2.dp, animatedBorderColor, RoundedCornerShape(16.dp))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "SYSTEM ARMOR: ${currentSuit.name.uppercase()}",
            color = currentSuit.arcReactorColor,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                .border(1.dp, currentSuit.primaryColor, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "MODE: ${currentSuit.systemMode}",
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = currentSuit.description,
                    color = Color.Gray,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(suitList) { suit ->
                val isSelected = suit.id == currentSuit.id
                Box(
                    modifier = Modifier
                        .size(width = 100.dp, height = 50.dp)
                        .background(
                            color = if (isSelected) suit.primaryColor else Color(0xFF21262D),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .border(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) suit.arcReactorColor else Color.DarkGray,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .clickable { ArmorController.equipSuit(suit) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = suit.mark.name.replace("_", " "),
                        color = if (isSelected) Color.White else Color.Gray,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
