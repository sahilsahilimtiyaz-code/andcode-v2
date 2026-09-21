package com.yugahashimoto.andcode.ui.components.premium

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ripple.RippleTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import com.yugahashimoto.andcode.ui.theme.premium.PremiumColorValues
import com.yugahashimoto.andcode.ui.theme.premium.PremiumTokens

/**
 * Breathing glow effect for active/latest message
 */
@Composable
fun BreathingGlow(
    modifier: Modifier = Modifier,
    color: Color = PremiumColorValues.NeonBlue,
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
                val maxRadius = PremiumTokens.GlowRadius.toPx() * glowRadius
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
        PremiumTokens.DurationFast to Color.Black.copy(alpha = 0.05f),
        PremiumTokens.DurationMedium to Color.Black.copy(alpha = 0.08f),
        PremiumTokens.DurationSlow to Color.Black.copy(alpha = 0.12f),
        PremiumTokens.DurationXSlow to Color.Black.copy(alpha = 0.15f),
    )

    val shadowColor = elevationColors[min(elevationLevel - 1, elevationColors.lastIndex)].second

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(PremiumTokens.RadiusLarge))
            .drawBehind {
                // Layered shadows for depth
                repeat(elevationLevel) { i ->
                    val offset = (i + 1) * 4
                    val alpha = 0.03f + i * 0.02f
                    drawRoundRect(
                        color = shadowColor.copy(alpha = alpha),
                        topLeft = Offset(offset.dp.toPx(), offset.dp.toPx()),
                        size = Size(size.width, size.height),
                        cornerRadius = PremiumTokens.RadiusLarge.toPx(),
                    )
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        androidx.compose.material3.Surface(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(PremiumTokens.RadiusLarge)),
            color = androidx.compose.material3.MaterialTheme.colorScheme.surface,
            tonalElevation = 0.dp,
        ) {
            content()
        }
    }
}

/**
 * Custom ripple theme for premium buttons
 */
object PremiumRippleTheme : RippleTheme {
    @Composable
    override fun defaultColor() = PremiumColorValues.NeonBlue.copy(alpha = 0.12f)

    @Composable
    override fun rippleAlpha() = androidx.compose.material.ripple.RippleAlpha(
        0.1f, 0.15f, 0.1f, 0.15f
    )
}

/**
 * Premium button with micro-interactions
 */
@Composable
fun PremiumButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: androidx.compose.material3.ButtonColors = androidx.compose.material3.ButtonDefaults.buttonColors(),
    contentPadding: androidx.compose.foundation.layout.PaddingValues = androidx.compose.material3.ButtonDefaults.ContentPadding,
    content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit,
) {
    val pressScale = remember { mutableStateOf(1f) }

    androidx.compose.material3.Button(
        onClick = {
            if (enabled) onClick()
        },
        modifier = modifier
            .graphicsLayer {
                scaleX = pressScale.value
                scaleY = pressScale.value
            }
            .pointerInput(enabled) {
                androidx.compose.foundation.gestures.detectTapGestures(
                    onPress = { pressScale.value = 0.98f },
                    onRelease = { pressScale.value = 1f },
                    onTapCancel = { pressScale.value = 1f },
                    onTap = {},
                )
            },
        enabled = enabled,
        colors = colors,
        contentPadding = contentPadding,
    ) {
        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    color = if (enabled) PremiumColorValues.NeonBlue.copy(alpha = 0.08f) else Color.Transparent,
                    shape = RoundedCornerShape(PremiumTokens.RadiusPill),
                ),
            contentAlignment = Alignment.Center,
        ) {
            content()
        }
    }
}