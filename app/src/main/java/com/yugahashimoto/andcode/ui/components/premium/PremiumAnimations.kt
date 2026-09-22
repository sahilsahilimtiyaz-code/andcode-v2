package com.yugahashimoto.andcode.ui.components.premium

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlin.math.max
import kotlin.math.min
import com.yugahashimoto.andcode.ui.theme.premium.PremiumColorValues
import com.yugahashimoto.andcode.ui.theme.premium.PremiumTokens

/**
 * Breathing glow effect for active/latest message
 */
@Composable
fun BreathingGlow(
    modifier: Modifier = Modifier,
    color: Color = PremiumColorValues.NEON_BLUE,
    isActive: Boolean = true,
) {
    if (!isActive) return

    val infiniteTransition = rememberInfiniteTransition(label = "breathingGlow")

    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "glowAlpha",
    )

    val glowRadius by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "glowRadius",
    )

    Box(
        modifier = modifier
            .drawBehind {
                val maxRadius = PremiumTokens.GLOW_RADIUS.toPx() * glowRadius
                val glowColor = color.copy(alpha = 0.15f * glowAlpha)
                drawCircle(
                    color = glowColor,
                    center = Offset(size.width / 2f, size.height / 2f),
                    radius = max(maxRadius, size.maxDimension / 2f + 20.dp.toPx()),
                )
            },
    )
}

/**
 * Layered shadow card with depth
 */
@Composable
fun LayeredShadowCard(
    modifier: Modifier = Modifier,
    elevationLevel: Int = 1, // 1-4
    content: @Composable () -> Unit,
) {
    val elevationColors = listOf(
        PremiumTokens.DURATION_FAST to Color.Black.copy(alpha = 0.05f),
        PremiumTokens.DURATION_MEDIUM to Color.Black.copy(alpha = 0.08f),
        PremiumTokens.DURATION_SLOW to Color.Black.copy(alpha = 0.12f),
        PremiumTokens.DURATION_XSLOW to Color.Black.copy(alpha = 0.15f),
    )

    val shadowColor = elevationColors[min(elevationLevel - 1, elevationColors.lastIndex)].second

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(PremiumTokens.RADIUS_LARGE))
            .drawBehind {
                // Layered shadows for depth
                val largeRadius = PremiumTokens.RADIUS_LARGE.toPx()
                val cornerRadius = CornerRadius(largeRadius, largeRadius)
                repeat(elevationLevel) { i ->
                    val offset = (i + 1) * 4
                    val alpha = 0.03f + i * 0.02f
                    drawRoundRect(
                        color = shadowColor.copy(alpha = alpha),
                        topLeft = Offset(offset.dp.toPx(), offset.dp.toPx()),
                        size = Size(size.width, size.height),
                        cornerRadius = cornerRadius,
                    )
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        androidx.compose.material3.Surface(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(PremiumTokens.RADIUS_LARGE)),
            color = androidx.compose.material3.MaterialTheme.colorScheme.surface,
            tonalElevation = 0.dp,
        ) {
            content()
        }
    }
}

/**
 * Premium button with micro-interactions
 */
@Composable
fun PremiumButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: androidx.compose.material3.ButtonColors = ButtonDefaults.buttonColors(),
    contentPadding: androidx.compose.foundation.layout.PaddingValues = ButtonDefaults.ContentPadding,
    content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit,
) {
    val pressScale = remember { mutableStateOf(1f) }
    val buttonContent = content

    Button(
        onClick = {
            if (enabled) onClick()
        },
        modifier = modifier
            .graphicsLayer {
                scaleX = pressScale.value
                scaleY = pressScale.value
            }
            .pointerInput(enabled) {
                detectTapGestures(
                    onPress = {
                        pressScale.value = 0.98f
                        try {
                            awaitRelease()
                        } finally {
                            pressScale.value = 1f
                        }
                    },
                )
            },
        enabled = enabled,
        colors = colors,
        contentPadding = contentPadding,
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    color = if (enabled) PremiumColorValues.NEON_BLUE.copy(alpha = 0.08f) else Color.Transparent,
                    shape = RoundedCornerShape(PremiumTokens.RADIUS_PILL),
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            buttonContent()
        }
    }
}
