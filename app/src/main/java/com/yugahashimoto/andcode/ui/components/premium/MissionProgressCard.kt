package com.yugahashimoto.andcode.ui.components.premium

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Terminal
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yugahashimoto.andcode.core.mission.MissionProgress
import com.yugahashimoto.andcode.core.mission.MissionStatus
import com.yugahashimoto.andcode.ui.gestures.premium.rememberPremiumHaptics
import com.yugahashimoto.andcode.ui.theme.premium.LocalReducedMotion
import com.yugahashimoto.andcode.ui.theme.premium.PremiumColorValues
import com.yugahashimoto.andcode.ui.theme.premium.PremiumTokens
import com.yugahashimoto.andcode.ui.theme.premium.glassPanel
import com.yugahashimoto.andcode.ui.theme.premium.glowShadowBreathing

/**
 * Premium glass mission progress banner card.
 *
 * Visual specs (from reference video):
 * - Glass-morphism card surface with deep space backdrop ([glassPanel])
 * - Neon gradient border or subtle glass border with 20dp corner radius
 * - Header row with glowing leading node, title ("Mission in progress"), and status percentage
 * - Animated gradient progress bar (Neon Blue -> Neon Violet -> Electric Purple)
 * - Expandable details section with [GeneratingProgressSteps]
 *
 * @param title Card header title.
 * @param statusText Right-side status text (e.g. "35.92% completed", "Mission Started").
 * @param progress Overall progress fraction from 0.0f to 1.0f.
 * @param modifier Layout modifier.
 * @param steps Optional list of progress steps to show in expandable section.
 * @param isInitiallyExpanded Whether step breakdown is expanded by default.
 */
@Composable
fun MissionProgressCard(
    title: String,
    statusText: String,
    progress: Float,
    modifier: Modifier = Modifier,
    steps: List<ProgressStepData> = emptyList(),
    isInitiallyExpanded: Boolean = false,
) {
    val reducedMotion = LocalReducedMotion.current
    var isExpanded by remember { mutableStateOf(isInitiallyExpanded) }

    val clampedProgress = progress.coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = clampedProgress,
        animationSpec = if (reducedMotion) {
            tween(0)
        } else {
            spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessLow,
            )
        },
        label = "overallMissionProgress",
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .glassPanel(
                shape = RoundedCornerShape(PremiumTokens.RadiusLarge),
                fillColor = PremiumColorValues.SpaceCard.copy(alpha = 0.85f),
                borderColor = PremiumColorValues.GlassBorder,
                borderWidth = 1.dp,
            )
            .glowShadowBreathing(
                glowColor = PremiumColorValues.NeonBlueGlow,
                baseRadius = PremiumTokens.GlowRadius,
                pulseRadius = 8.dp,
                reducedMotion = reducedMotion,
            )
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Header Row: Left icon + Title, Right status label + expand toggle
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = steps.isNotEmpty()) {
                    isExpanded = !isExpanded
                },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                // Leading glowing pulse indicator
                MissionPulseIcon(isRunning = clampedProgress > 0f && clampedProgress < 1f)

                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                    ),
                    color = PremiumColorValues.ForegroundPrimary,
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontSize = 12.sp,
                    ),
                    color = PremiumColorValues.ForegroundMuted,
                )

                if (steps.isNotEmpty()) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (isExpanded) "Collapse steps" else "Expand steps",
                        tint = PremiumColorValues.ForegroundMuted,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }

        // Animated horizontal gradient progress bar
        MissionAnimatedProgressBar(
            progress = animatedProgress,
            isActive = clampedProgress > 0f && clampedProgress < 1f,
        )

        // Expandable step timeline
        if (steps.isNotEmpty()) {
            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn(animationSpec = tween(PremiumTokens.DurationFast)) +
                        expandVertically(animationSpec = tween(PremiumTokens.DurationMedium)),
                exit = fadeOut(animationSpec = tween(PremiumTokens.DurationFast)) +
                        shrinkVertically(animationSpec = tween(PremiumTokens.DurationMedium)),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                ) {
                    Text(
                        text = "Generating progress",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Medium,
                            fontSize = 13.sp,
                        ),
                        color = PremiumColorValues.ForegroundMuted,
                        modifier = Modifier.padding(bottom = 12.dp),
                    )

                    GeneratingProgressSteps(steps = steps)
                }
            }
        }
    }
}

/**
 * Animated gradient progress bar spanning the width of the card.
 */
@Composable
private fun MissionAnimatedProgressBar(
    progress: Float,
    isActive: Boolean,
    modifier: Modifier = Modifier,
    height: Dp = 7.dp,
) {
    val reducedMotion = LocalReducedMotion.current

    val infiniteTransition = rememberInfiniteTransition(label = "missionProgressShimmer")
    val shimmerX by if (reducedMotion || !isActive) {
        infiniteTransition.animateFloat(0f, 0f, infiniteRepeatable(tween(Int.MAX_VALUE)), label = "shimmerStatic")
    } else {
        infiniteTransition.animateFloat(
            initialValue = -0.3f,
            targetValue = 1.3f,
            animationSpec = infiniteRepeatable(
                animation = tween(1400, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
            label = "shimmerRunning",
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(PremiumTokens.RadiusPill))
            .background(PremiumColorValues.GlassSurfaceLow)
            .border(
                width = 0.5.dp,
                color = PremiumColorValues.GlassBorder,
                shape = RoundedCornerShape(PremiumTokens.RadiusPill),
            ),
    ) {
        if (progress > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(PremiumTokens.RadiusPill))
                    .background(
                        brush = Brush.horizontalGradient(
                            listOf(
                                PremiumColorValues.NeonBlue,
                                PremiumColorValues.NeonViolet,
                                PremiumColorValues.ElectricPurple,
                            ),
                        ),
                    )
                    .drawBehind {
                        if (isActive && !reducedMotion) {
                            // Glowing leading tip
                            val headX = size.width
                            drawCircle(
                                color = PremiumColorValues.NeonBlueGlow,
                                radius = size.height * 2.2f,
                                center = Offset(headX, size.height / 2f),
                            )

                            // Shimmer sweep across the progress fill
                            val sweepStart = size.width * (shimmerX - 0.25f)
                            val sweepEnd = size.width * (shimmerX + 0.25f)
                            drawRect(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color.White.copy(alpha = 0.35f),
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

/**
 * Glowing circular status node for the header.
 */
@Composable
private fun MissionPulseIcon(
    isRunning: Boolean,
    size: Dp = 22.dp,
) {
    val reducedMotion = LocalReducedMotion.current

    val infiniteTransition = rememberInfiniteTransition(label = "missionIconPulse")
    val pulseAlpha by if (reducedMotion || !isRunning) {
        infiniteTransition.animateFloat(0.9f, 0.9f, infiniteRepeatable(tween(Int.MAX_VALUE)), label = "pulseStatic")
    } else {
        infiniteTransition.animateFloat(
            initialValue = 0.6f,
            targetValue = 1.0f,
            animationSpec = infiniteRepeatable(
                animation = tween(PremiumTokens.DurationXSlow, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "pulseAlpha",
        )
    }

    Box(
        modifier = Modifier
            .size(size)
            .drawBehind {
                if (isRunning && !reducedMotion) {
                    drawCircle(
                        color = PremiumColorValues.NeonBlueGlow,
                        radius = this.size.minDimension * 0.8f * pulseAlpha,
                    )
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(PremiumColorValues.NeonBlueTint)
                .border(1.2.dp, PremiumColorValues.NeonBlue, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Default.Terminal,
                contentDescription = null,
                tint = PremiumColorValues.NeonBlue,
                modifier = Modifier.size(12.dp),
            )
        }
    }
}

/**
 * Overload for [MissionProgressCard] accepting domain [MissionProgress].
 */
@Composable
fun MissionProgressCard(
    missionProgress: MissionProgress,
    modifier: Modifier = Modifier,
    isInitiallyExpanded: Boolean = false,
) {
    val title = when (missionProgress.status) {
        MissionStatus.RUNNING -> "Mission in progress"
        MissionStatus.COMPLETED -> "Mission Completed"
        MissionStatus.FAILED -> "Mission Failed"
        MissionStatus.PAUSED -> "Mission Paused"
        MissionStatus.CANCELLED -> "Mission Cancelled"
        MissionStatus.PENDING -> "Mission Started"
    }

    val statusText = when (missionProgress.status) {
        MissionStatus.COMPLETED -> "100% completed"
        MissionStatus.FAILED -> "Failed at ${missionProgress.currentStep.displayName}"
        else -> "${missionProgress.progressPercent}% completed"
    }

    val progressFraction = missionProgress.progressPercent / 100f
    val steps = missionProgress.toProgressStepDataList()
    val haptics = rememberPremiumHaptics()

    LaunchedEffect(missionProgress.status) {
        when (missionProgress.status) {
            MissionStatus.COMPLETED -> haptics.triggerSuccess()
            MissionStatus.FAILED -> haptics.triggerLongPress()
            else -> {}
        }
    }

    MissionProgressCard(
        title = title,
        statusText = statusText,
        progress = progressFraction,
        modifier = modifier,
        steps = steps,
        isInitiallyExpanded = isInitiallyExpanded,
    )
}
