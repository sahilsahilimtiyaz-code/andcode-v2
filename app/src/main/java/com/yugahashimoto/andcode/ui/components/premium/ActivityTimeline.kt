package com.yugahashimoto.andcode.ui.components.premium

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Timeline
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yugahashimoto.andcode.ui.theme.premium.LocalReducedMotion
import com.yugahashimoto.andcode.ui.theme.premium.PremiumColorValues
import com.yugahashimoto.andcode.ui.theme.premium.PremiumTokens
import com.yugahashimoto.andcode.ui.theme.premium.glassPanel
import kotlinx.coroutines.delay

/**
 * Execution status for an activity timeline entry.
 */
enum class TimelineItemStatus {
    COMPLETED,
    IN_PROGRESS,
    PENDING,
    FAILED,
}

/**
 * Data model representing an event or task in the [ActivityTimeline].
 */
data class TimelineEvent(
    val id: String,
    val title: String,
    val description: String? = null,
    val status: TimelineItemStatus = TimelineItemStatus.COMPLETED,
    val timestamp: String? = null,
    val codeSnippet: String? = null,
    val durationMs: Long? = null,
)

/**
 * Premium Activity Timeline component.
 *
 * Features (matching reference video & specifications):
 * 1. **Animated Checkmarks**: Self-drawing stroke-trim animation using [PathMeasure]
 *    that dynamically renders the checkmark on completion.
 * 2. **Animated Connector Lines**: Vertical connectors with animated top-to-bottom
 *    gradient fill ([PremiumColorValues.NeonGreen] to [PremiumColorValues.NeonBlue]).
 * 3. **Staggered Reveal Animations**: Sequential entry transition with 60ms stagger
 *    ([PremiumTokens.StaggerItem]), spring slide-up, and fade-in.
 * 4. **Glass Panel Container**: Polished frosted glass surface with deep space backdrop.
 * 5. **Accessibility & Branding Law**: Full [LocalReducedMotion] support and zero watermarks.
 *
 * @param events List of timeline events to display.
 * @param modifier Layout modifier.
 * @param title Header title (e.g. "Activity Timeline" or "Activity Operations").
 * @param isInitiallyExpanded Whether timeline list is expanded by default.
 * @param onEventClick Optional click listener for individual events.
 */
@Composable
fun ActivityTimeline(
    events: List<TimelineEvent>,
    modifier: Modifier = Modifier,
    title: String = "Activity Operations",
    isInitiallyExpanded: Boolean = true,
    onEventClick: ((TimelineEvent) -> Unit)? = null,
) {
    var isExpanded by remember { mutableStateOf(isInitiallyExpanded) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .glassPanel(
                shape = RoundedCornerShape(PremiumTokens.RadiusLarge),
                fillColor = PremiumColorValues.SpaceCard.copy(alpha = 0.88f),
                borderColor = PremiumColorValues.GlassBorder,
                borderWidth = 1.dp,
            )
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Header: Title + Event count badge + Expand toggle
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { isExpanded = !isExpanded },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(PremiumColorValues.NeonBlueTint)
                        .border(1.dp, PremiumColorValues.NeonBlue, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.Timeline,
                        contentDescription = null,
                        tint = PremiumColorValues.NeonBlue,
                        modifier = Modifier.size(14.dp),
                    )
                }

                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                    ),
                    color = PremiumColorValues.ForegroundPrimary,
                )

                // Event count pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(PremiumTokens.RadiusSmall))
                        .background(PremiumColorValues.GlassSurface)
                        .padding(horizontal = 7.dp, vertical = 2.dp),
                ) {
                    Text(
                        text = "${events.size}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                        ),
                        color = PremiumColorValues.NeonBlueSoft,
                    )
                }
            }

            Icon(
                imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = if (isExpanded) "Collapse timeline" else "Expand timeline",
                tint = PremiumColorValues.ForegroundMuted,
                modifier = Modifier.size(20.dp),
            )
        }

        // Expandable list with staggered animated reveal
        AnimatedVisibility(
            visible = isExpanded,
            enter = fadeIn(animationSpec = tween(PremiumTokens.DurationFast)),
        ) {
            if (events.isEmpty()) {
                TimelineEmptyState()
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                ) {
                    events.forEachIndexed { index, event ->
                        val isLast = index == events.lastIndex
                        val nextEvent = events.getOrNull(index + 1)
                        val isConnectorActive = event.status == TimelineItemStatus.COMPLETED &&
                                (nextEvent?.status == TimelineItemStatus.COMPLETED || nextEvent?.status == TimelineItemStatus.IN_PROGRESS)

                        StaggeredTimelineRow(
                            event = event,
                            index = index,
                            isLast = isLast,
                            isConnectorActive = isConnectorActive,
                            onClick = onEventClick?.let { { it(event) } },
                        )
                    }
                }
            }
        }
    }
}

/**
 * Animated wrapper providing staggered entrance motion (60ms delay per row).
 */
@Composable
private fun StaggeredTimelineRow(
    event: TimelineEvent,
    index: Int,
    isLast: Boolean,
    isConnectorActive: Boolean,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val reducedMotion = LocalReducedMotion.current
    var isRevealed by remember { mutableStateOf(reducedMotion) }

    LaunchedEffect(Unit) {
        if (!reducedMotion) {
            val delayMs = (index * PremiumTokens.StaggerItem).toLong()
            delay(delayMs)
            isRevealed = true
        }
    }

    val alpha by animateFloatAsState(
        targetValue = if (isRevealed) 1f else 0f,
        animationSpec = tween(
            durationMillis = PremiumTokens.DurationMedium,
            easing = PremiumTokens.EasingDecelerate,
        ),
        label = "rowAlpha_$index",
    )

    val offsetY by animateFloatAsState(
        targetValue = if (isRevealed) 0f else 20f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow,
        ),
        label = "rowOffset_$index",
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .graphicsLayer {
                this.alpha = alpha
                translationY = offsetY
            }
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // Node & Animated Connector
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(26.dp),
        ) {
            TimelineNode(status = event.status)

            if (!isLast) {
                AnimatedTimelineConnector(
                    isActive = isConnectorActive,
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 3.dp),
                )
            }
        }

        // Details column
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = if (isLast) 2.dp else 18.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            // Title & Timestamp Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = event.title,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                    ),
                    color = when (event.status) {
                        TimelineItemStatus.PENDING -> PremiumColorValues.ForegroundMuted
                        else -> PremiumColorValues.ForegroundPrimary
                    },
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    if (event.durationMs != null) {
                        Text(
                            text = "${event.durationMs}ms",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                            color = PremiumColorValues.ForegroundHint,
                        )
                    }

                    if (!event.timestamp.isNullOrBlank()) {
                        Text(
                            text = event.timestamp,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                            color = PremiumColorValues.ForegroundHint,
                        )
                    }
                }
            }

            // Description
            if (!event.description.isNullOrBlank()) {
                Text(
                    text = event.description,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = PremiumColorValues.ForegroundMuted,
                )
            }

            // Monospaced code snippet or path badge
            if (!event.codeSnippet.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(PremiumTokens.RadiusSmall))
                        .background(PremiumColorValues.GlassSurfaceLow)
                        .border(0.8.dp, PremiumColorValues.GlassBorder, RoundedCornerShape(PremiumTokens.RadiusSmall))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                ) {
                    Text(
                        text = event.codeSnippet,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                        ),
                        color = PremiumColorValues.NeonBlueSoft,
                    )
                }
            }
        }
    }
}

/**
 * Timeline Node with self-drawing checkmark for COMPLETED status.
 *
 * Uses [PathMeasure] to trim-draw the checkmark from start to finish
 * when transitioning to completed.
 */
@Composable
private fun TimelineNode(
    status: TimelineItemStatus,
    size: Dp = 22.dp,
) {
    val reducedMotion = LocalReducedMotion.current

    // Checkmark self-drawing stroke trim progress (0f -> 1f)
    var isDrawn by remember { mutableStateOf(reducedMotion) }
    LaunchedEffect(status) {
        if (status == TimelineItemStatus.COMPLETED && !reducedMotion) {
            isDrawn = true
        }
    }

    val checkmarkTrimProgress by animateFloatAsState(
        targetValue = if (isDrawn || reducedMotion) 1f else 0f,
        animationSpec = tween(
            durationMillis = PremiumTokens.DurationMedium,
            easing = FastOutSlowInEasing,
        ),
        label = "checkmarkTrim",
    )

    // Pulse animation for active node
    val infiniteTransition = rememberInfiniteTransition(label = "activeNodePulse")
    val pulseAlpha by if (reducedMotion || status != TimelineItemStatus.IN_PROGRESS) {
        infiniteTransition.animateFloat(1f, 1f, infiniteRepeatable(tween(Int.MAX_VALUE)), label = "pulseStatic")
    } else {
        infiniteTransition.animateFloat(
            initialValue = 0.5f,
            targetValue = 1.0f,
            animationSpec = infiniteRepeatable(
                animation = tween(800, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "pulseAlpha",
        )
    }

    Box(
        modifier = Modifier
            .size(size)
            .drawBehind {
                when (status) {
                    TimelineItemStatus.COMPLETED -> {
                        if (!reducedMotion) {
                            drawCircle(
                                color = PremiumColorValues.NeonGreenGlow,
                                radius = this.size.minDimension * 0.75f,
                            )
                        }
                    }
                    TimelineItemStatus.IN_PROGRESS -> {
                        if (!reducedMotion) {
                            drawCircle(
                                color = PremiumColorValues.NeonBlueGlow.copy(
                                    alpha = PremiumColorValues.NeonBlueGlow.alpha * pulseAlpha,
                                ),
                                radius = this.size.minDimension * 0.85f,
                            )
                        }
                    }
                    else -> Unit
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        when (status) {
            TimelineItemStatus.COMPLETED -> {
                // Background circle with self-drawing checkmark
                Box(
                    modifier = Modifier
                        .size(size)
                        .clip(CircleShape)
                        .background(PremiumColorValues.NeonGreen),
                    contentAlignment = Alignment.Center,
                ) {
                    SelfDrawingCheckmark(
                        trimFraction = checkmarkTrimProgress,
                        modifier = Modifier.size(12.dp),
                    )
                }
            }

            TimelineItemStatus.IN_PROGRESS -> {
                Box(
                    modifier = Modifier
                        .size(size)
                        .clip(CircleShape)
                        .background(PremiumColorValues.NeonBlueTint)
                        .border(1.8.dp, PremiumColorValues.NeonBlue, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(PremiumColorValues.NeonBlue),
                    )
                }
            }

            TimelineItemStatus.FAILED -> {
                Box(
                    modifier = Modifier
                        .size(size)
                        .clip(CircleShape)
                        .background(PremiumColorValues.NeonRedTint)
                        .border(1.5.dp, PremiumColorValues.NeonRed, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Failed",
                        tint = PremiumColorValues.NeonRed,
                        modifier = Modifier.size(12.dp),
                    )
                }
            }

            TimelineItemStatus.PENDING -> {
                Box(
                    modifier = Modifier
                        .size(size)
                        .clip(CircleShape)
                        .background(PremiumColorValues.GlassSurfaceLow)
                        .border(1.2.dp, PremiumColorValues.GlassBorder, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .size(4.dp)
                            .clip(CircleShape)
                            .background(PremiumColorValues.ForegroundHint),
                    )
                }
            }
        }
    }
}

/**
 * Draws an animated checkmark using [PathMeasure] stroke-trim.
 */
@Composable
private fun SelfDrawingCheckmark(
    trimFraction: Float,
    modifier: Modifier = Modifier,
    color: Color = PremiumColorValues.SpaceBlack,
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        val checkPath = Path().apply {
            moveTo(w * 0.15f, h * 0.50f)
            lineTo(w * 0.42f, h * 0.80f)
            lineTo(w * 0.85f, h * 0.22f)
        }

        val pathMeasure = PathMeasure()
        pathMeasure.setPath(checkPath, false)
        val totalLength = pathMeasure.length

        val animatedSegmentPath = Path()
        pathMeasure.getSegment(
            startDistance = 0f,
            stopDistance = totalLength * trimFraction.coerceIn(0f, 1f),
            destination = animatedSegmentPath,
            startWithMoveTo = true,
        )

        drawPath(
            path = animatedSegmentPath,
            color = color,
            style = Stroke(
                width = 2.dp.toPx(),
                cap = StrokeCap.Round,
                join = StrokeJoin.Round,
            ),
        )
    }
}

/**
 * Animated vertical connector line with downward gradient fill animation.
 */
@Composable
private fun AnimatedTimelineConnector(
    isActive: Boolean,
    modifier: Modifier = Modifier,
    lineWidth: Dp = 2.dp,
) {
    val reducedMotion = LocalReducedMotion.current

    val fillProgress by animateFloatAsState(
        targetValue = if (isActive || reducedMotion) 1f else 0f,
        animationSpec = tween(
            durationMillis = 500,
            easing = FastOutSlowInEasing,
        ),
        label = "connectorFill",
    )

    Canvas(
        modifier = modifier
            .width(lineWidth)
            .fillMaxHeight(),
    ) {
        val strokePx = lineWidth.toPx()
        val cx = size.width / 2f

        // Base dim connector
        drawLine(
            color = PremiumColorValues.GlassBorder,
            start = Offset(cx, 0f),
            end = Offset(cx, size.height),
            strokeWidth = strokePx,
            cap = StrokeCap.Round,
        )

        // Animated neon gradient fill from top downward
        if (fillProgress > 0f) {
            val filledHeight = size.height * fillProgress
            drawLine(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        PremiumColorValues.NeonGreen,
                        PremiumColorValues.NeonBlue,
                    ),
                    startY = 0f,
                    endY = size.height,
                ),
                start = Offset(cx, 0f),
                end = Offset(cx, filledHeight),
                strokeWidth = strokePx,
                cap = StrokeCap.Round,
            )
        }
    }
}

/**
 * Placeholder shown when there are no activity events.
 */
@Composable
private fun TimelineEmptyState(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "No activity recorded yet",
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
            color = PremiumColorValues.ForegroundHint,
        )
    }
}
