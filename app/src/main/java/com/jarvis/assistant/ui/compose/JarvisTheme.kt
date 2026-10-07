package com.jarvis.assistant.ui.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object JColors {
    val Cyan = Color(0xFF00D9FF)
    val CyanBright = Color(0xFFB8ECFF)
    val CyanDim = Color(0xFF007A99)
    val CyanSoft = Color(0xFF5A8A99)
    val Amber = Color(0xFFFFAA00)
    val Orange = Color(0xFFFF8C00)
    val Red = Color(0xFFFF6B6B)
    val Green = Color(0xFF22C55E)
    val Bg = Color(0xFF03080E)
    val BgDeep = Color(0xFF01040A)
    val Panel = Color(0xFF0A1825)
    val Text = Color(0xFFE8FBFF)
    val TextDim = Color(0xFF8AB4C0)
    val Muted = Color(0xFF2A4A55)
    val GlassTop = Color(0xCC0C1A28)
    val GlassBot = Color(0x99050810)
}

fun glassBrush() = Brush.verticalGradient(listOf(JColors.GlassTop, JColors.GlassBot))
fun bgBrush() = Brush.verticalGradient(listOf(JColors.BgDeep, JColors.Bg, Color(0xFF061018)))

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    corner: Dp = 18.dp,
    borderColor: Color = JColors.Cyan.copy(alpha = 0.55f),
    content: @Composable () -> Unit
) {
    val shape = RoundedCornerShape(corner)
    Box(
        modifier
            .clip(shape)
            .background(glassBrush())
            .border(1.dp, borderColor, shape)
    ) { content() }
}

@Composable
fun GlassButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    filled: Boolean = false
) {
    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier
            .height(48.dp)
            .clip(shape)
            .background(
                if (filled) Brush.horizontalGradient(listOf(JColors.Cyan, Color(0xFF0099BB)))
                else glassBrush()
            )
            .border(1.dp, JColors.Cyan.copy(alpha = 0.55f), shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text,
            color = if (filled) JColors.Bg else JColors.Cyan,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
fun HudTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        modifier = modifier,
        color = JColors.Cyan,
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.Monospace,
        letterSpacing = 1.sp
    )
}
