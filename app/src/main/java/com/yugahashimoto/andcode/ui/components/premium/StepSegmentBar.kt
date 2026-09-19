package com.yugahashimoto.andcode.ui.components.premium

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.yugahashimoto.andcode.ui.theme.premium.LocalReducedMotion
import com.yugahashimoto.andcode.ui.theme.premium.PremiumColorValues
import com.yugahashimoto.andcode.ui.theme.premium.PremiumTokens

/**
 * 4-segment gradient progress bar for mission steps.
 *
 * Each step in the mission progress view displays a segmented bar divided into
 * [segmentCount] segments (default 4). Each segment fills independently to represent
 * granular sub-progress within the step.
 *
 * Visual specs:
 * - Empty segments: Subdued glass surface ([PremiumColorValues.GlassSurface])
 * - Filled segments: Vivid Neon Blue -> Electric Purple gradient
 * - Active filling segment: Animated fill width with leading glow and optional shimmer
 *
 * @param progress Overall step progress from 0.0f to 1.0f.
 * @param modifier Layout modifier.
 * @param segmentCount Number of segments (default 4 as per reference design).
 * @param height Height of the segment pills.
 * @param spacing Spacing between adjacent segments.
 * @param shape Corner radius shape for each segment pill.
 * @param gradientColors Color stops for the filled gradient.
 */
@Composable
fun StepSegmentBar(
    progress: Float,
    modifier: Modifier = Modifier,
    segmentCount: Int = 4,
    height: Dp = 6.dp,
    spacing: Dp = 6.dp,
    shape: Shape = RoundedCornerShape(PremiumTokens.RadiusPill),
    gradientColors: List<Color> = listOf(
        PremiumColorValues.NeonBlue,
        PremiumColorValues.NeonViolet,
        PremiumColorValues.ElectricPurple,
    ),
) {
    val reducedMotion = LocalReducedMotion.current
    val clampedProgress = progress.coerceIn(0f, 1f)

    // Animated shimmer for actively progressing bar
    val infiniteTransition = rememberInfiniteTransition(label = "segmentShimmer")
    val shimmerX by if (reducedMotion || clampedProgress >= 1f || clampedProgress <= 0f) {
        infiniteTransition.animateFloat(0f, 0f, infiniteRepeatable(tween(Int.MAX_VALUE)), label = "shimmerStatic")
    } else {
        infiniteTransition.animateFloat(
            initialValue = -0.5f,
            targetValue = 1.5f,
            animationSpec = infiniteRepeatable(
                animation = tween(1200, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
            label = "shimmerActive",
        )
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(height),
        horizontalArrangement = Arrangement.spacedBy(spacing),
    ) {
        val segmentFraction = 1f / segmentCount

        for (i in 0 until segmentCount) {
            val segmentStart = i * segmentFraction
            val segmentEnd = (i + 1) * segmentFraction
            val rawSegmentProgress = when {
                clampedProgress >= segmentEnd -> 1f
                clampedProgress <= segmentStart -> 0f
                else -> (clampedProgress - segmentStart) / segmentFraction
            }

            val animatedSegmentProgress by animateFloatAsState(
                targetValue = rawSegmentProgress,
                animationSpec = if (reducedMotion) tween(0) else PremiumTokens.tweenFast(),
                label = "segmentProgress_$i",
            )

            val isActive = rawSegmentProgress > 0f && rawSegmentProgress < 1f

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(shape)
                    .background(PremiumColorValues.GlassSurface),
            ) {
                if (animatedSegmentProgress > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(animatedSegmentProgress)
                            .fillMaxHeight()
                            .clip(shape)
                            .background(
                                brush = Brush.horizontalGradient(gradientColors),
                            )
                            .drawBehind {
                                if (isActive && !reducedMotion) {
                                    // Leading edge glow
                                    val headX = size.width
                                    drawCircle(
                                        color = PremiumColorValues.NeonBlueGlow,
                                        radius = size.height * 1.5f,
                                        center = Offset(headX, size.height / 2f),
                                    )

                                    // Subtle shimmer sweep
                                    val sweepStart = size.width * (shimmerX - 0.3f)
                                    val sweepEnd = size.width * (shimmerX + 0.3f)
                                    drawRect(
                                        brush = Brush.horizontalGradient(
                                            colors = listOf(
                                                Color.Transparent,
                                                Color.White.copy(alpha = 0.25f),
                                                Color.Transparent,
                                            ),
                                            startX = sweepStart,
                                            endX = sweepEnd,
                                        ),
                                    )
                                }
                            },
                    )
                }
            }
        }
    }
}
