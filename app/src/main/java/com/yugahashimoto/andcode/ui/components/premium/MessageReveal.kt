package com.yugahashimoto.andcode.ui.components.premium

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import com.yugahashimoto.andcode.ui.theme.premium.LocalReducedMotion
import com.yugahashimoto.andcode.ui.theme.premium.PremiumTokens

/**
 * Message Reveal animation wrapper.
 *
 * Wraps any composable (typically a chat message bubble) and plays a one-shot
 * entrance animation when [visible] transitions to `true`:
 *
 *   1. **Fade in**     : alpha 0 → 1 over [PremiumTokens.DurationMedium]
 *   2. **Slide up**    : 12dp downward offset → 0 (emphasised spring)
 *   3. **Scale**       : 0.96 → 1.0 with a bouncy spring
 *   4. **Shimmer sweep**: one-shot white highlight that crosses left → right
 *                         over [PremiumTokens.ShimmerSweep] ms
 *
 * All animations are disabled (immediate full display) when
 * [LocalReducedMotion] is `true`.
 *
 * Usage:
 * ```kotlin
 * MessageReveal(visible = message.isNew) {
 *     MessageBubble(message)
 * }
 * ```
 *
 * @param visible       Drives the reveal. Set to `true` once to trigger.
 * @param modifier      Layout modifier applied to the outer container.
 * @param slideOffsetPx Vertical slide distance in pixels (positive = starts below).
 *                      Defaults to 36 (≈ 12dp at hdpi).
 * @param content       The composable to reveal.
 */
@Composable
fun MessageReveal(
    visible: Boolean,
    modifier: Modifier = Modifier,
    slideOffsetPx: Int = 36,
    content: @Composable () -> Unit,
) {
    val reducedMotion = LocalReducedMotion.current

    // ── Shimmer state ──────────────────────────────────────────────────────
    var shimmerTriggered by remember { mutableStateOf(false) }
    var shimmerProgress by remember { mutableStateOf(-0.4f) }

    // ── Scale spring ───────────────────────────────────────────────────────
    val scale by animateFloatAsState(
        targetValue = if (visible || reducedMotion) 1f else 0.96f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow,
        ),
        label = "msgScale",
    )

    // ── Fire shimmer once when visible becomes true ────────────────────────
    LaunchedEffect(visible) {
        if (visible && !reducedMotion) {
            // Delay shimmer until the slide + fade are well underway
            kotlinx.coroutines.delay(PremiumTokens.DurationFast.toLong())
            shimmerTriggered = true
        }
    }

    // Animate shimmer X offset: -0.4 → 1.4 (sweeps fully across)
    val shimmerX by animateFloatAsState(
        targetValue = if (shimmerTriggered) 1.4f else -0.4f,
        animationSpec = tween(
            durationMillis = PremiumTokens.ShimmerSweep,
            easing = PremiumTokens.EasingDecelerate,
        ),
        label = "shimmerX",
    )

    if (reducedMotion) {
        // Instant display, no animation
        Box(modifier = modifier.fillMaxWidth()) {
            content()
        }
    } else {
        AnimatedVisibility(
            visible = visible,
            modifier = modifier.fillMaxWidth(),
            enter = fadeIn(
                animationSpec = tween(
                    durationMillis = PremiumTokens.DurationMedium,
                    easing = PremiumTokens.EasingDecelerate,
                ),
            ) + slideInVertically(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow,
                ),
                initialOffsetY = { slideOffsetPx },
            ),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0f, 1f)
                    }
                    // One-shot shimmer sweep overlay drawn on top of content
                    .drawWithContent {
                        drawContent()
                        if (shimmerTriggered) {
                            val shimmerBrush = Brush.linearGradient(
                                colorStops = arrayOf(
                                    0.0f to Color.Transparent,
                                    0.4f to Color.White.copy(alpha = 0.07f),
                                    0.5f to Color.White.copy(alpha = 0.12f),
                                    0.6f to Color.White.copy(alpha = 0.07f),
                                    1.0f to Color.Transparent,
                                ),
                                start = Offset(x = size.width * (shimmerX - 0.2f), y = 0f),
                                end = Offset(x = size.width * (shimmerX + 0.2f), y = size.height),
                                tileMode = TileMode.Clamp,
                            )
                            drawRect(brush = shimmerBrush)
                        }
                    },
            ) {
                content()
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Standalone shimmer-only modifier (reusable for cards / panels)
// ─────────────────────────────────────────────────────────────────────────────

/**
 * A celebration shimmer that plays once when [trigger] becomes `true`.
 *
 * Sweeps a bright highlight across the composable left to right.  Useful for
 * "Build Successful" cards and other one-shot celebration moments.
 *
 * @param trigger      When this flips to `true` the shimmer runs once.
 * @param highlightColor Colour of the highlight. Defaults to semi-transparent white.
 * @param reducedMotion  When `true` no shimmer is drawn.
 */
fun Modifier.celebrationShimmer(
    trigger: Boolean,
    highlightColor: Color = Color.White.copy(alpha = 0.14f),
    reducedMotion: Boolean = false,
): Modifier =
    if (!trigger || reducedMotion) this
    else drawWithContent {
        drawContent()
        val shimmerBrush = Brush.linearGradient(
            colorStops = arrayOf(
                0.0f to Color.Transparent,
                0.45f to highlightColor,
                0.55f to highlightColor,
                1.0f to Color.Transparent,
            ),
            start = Offset(0f, 0f),
            end = Offset(size.width, size.height),
        )
        drawRect(brush = shimmerBrush)
    }
