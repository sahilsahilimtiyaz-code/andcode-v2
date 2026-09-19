package com.yugahashimoto.andcode.ui.gestures.premium

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yugahashimoto.andcode.ui.theme.premium.LocalReducedMotion
import com.yugahashimoto.andcode.ui.theme.premium.PremiumColorValues
import com.yugahashimoto.andcode.ui.theme.premium.PremiumTokens
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Swipe direction allowed for the action.
 */
enum class SwipeDirection {
    START_TO_END, // Left to Right
    END_TO_START, // Right to Left
    BOTH,
}

/**
 * Swipe to Archive/Delete container wrapper.
 *
 * Provides fluid swipe-to-dismiss gesture with:
 * - Spring physics for tracking and resting settle
 * - Tactile haptic feedback when crossing the trigger threshold
 * - Glass reveal background with glowing icon badge
 * - Bouncy icon scale when armed
 * - Full [LocalReducedMotion] accessibility support
 *
 * @param onArchive Callback executed when swipe crosses threshold and is released.
 * @param modifier Layout modifier.
 * @param enabled Whether gesture is active.
 * @param direction Allowed swipe direction (default: END_TO_START).
 * @param threshold Threshold distance in Dp to trigger archive (default: 80.dp).
 * @param icon Action icon (default: Archive).
 * @param actionLabel Action label text (default: "Archive").
 * @param accentColor Glow and icon accent color (default: NeonAmber).
 * @param shape Corner radius shape for background and content.
 * @param content Composable item content.
 */
@Composable
fun SwipeToArchiveContainer(
    onArchive: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    direction: SwipeDirection = SwipeDirection.END_TO_START,
    threshold: Dp = 80.dp,
    icon: ImageVector = Icons.Default.Archive,
    actionLabel: String = "Archive",
    accentColor: Color = PremiumColorValues.NeonAmber,
    shape: Shape = RoundedCornerShape(PremiumTokens.RadiusLarge),
    content: @Composable () -> Unit,
) {
    val reducedMotion = LocalReducedMotion.current
    val haptics = rememberPremiumHaptics()
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()

    val thresholdPx = with(density) { threshold.toPx() }
    val offsetX = remember { Animatable(0f) }
    var hasTriggeredHaptic by remember { mutableStateOf(false) }
    var isDismissed by remember { mutableStateOf(false) }

    val draggableState = rememberDraggableState { delta ->
        coroutineScope.launch {
            val newOffset = when (direction) {
                SwipeDirection.START_TO_END -> (offsetX.value + delta).coerceAtLeast(0f)
                SwipeDirection.END_TO_START -> (offsetX.value + delta).coerceAtMost(0f)
                SwipeDirection.BOTH -> offsetX.value + delta
            }

            // Haptic feedback tick when crossing threshold
            val isPastThreshold = abs(newOffset) >= thresholdPx
            if (isPastThreshold && !hasTriggeredHaptic) {
                haptics.triggerThreshold()
                hasTriggeredHaptic = true
            } else if (!isPastThreshold && hasTriggeredHaptic) {
                hasTriggeredHaptic = false
            }

            offsetX.snapTo(newOffset)
        }
    }

    AnimatedVisibility(
        visible = !isDismissed,
        exit = fadeOut(animationSpec = tween(PremiumTokens.DurationFast)) +
                shrinkVertically(animationSpec = tween(PremiumTokens.DurationMedium)),
    ) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .clip(shape),
        ) {
            // Background Action Reveal Layer
            val currentOffset = offsetX.value
            val isArmed = abs(currentOffset) >= thresholdPx

            if (abs(currentOffset) > 4f) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    accentColor.copy(alpha = 0.18f),
                                    accentColor.copy(alpha = 0.28f),
                                ),
                            ),
                        )
                        .padding(horizontal = 20.dp),
                    contentAlignment = if (currentOffset < 0) Alignment.CenterEnd else Alignment.CenterStart,
                ) {
                    val iconScale = if (isArmed && !reducedMotion) 1.2f else 1.0f

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.graphicsLayer {
                            scaleX = iconScale
                            scaleY = iconScale
                        },
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(accentColor.copy(alpha = 0.25f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = actionLabel,
                                tint = accentColor,
                                modifier = Modifier.size(20.dp),
                            )
                        }

                        Text(
                            text = actionLabel,
                            color = accentColor,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }

            // Foreground Content Layer with spring drag & settle
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                    .draggable(
                        state = draggableState,
                        orientation = Orientation.Horizontal,
                        enabled = enabled,
                        onDragStopped = {
                            coroutineScope.launch {
                                if (abs(offsetX.value) >= thresholdPx) {
                                    // Settle off-screen or trigger archive
                                    haptics.triggerSuccess()
                                    if (reducedMotion) {
                                        isDismissed = true
                                        onArchive()
                                    } else {
                                        val targetDismissX = if (offsetX.value > 0) 1000f else -1000f
                                        offsetX.animateTo(
                                            targetValue = targetDismissX,
                                            animationSpec = tween(PremiumTokens.DurationFast),
                                        )
                                        isDismissed = true
                                        onArchive()
                                    }
                                } else {
                                    // Snap back with spring
                                    offsetX.animateTo(
                                        targetValue = 0f,
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioMediumBouncy,
                                            stiffness = Spring.StiffnessMedium,
                                        ),
                                    )
                                    hasTriggeredHaptic = false
                                }
                            }
                        },
                    ),
            ) {
                content()
            }
        }
    }
}
