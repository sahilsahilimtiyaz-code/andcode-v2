package com.yugahashimoto.andcode.ui.gestures.premium

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
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
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Two-finger project switch navigation gesture wrapper.
 *
 * Detects 2-finger horizontal drag to switch between active projects/workspaces:
 * - Swipe left (2 fingers): Switch to Next Project
 * - Swipe right (2 fingers): Switch to Previous Project
 * - Displays a floating frosted glass HUD card during the gesture
 * - Haptic feedback ticks when threshold is crossed
 * - Spring physics settle on release
 *
 * @param onSwitchPrevious Callback to switch to previous workspace/project.
 * @param onSwitchNext Callback to switch to next workspace/project.
 * @param modifier Layout modifier.
 * @param enabled Whether gesture detection is enabled.
 * @param threshold Drag threshold in Dp to confirm switch (default: 90.dp).
 * @param previousProjectName Optional label of previous project for the HUD preview.
 * @param nextProjectName Optional label of next project for the HUD preview.
 * @param content Target screen content.
 */
@Composable
fun TwoFingerProjectSwitchContainer(
    onSwitchPrevious: () -> Unit,
    onSwitchNext: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    threshold: Dp = 90.dp,
    previousProjectName: String? = null,
    nextProjectName: String? = null,
    content: @Composable () -> Unit,
) {
    val haptics = rememberPremiumHaptics()
    val reducedMotion = LocalReducedMotion.current
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()

    val thresholdPx = with(density) { threshold.toPx() }
    val dragOffsetX = remember { Animatable(0f) }
    var isTwoFingerActive by remember { mutableStateOf(false) }
    var hasTriggeredThresholdHaptic by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput

                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val activePointers = mutableMapOf<PointerId, androidx.compose.ui.geometry.Offset>()
                    activePointers[down.id] = down.position

                    var accumulatedDragX = 0f
                    var twoFingerGestureActive = false

                    while (true) {
                        val event = awaitPointerEvent()
                        val currentPointers = event.changes.filter { !it.isConsumed && it.pressed }

                        if (currentPointers.size >= 2) {
                            twoFingerGestureActive = true
                            isTwoFingerActive = true

                            // Calculate average horizontal delta across all active touches
                            val totalDeltaX = currentPointers.map { it.positionChange().x }.average().toFloat()
                            accumulatedDragX += totalDeltaX
                            currentPointers.forEach { it.consume() }

                            coroutineScope.launch {
                                dragOffsetX.snapTo(accumulatedDragX)
                            }

                            val isPastThreshold = abs(accumulatedDragX) >= thresholdPx
                            if (isPastThreshold && !hasTriggeredThresholdHaptic) {
                                haptics.triggerThreshold()
                                hasTriggeredThresholdHaptic = true
                            } else if (!isPastThreshold && hasTriggeredThresholdHaptic) {
                                hasTriggeredThresholdHaptic = false
                            }
                        } else if (twoFingerGestureActive && currentPointers.size < 2) {
                            // Touch released or lifted
                            val finalOffset = accumulatedDragX
                            twoFingerGestureActive = false
                            isTwoFingerActive = false

                            coroutineScope.launch {
                                if (abs(finalOffset) >= thresholdPx) {
                                    haptics.triggerSuccess()
                                    if (finalOffset > 0) {
                                        onSwitchPrevious()
                                    } else {
                                        onSwitchNext()
                                    }
                                }

                                dragOffsetX.animateTo(
                                    targetValue = 0f,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessMedium,
                                    ),
                                )
                                hasTriggeredThresholdHaptic = false
                            }
                            break
                        }

                        if (event.changes.all { !it.pressed }) {
                            break
                        }
                    }
                }
            },
    ) {
        content()

        // Floating HUD indicator when 2-finger gesture is active
        AnimatedVisibility(
            visible = isTwoFingerActive && abs(dragOffsetX.value) > 10f,
            enter = fadeIn(animationSpec = tween(PremiumTokens.DurationFast)) +
                    scaleIn(animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)),
            exit = fadeOut(animationSpec = tween(PremiumTokens.DurationFast)) +
                    scaleOut(animationSpec = tween(PremiumTokens.DurationFast)),
            modifier = Modifier.align(Alignment.Center),
        ) {
            val offset = dragOffsetX.value
            val isArmed = abs(offset) >= thresholdPx
            val isMovingRight = offset > 0

            val targetName = if (isMovingRight) {
                previousProjectName ?: "Previous Project"
            } else {
                nextProjectName ?: "Next Project"
            }

            TwoFingerSwitchHud(
                isArmed = isArmed,
                isMovingRight = isMovingRight,
                targetProjectName = targetName,
                dragFraction = (abs(offset) / thresholdPx).coerceIn(0f, 1f),
            )
        }
    }
}

/**
 * Floating glass HUD card shown during 2-finger workspace swipe.
 */
@Composable
private fun TwoFingerSwitchHud(
    isArmed: Boolean,
    isMovingRight: Boolean,
    targetProjectName: String,
    dragFraction: Float,
    modifier: Modifier = Modifier,
) {
    val reducedMotion = LocalReducedMotion.current
    val hudShape = RoundedCornerShape(PremiumTokens.RadiusLarge)
    val accentColor = if (isArmed) PremiumColorValues.NeonBlue else PremiumColorValues.ForegroundMuted

    Box(
        modifier = modifier
            .glowShadow(
                glowColor = if (isArmed) PremiumColorValues.NeonBlueGlow else PremiumColorValues.GlassSurface,
                radius = PremiumTokens.GlowRadius,
                reducedMotion = reducedMotion,
            )
            .glassPanel(
                shape = hudShape,
                fillColor = PremiumColorValues.SpaceCard.copy(alpha = 0.92f),
                borderColor = if (isArmed) PremiumColorValues.NeonBlue else PremiumColorValues.GlassBorder,
                borderWidth = 1.2.dp,
            )
            .padding(horizontal = 20.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (isMovingRight) Icons.AutoMirrored.Filled.ArrowBack else Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(20.dp),
                )
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = if (isMovingRight) "Switching to Previous" else "Switching to Next",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                    color = PremiumColorValues.ForegroundHint,
                )

                Text(
                    text = targetProjectName,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                    ),
                    color = if (isArmed) PremiumColorValues.NeonBlue else PremiumColorValues.ForegroundPrimary,
                )
            }
        }
    }
}
