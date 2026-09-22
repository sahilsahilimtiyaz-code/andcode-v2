package com.yugahashimoto.andcode.ui.components.premium

import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.yugahashimoto.andcode.ui.theme.premium.LocalReducedMotion
import com.yugahashimoto.andcode.ui.theme.premium.PremiumColorValues
import com.yugahashimoto.andcode.ui.theme.premium.PremiumTokens

/**
 * Premium typing indicator: 5 glowing dots that pulse sequentially.
 *
 * Animation spec (from reference video):
 *   - Total cycle: [PremiumTokens.TypingPulse] = 600ms
 *   - Each dot staggered by [PremiumTokens.StaggerDot] = 60ms
 *   - Scale: 0.6 → 1.0 with [FastOutSlowInEasing]
 *   - Alpha: 0.4 → 1.0
 *   - Glow radius expands in sync with scale
 *
 * The active dot colour sweeps through the primary gradient (Blue → Purple),
 * giving a "wave" illusion without requiring gradient brushes per dot.
 *
 * Accessibility: when [LocalReducedMotion] is `true`, all dots are rendered
 * static at full scale/alpha (no animation loop).
 *
 * @param modifier      Optional layout modifier.
 * @param dotCount      Number of dots. Defaults to 5 (reference spec).
 * @param dotSize       Diameter of each dot. Defaults to 10dp.
 * @param spacing       Gap between dots. Defaults to 6dp.
 * @param dotColors     List of neon colours per dot (left to right).
 */
@Composable
fun TypingIndicatorDots(
    modifier: Modifier = Modifier,
    dotCount: Int = 5,
    dotSize: Dp = 10.dp,
    spacing: Dp = 6.dp,
    dotColors: List<Color> = defaultDotColors,
) {
    val reducedMotion = LocalReducedMotion.current

    // Clamp colours list to dotCount
    val colors = List(dotCount) { i -> dotColors.getOrElse(i) { PremiumColorValues.NeonBlue } }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(spacing),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        for (i in 0 until dotCount) {
            val (scale, alpha) = if (reducedMotion) {
                1f to 1f
            } else {
                animateDot(index = i, dotCount = dotCount)
            }

            val dotColor = colors[i]
            val glowColor = dotColor.copy(alpha = 0.35f * alpha)

            Box(
                modifier = Modifier
                    .size(dotSize)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        this.alpha = alpha
                    }
                    .drawBehind {
                        // Outer glow ring
                        drawCircle(
                            color = glowColor,
                            radius = size.minDimension / 2f + 6f * scale,
                        )
                    }
                    .clip(CircleShape)
                    .background(color = dotColor.copy(alpha = 0.9f), shape = CircleShape),
            )
        }
    }
}

/**
 * Drives the infinite pulse animation for a single dot at [index].
 *
 * Returns (scale, alpha) as animated [Float] state values.
 */
@Composable
private fun animateDot(index: Int, dotCount: Int): Pair<Float, Float> {
    val totalCycle = PremiumTokens.TypingPulse  // 600ms full wave
    val stagger = PremiumTokens.StaggerDot      // 60ms per dot
    // Each dot starts its peak at a staggered offset within the cycle
    val dotDelay = (index * stagger) % totalCycle
    val rampUp = 150   // ms to reach peak
    val hold = 80      // ms at peak
    val rampDown = 150 // ms back to baseline
    val restDuration = totalCycle - rampUp - hold - rampDown  // remaining rest

    val infiniteTransition = rememberInfiniteTransition(label = "dot_$index")

    val scale by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = totalCycle
                delayMillis = dotDelay
                0.6f at 0 using dotEasing
                1.0f at (rampUp) using dotEasing
                1.0f at (rampUp + hold)
                0.6f at (rampUp + hold + rampDown) using dotEasing
                0.6f at totalCycle
            },
            repeatMode = RepeatMode.Restart,
        ),
        label = "dotScale_$index",
    )

    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.4f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = totalCycle
                delayMillis = dotDelay
                0.4f at 0 using dotEasing
                1.0f at rampUp using dotEasing
                1.0f at (rampUp + hold)
                0.4f at (rampUp + hold + rampDown) using dotEasing
                0.4f at totalCycle
            },
            repeatMode = RepeatMode.Restart,
        ),
        label = "dotAlpha_$index",
    )

    return scale to alpha
}

private val dotEasing: Easing = FastOutSlowInEasing

/** Default 5-dot colour gradient from Neon Blue → Electric Purple. */
private val defaultDotColors = listOf(
    PremiumColorValues.NeonBlue,
    PremiumColorValues.NeonBlueViolet,
    PremiumColorValues.NeonViolet,
    PremiumColorValues.NeonVioletPurple,
    PremiumColorValues.ElectricPurple,
)
