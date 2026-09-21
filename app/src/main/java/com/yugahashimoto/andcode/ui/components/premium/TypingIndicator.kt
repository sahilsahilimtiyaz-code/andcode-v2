package com.yugahashimoto.andcode.ui.components.premium

import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.yugahashimoto.andcode.ui.theme.premium.LocalReducedMotion
import com.yugahashimoto.andcode.ui.theme.premium.PremiumColorValues
import com.yugahashimoto.andcode.ui.theme.premium.PremiumTokens
import com.yugahashimoto.andcode.ui.theme.premium.rememberReducedMotion
import kotlin.math.max

@Composable
fun TypingIndicator(
    modifier: Modifier = Modifier,
    barCount: Int = 3,
    barWidth: Dp = 6.dp,
    barMaxHeight: Dp = 28.dp,
    spacing: Dp = 8.dp,
    containerPadding: Dp = 16.dp,
) {
    val reducedMotion = rememberReducedMotion()
    val density = LocalDensity.current
    val barMaxHeightPx = with(density) { barMaxHeight.toPx() }

    val infiniteTransition = rememberInfiniteTransition(label = "typingIndicator")

    val bars = (0 until barCount).map { index ->
        val delayMs = index * 150L
        val cycleDuration = 1200

        infiniteTransition.animateFloat(
            initialValue = 0.3f,
            targetValue = 0.3f,
            animationSpec = infiniteRepeatable(
                animation = keyframes {
                    durationMillis = cycleDuration
                    delayMillis = delayMs
                    // Spring-like curve: fast attack, overshoot, settle
                    0.3f at 0 with FastOutSlowInEasing
                    1.0f at (cycleDuration * 0.3) with FastOutSlowInEasing
                    0.3f at cycleDuration with FastOutSlowInEasing
                },
                repeatMode = RepeatMode.Restart,
            ),
            label = "bar_$index",
        )
    }

    val neonBlue = Color(0xFF00D4FF)
    val electricPurple = Color(0xFFB84CFF)

    Box(
        modifier = modifier
            .padding(horizontal = containerPadding, vertical = 8.dp)
            .height(barMaxHeight + 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        // Glass-morphism pill container
        Box(
            modifier = Modifier
                .width((barWidth * barCount + spacing * (barCount - 1)) + containerPadding * 2)
                .height(barMaxHeight + 16.dp)
                .clip(RoundedCornerShape(PremiumTokens.RadiusPill))
                .drawBehind {
                    // Glass background with blur effect approximation
                    val glassColor = Color.White.copy(alpha = 0.08f)
                    val borderColor = Color.White.copy(alpha = 0.15f)
                    drawRoundRect(
                        color = glassColor,
                        size = this.size.toSize(),
                        cornerRadius = PremiumTokens.RadiusPill.toPx(),
                    )
                    drawRoundRect(
                        color = borderColor,
                        size = this.size.toSize(),
                        cornerRadius = PremiumTokens.RadiusPill.toPx(),
                        style = androidx.compose.ui.graphics.Stroke(width = 1.dp.toPx()),
                    )
                    // Subtle inner glow
                    drawRoundRect(
                        color = neonBlue.copy(alpha = 0.03f),
                        size = this.size.toSize(),
                        cornerRadius = PremiumTokens.RadiusPill.toPx(),
                        style = androidx.compose.ui.graphics.Stroke(width = 2.dp.toPx()),
                    )
                },
            contentAlignment = Alignment.Center,
        ) {
            Row(
                modifier = Modifier
                    .width((barWidth * barCount + spacing * (barCount - 1)) + containerPadding * 2)
                    .height(barMaxHeight + 16.dp)
                    .padding(horizontal = containerPadding, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(spacing),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                bars.forEachIndexed { index, barAnim ->
                    val scale by barAnim
                    val currentScale = if (reducedMotion) 1f else max(0.3f, scale)

                    Box(
                        modifier = Modifier
                            .width(barWidth)
                            .height(barMaxHeight)
                            .graphicsLayer {
                                scaleY = currentScale
                                transformOrigin = androidx.compose.ui.graphics.TransformOrigin(
                                    0.5f, 1f
                                )
                            }
                            .clip(RoundedCornerShape(PremiumTokens.RadiusSmall))
                            .drawBehind {
                                // Outer glow shadow
                                val glowColor = electricPurple.copy(alpha = 0.5f * currentScale)
                                drawRoundRect(
                                    color = glowColor,
                                    topLeft = Offset(-6.dp.toPx(), -6.dp.toPx()),
                                    size = Size(
                                        (barWidth + 12.dp).toPx(),
                                        (barMaxHeight + 12.dp).toPx() * currentScale
                                    ),
                                    cornerRadius = PremiumTokens.RadiusSmall.toPx(),
                                )
                                // Inner highlight
                                val highlightColor = neonBlue.copy(alpha = 0.3f * currentScale)
                                drawRoundRect(
                                    color = highlightColor,
                                    topLeft = Offset(-2.dp.toPx(), -2.dp.toPx()),
                                    size = Size(
                                        (barWidth + 4.dp).toPx(),
                                        (barMaxHeight + 4.dp).toPx() * currentScale
                                    ),
                                    cornerRadius = PremiumTokens.RadiusSmall.toPx(),
                                )
                            }
                    ) {
                        // Gradient fill
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    brush = Brush.linearGradient(
                                        colors = listOf(neonBlue, electricPurple),
                                        start = Offset(0f, barMaxHeightPx),
                                        end = Offset(0f, 0f),
                                    ),
                                    shape = RoundedCornerShape(PremiumTokens.RadiusSmall),
                                )
                        )
                    }
                }
            }
        }
    }
}