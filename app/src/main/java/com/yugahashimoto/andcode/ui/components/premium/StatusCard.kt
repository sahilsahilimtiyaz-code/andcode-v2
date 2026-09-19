package com.yugahashimoto.andcode.ui.components.premium

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yugahashimoto.andcode.ui.theme.premium.LocalReducedMotion
import com.yugahashimoto.andcode.ui.theme.premium.PremiumColorValues
import com.yugahashimoto.andcode.ui.theme.premium.PremiumTokens
import com.yugahashimoto.andcode.ui.theme.premium.glassPanel
import com.yugahashimoto.andcode.ui.theme.premium.glowShadow
import kotlin.math.roundToInt

/**
 * Visual variant types for [StatusCard].
 */
enum class StatusCardVariant {
    /** Green glow (#00EE88) + celebration shimmer animation */
    SUCCESS,

    /** Red glow (#FF4757) + ±4dp shake animation (200ms) */
    FAILURE,

    /** Amber glow (#FFB84D) + breathing pulse animation */
    WARNING,

    /** Cyan-blue glow (#00D4FF) + steady aura glow */
    INFO,
}

/**
 * Configuration descriptor for a [StatusCardVariant].
 */
private data class VariantVisualConfig(
    val accentColor: Color,
    val glowColor: Color,
    val tintColor: Color,
    val defaultIcon: ImageVector,
    val iconContentDescription: String,
)

private fun getVariantConfig(variant: StatusCardVariant): VariantVisualConfig =
    when (variant) {
        StatusCardVariant.SUCCESS -> VariantVisualConfig(
            accentColor = PremiumColorValues.NeonGreen,
            glowColor = PremiumColorValues.NeonGreenGlow,
            tintColor = PremiumColorValues.NeonGreenTint,
            defaultIcon = Icons.Default.Check,
            iconContentDescription = "Success",
        )
        StatusCardVariant.FAILURE -> VariantVisualConfig(
            accentColor = PremiumColorValues.NeonRed,
            glowColor = PremiumColorValues.NeonRedGlow,
            tintColor = PremiumColorValues.NeonRedTint,
            defaultIcon = Icons.Default.Close,
            iconContentDescription = "Failure",
        )
        StatusCardVariant.WARNING -> VariantVisualConfig(
            accentColor = PremiumColorValues.NeonAmber,
            glowColor = PremiumColorValues.NeonAmberGlow,
            tintColor = PremiumColorValues.NeonAmberTint,
            defaultIcon = Icons.Default.Warning,
            iconContentDescription = "Warning",
        )
        StatusCardVariant.INFO -> VariantVisualConfig(
            accentColor = PremiumColorValues.NeonBlue,
            glowColor = PremiumColorValues.NeonBlueGlow,
            tintColor = PremiumColorValues.NeonBlueTint,
            defaultIcon = Icons.Default.Info,
            iconContentDescription = "Information",
        )
    }

/**
 * Premium Status Card component with 4 visual variants:
 *
 * 1. **SUCCESS**: Emerald neon glow, celebration shimmer sweep, and check badge
 *    (e.g., "Build Successful - Tests passed 24/24").
 * 2. **FAILURE**: Red neon glow and a single ±4dp shake animation (200ms).
 * 3. **WARNING**: Amber neon glow and rhythmic pulse breathing animation.
 * 4. **INFO**: Vivid cyan-blue neon glow and steady illumination.
 *
 * Features:
 * - Glass surface background ([glassPanel]) with 20dp corner radius
 * - Expandable details section with smooth spring-driven enter/exit
 * - Icon badge, title, message/subtitle, optional timestamp or auxiliary action
 * - Zero watermarks (Branding Law enforced)
 * - Complete [LocalReducedMotion] accessibility support
 *
 * @param variant Visual state variant (SUCCESS, FAILURE, WARNING, INFO).
 * @param title Primary headline (e.g. "Build Successful").
 * @param message Secondary descriptive message (e.g. "Tests passed (24/24)").
 * @param modifier Layout modifier.
 * @param timestamp Optional timestamp string (e.g. "3:55 PM").
 * @param icon Optional custom icon override.
 * @param isInitiallyExpanded Initial state of the expandable details section.
 * @param detailsContent Optional composable content displayed when expanded.
 */
@Composable
fun StatusCard(
    variant: StatusCardVariant,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    timestamp: String? = null,
    icon: ImageVector? = null,
    isInitiallyExpanded: Boolean = false,
    detailsContent: (@Composable () -> Unit)? = null,
) {
    val reducedMotion = LocalReducedMotion.current
    val config = remember(variant) { getVariantConfig(variant) }
    var isExpanded by remember { mutableStateOf(isInitiallyExpanded) }

    // ─────────────────────────────────────────────────────────────────────────
    // Motion: Shake animation for FAILURE (±4dp, 200ms)
    // ─────────────────────────────────────────────────────────────────────────
    val density = LocalDensity.current
    val shakeOffsetPx = remember { Animatable(0f) }

    LaunchedEffect(variant) {
        if (variant == StatusCardVariant.FAILURE && !reducedMotion) {
            val shakeAmplitude = with(density) { 4.dp.toPx() }
            shakeOffsetPx.snapTo(0f)
            shakeOffsetPx.animateTo(
                targetValue = 0f,
                animationSpec = keyframes {
                    durationMillis = 200
                    0f at 0
                    -shakeAmplitude at 30 using FastOutSlowInEasing
                    shakeAmplitude at 70 using FastOutSlowInEasing
                    (-shakeAmplitude * 0.7f) at 110 using FastOutSlowInEasing
                    (shakeAmplitude * 0.5f) at 150 using FastOutSlowInEasing
                    0f at 200
                },
            )
        } else {
            shakeOffsetPx.snapTo(0f)
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Motion: Celebration Shimmer for SUCCESS
    // ─────────────────────────────────────────────────────────────────────────
    val infiniteTransition = rememberInfiniteTransition(label = "statusCardMotion")
    val shimmerX by if (reducedMotion || variant != StatusCardVariant.SUCCESS) {
        infiniteTransition.animateFloat(0f, 0f, infiniteRepeatable(tween(Int.MAX_VALUE)), label = "shimmerStatic")
    } else {
        infiniteTransition.animateFloat(
            initialValue = -0.5f,
            targetValue = 1.6f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1800, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
            label = "shimmerSweep",
        )
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Motion: Amber Pulse for WARNING & Gentle Glow for INFO
    // ─────────────────────────────────────────────────────────────────────────
    val pulseAlpha by if (reducedMotion || (variant != StatusCardVariant.WARNING && variant != StatusCardVariant.INFO)) {
        infiniteTransition.animateFloat(1f, 1f, infiniteRepeatable(tween(Int.MAX_VALUE)), label = "pulseStatic")
    } else {
        val duration = if (variant == StatusCardVariant.WARNING) 1000 else 1600
        infiniteTransition.animateFloat(
            initialValue = 0.65f,
            targetValue = 1.0f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = duration, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "variantPulse",
        )
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Layout & Surface
    // ─────────────────────────────────────────────────────────────────────────
    val cardShape = RoundedCornerShape(PremiumTokens.RadiusLarge)

    Box(
        modifier = modifier
            .offset { IntOffset(x = shakeOffsetPx.value.roundToInt(), y = 0) }
            .fillMaxWidth()
            .glowShadow(
                glowColor = config.glowColor.copy(alpha = config.glowColor.alpha * pulseAlpha),
                radius = PremiumTokens.GlowRadius,
                reducedMotion = reducedMotion,
            )
            .clip(cardShape)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        config.tintColor.copy(alpha = 0.22f * pulseAlpha),
                        PremiumColorValues.SpaceCard.copy(alpha = 0.92f),
                    ),
                ),
            )
            .border(
                width = 1.2.dp,
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        config.accentColor.copy(alpha = 0.65f * pulseAlpha),
                        config.accentColor.copy(alpha = 0.20f),
                        PremiumColorValues.GlassBorder,
                    ),
                ),
                shape = cardShape,
            )
            .drawWithContent {
                drawContent()

                // Draw celebration shimmer sweep over SUCCESS card
                if (variant == StatusCardVariant.SUCCESS && !reducedMotion) {
                    val sweepStart = size.width * (shimmerX - 0.25f)
                    val sweepEnd = size.width * (shimmerX + 0.25f)
                    val shimmerBrush = Brush.linearGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.White.copy(alpha = 0.12f),
                            PremiumColorValues.NeonGreen.copy(alpha = 0.18f),
                            Color.White.copy(alpha = 0.12f),
                            Color.Transparent,
                        ),
                        start = Offset(sweepStart, 0f),
                        end = Offset(sweepEnd, size.height),
                        tileMode = TileMode.Clamp,
                    )
                    drawRect(brush = shimmerBrush)
                }
            }
            .padding(16.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Header Row: Badge Icon + Texts + Timestamp / Arrow
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = detailsContent != null) {
                        isExpanded = !isExpanded
                    },
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Circular status icon badge
                StatusBadgeIcon(
                    config = config,
                    iconOverride = icon,
                    variant = variant,
                    pulseAlpha = pulseAlpha,
                )

                // Title & Message column
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                        ),
                        color = PremiumColorValues.ForegroundPrimary,
                    )

                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 13.sp,
                        ),
                        color = PremiumColorValues.ForegroundMuted,
                    )
                }

                // Timestamp and optional expand toggle
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    if (!timestamp.isNullOrBlank()) {
                        Text(
                            text = timestamp,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 11.sp,
                            ),
                            color = PremiumColorValues.ForegroundHint,
                        )
                    }

                    if (detailsContent != null) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = if (isExpanded) "Collapse details" else "Expand details",
                            tint = PremiumColorValues.ForegroundMuted,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }

            // Expandable details section
            if (detailsContent != null) {
                AnimatedVisibility(
                    visible = isExpanded,
                    enter = fadeIn(
                        animationSpec = tween(
                            durationMillis = PremiumTokens.DurationFast,
                            easing = PremiumTokens.EasingDecelerate,
                        ),
                    ) + expandVertically(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow,
                        ),
                    ),
                    exit = fadeOut(
                        animationSpec = tween(
                            durationMillis = PremiumTokens.DurationFast,
                            easing = PremiumTokens.EasingAccelerate,
                        ),
                    ) + shrinkVertically(
                        animationSpec = tween(
                            durationMillis = PremiumTokens.DurationMedium,
                            easing = PremiumTokens.EasingEmphasized,
                        ),
                    ),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                    ) {
                        HorizontalDivider(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp),
                            thickness = 0.8.dp,
                            color = PremiumColorValues.GlassBorder,
                        )

                        detailsContent()
                    }
                }
            }
        }
    }
}

/**
 * Glowing circular badge icon for the StatusCard header.
 */
@Composable
private fun StatusBadgeIcon(
    config: VariantVisualConfig,
    iconOverride: ImageVector?,
    variant: StatusCardVariant,
    pulseAlpha: Float,
    size: Dp = 38.dp,
) {
    val reducedMotion = LocalReducedMotion.current

    Box(
        modifier = Modifier
            .size(size)
            .drawBehind {
                if (!reducedMotion) {
                    drawCircle(
                        color = config.glowColor.copy(alpha = config.glowColor.alpha * pulseAlpha),
                        radius = size.toPx() * 0.75f,
                    )
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(config.accentColor.copy(alpha = if (variant == StatusCardVariant.SUCCESS) 0.95f else 0.25f))
                .border(
                    width = 1.5.dp,
                    color = config.accentColor,
                    shape = CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = iconOverride ?: config.defaultIcon,
                contentDescription = config.iconContentDescription,
                tint = if (variant == StatusCardVariant.SUCCESS) {
                    PremiumColorValues.SpaceBlack
                } else {
                    config.accentColor
                },
                modifier = Modifier.size(20.dp),
            )
        }
    }
}
