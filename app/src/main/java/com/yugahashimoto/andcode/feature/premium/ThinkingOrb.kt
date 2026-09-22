package com.yugahashimoto.andcode.feature.premium

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun ThinkingOrb(
    modifier: Modifier = Modifier,
    primaryColor: Color = Color(0xFF00D4FF),
    secondaryColor: Color = Color(0xFFB84CFF)
) {
    val infiniteTransition = rememberInfiniteTransition(label = "thinking_orb")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Canvas(modifier = modifier.size(48.dp)) {
        val centerX = size.width / 2
        val centerY = size.height / 2
        val radius = size.width / 3

        // Outer ring
        drawCircle(
            brush = Brush.sweepGradient(
                colors = listOf(primaryColor, secondaryColor, primaryColor)
            ),
            radius = radius * pulse,
            center = Offset(centerX, centerY)
        )

        // Inner glow
        drawCircle(
            color = Color.White.copy(alpha = 0.3f * pulse),
            radius = radius * 0.5f,
            center = Offset(centerX, centerY)
        )

        // Orbiting dots
        for (i in 0..5) {
            val angle = Math.toRadians((rotation + i * 60.0))
            val dotX = centerX + cos(angle).toFloat() * radius * 0.8f
            val dotY = centerY + sin(angle).toFloat() * radius * 0.8f
            drawCircle(
                color = if (i % 2 == 0) primaryColor else secondaryColor,
                radius = 4f,
                center = Offset(dotX, dotY)
            )
        }
    }
}
