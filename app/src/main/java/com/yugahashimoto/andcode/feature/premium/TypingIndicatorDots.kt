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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun TypingIndicatorDots(
    modifier: Modifier = Modifier,
    dotColor: Color = Color(0xFF00D4FF)
) {
    val infiniteTransition = rememberInfiniteTransition(label = "typing_dots")

    val dot1Alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot1"
    )
    val dot2Alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 400, delayMillis = 150, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot2"
    )
    val dot3Alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 400, delayMillis = 300, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot3"
    )

    Canvas(modifier = modifier.size(48.dp)) {
        val dotSize = 8f
        val spacing = 12f
        val startX = (size.width - (dotSize * 3 + spacing * 2)) / 2
        val centerY = size.height / 2

        drawRoundRect(
            color = dotColor.copy(alpha = dot1Alpha),
            topLeft = Offset(startX, centerY - dotSize / 2),
            size = Size(dotSize, dotSize),
            cornerRadius = CornerRadius(dotSize / 2)
        )
        drawRoundRect(
            color = dotColor.copy(alpha = dot2Alpha),
            topLeft = Offset(startX + dotSize + spacing, centerY - dotSize / 2),
            size = Size(dotSize, dotSize),
            cornerRadius = CornerRadius(dotSize / 2)
        )
        drawRoundRect(
            color = dotColor.copy(alpha = dot3Alpha),
            topLeft = Offset(startX + (dotSize + spacing) * 2, centerY - dotSize / 2),
            size = Size(dotSize, dotSize),
            cornerRadius = CornerRadius(dotSize / 2)
        )
    }
}
