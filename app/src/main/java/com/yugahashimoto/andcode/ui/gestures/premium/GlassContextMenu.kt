package com.yugahashimoto.andcode.ui.gestures.premium

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.yugahashimoto.andcode.ui.theme.premium.LocalReducedMotion
import com.yugahashimoto.andcode.ui.theme.premium.PremiumColorValues
import com.yugahashimoto.andcode.ui.theme.premium.PremiumTokens
import com.yugahashimoto.andcode.ui.theme.premium.glassPanel
import com.yugahashimoto.andcode.ui.theme.premium.glowShadow
import com.yugahashimoto.andcode.ui.theme.premium.neonBorder

/**
 * Action item specification for [GlassContextMenu].
 */
data class ContextMenuAction(
    val id: String,
    val title: String,
    val icon: ImageVector? = null,
    val shortcut: String? = null,
    val isDestructive: Boolean = false,
    val onClick: () -> Unit,
)

/**
 * Glass context menu wrapper.
 *
 * Attaches a long-press gesture to [content]. When triggered:
 * 1. Emits a distinct long-press haptic tick.
 * 2. Displays a floating frosted glass panel with action items.
 * 3. Menu items have glowing neon selection states.
 * 4. Tap outside or select an action smoothly dismisses with spring physics.
 *
 * @param actions List of context menu actions.
 * @param modifier Layout modifier for the target element.
 * @param enabled Whether long-press menu is active.
 * @param content The composable that triggers the context menu on long-press.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GlassContextMenuContainer(
    actions: List<ContextMenuAction>,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    val haptics = rememberPremiumHaptics()
    val reducedMotion = LocalReducedMotion.current
    var isMenuOpen by remember { mutableStateOf(false) }

    val pressedScale by animateFloatAsState(
        targetValue = if (isMenuOpen && !reducedMotion) 0.97f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "targetPressedScale",
    )

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = pressedScale
                scaleY = pressedScale
            }
            .combinedClickable(
                enabled = enabled,
                onLongClick = {
                    haptics.triggerLongPress()
                    isMenuOpen = true
                },
                onClick = {},
            ),
    ) {
        content()
    }

    if (isMenuOpen) {
        Dialog(
            onDismissRequest = { isMenuOpen = false },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                dismissOnBackPress = true,
                dismissOnClickOutside = true,
            ),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(PremiumColorValues.Scrim)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { isMenuOpen = false },
                contentAlignment = Alignment.Center,
            ) {
                GlassContextMenuPanel(
                    actions = actions,
                    onDismiss = { isMenuOpen = false },
                )
            }
        }
    }
}

/**
 * Floating glass menu panel presenting contextual actions.
 */
@Composable
fun GlassContextMenuPanel(
    actions: List<ContextMenuAction>,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = rememberPremiumHaptics()
    val reducedMotion = LocalReducedMotion.current
    val menuShape = RoundedCornerShape(PremiumTokens.RadiusLarge)

    Box(
        modifier = modifier
            .widthIn(min = 220.dp, max = 280.dp)
            .glowShadow(
                glowColor = PremiumColorValues.NeonBlueGlow,
                radius = PremiumTokens.GlowRadius,
                reducedMotion = reducedMotion,
            )
            .glassPanel(
                shape = menuShape,
                fillColor = PremiumColorValues.SpaceCard.copy(alpha = 0.94f),
                borderColor = PremiumColorValues.GlassBorder,
                borderWidth = 1.2.dp,
            )
            .padding(vertical = 8.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
        ) {
            actions.forEachIndexed { index, action ->
                val actionColor = if (action.isDestructive) {
                    PremiumColorValues.NeonRed
                } else {
                    PremiumColorValues.ForegroundPrimary
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            haptics.triggerSelection()
                            action.onClick()
                            onDismiss()
                        }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        if (action.icon != null) {
                            Icon(
                                imageVector = action.icon,
                                contentDescription = null,
                                tint = actionColor,
                                modifier = Modifier.size(18.dp),
                            )
                        }

                        Text(
                            text = action.title,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Medium,
                                fontSize = 14.sp,
                            ),
                            color = actionColor,
                        )
                    }

                    if (!action.shortcut.isNullOrBlank()) {
                        Text(
                            text = action.shortcut,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                            ),
                            color = PremiumColorValues.ForegroundHint,
                        )
                    }
                }

                if (index < actions.lastIndex) {
                    HorizontalDivider(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp),
                        thickness = 0.6.dp,
                        color = PremiumColorValues.GlassBorder,
                    )
                }
            }
        }
    }
}
