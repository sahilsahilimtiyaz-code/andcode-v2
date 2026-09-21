package com.yugahashimoto.andcode.ui.components.premium

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Stroke
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yugahashimoto.andcode.ui.theme.premium.LOCAL_REDUCED_MOTION
import com.yugahashimoto.andcode.ui.theme.premium.PremiumColorValues
import com.yugahashimoto.andcode.ui.theme.premium.PremiumTokens
import com.yugahashimoto.andcode.ui.theme.premium.rememberReducedMotion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

data class ReasoningStep(
    val id: String,
    val title: String,
    val description: String? = null,
    val status: ReasoningStepStatus = ReasoningStepStatus.PENDING,
)

enum class ReasoningStepStatus { PENDING, IN_PROGRESS, COMPLETED, ERROR }

@Composable
fun ReasoningPanel(
    modifier: Modifier = Modifier,
    steps: List<ReasoningStep> = defaultSteps,
    progress: Float = 0f,
    onToggleExpand: () -> Unit,
    expanded: Boolean = true,
) {
    val reducedMotion = rememberReducedMotion()

    val infiniteTransition = rememberInfiniteTransition(label = "reasoningPanel")

    val progressAnim = infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "progressPulse",
    )

    val glowAnim = infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "glowAnim",
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize()
            .clip(RoundedCornerShape(PremiumTokens.RADIUS_LARGE))
            .drawBehind {
                val glowColor = PremiumColorValues.NEON_BLUE.copy(alpha = 0.08f * glowAnim.value)
                val largeRadius = PremiumTokens.RADIUS_LARGE.toPx()
                val cornerRadius = CornerRadius(largeRadius, largeRadius)
                drawRoundRect(
                    color = glowColor,
                    size = size,
                    cornerRadius = cornerRadius,
                    style = Stroke(width = 2.dp.toPx()),
                )
            },
        color = Color.White.copy(alpha = 0.05f),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = PremiumColorValues.NEON_BLUE.copy(alpha = 0.15f),
        ),
        shape = RoundedCornerShape(PremiumTokens.RADIUS_LARGE),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            // Header with expand/collapse toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Psychology,
                        contentDescription = null,
                        tint = PremiumColorValues.NEON_BLUE,
                        modifier = Modifier.size(20.dp),
                    )
                    Column {
                        Text(
                            text = "Reasoning",
                            style = androidx.compose.material3.MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                        )
                        if (!expanded) {
                            Text(
                                text = "${(progress * 100).toInt()}%",
                                style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                                color = PremiumColorValues.NEON_BLUE.copy(alpha = 0.8f),
                            )
                        }
                    }
                }
                IconButton(
                    onClick = onToggleExpand,
                    modifier = Modifier.size(36.dp),
                ) {
                    Icon(
                        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (expanded) "Collapse" else "Expand",
                        tint = PremiumColorValues.NEON_BLUE,
                        modifier = Modifier
                            .size(20.dp)
                            .graphicsLayer {
                                rotationZ = if (expanded) 180f else 0f
                            }
                            .animateContentSize(),
                    )
                }
            }

            if (expanded) {
                androidx.compose.foundation.layout.Spacer(Modifier.height(12.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateContentSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    steps.forEachIndexed { index, step ->
                        ReasoningStepItem(
                            step = step,
                            index = index,
                            isLast = index == steps.lastIndex,
                            progressAnim = progressAnim,
                            reducedMotion = reducedMotion,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReasoningStepItem(
    step: ReasoningStep,
    index: Int,
    isLast: Boolean,
    progressAnim: State<Float>,
    reducedMotion: Boolean,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "step_$index")

    val checkmarkAnim by animateFloatAsState(
        targetValue = if (step.status == ReasoningStepStatus.COMPLETED) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "checkmark_$index",
    )

    val pulseAnim = infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulse_$index",
    )

    val statusColor = when (step.status) {
        ReasoningStepStatus.COMPLETED -> PremiumColorValues.NEON_GREEN
        ReasoningStepStatus.IN_PROGRESS -> PremiumColorValues.NEON_BLUE
        ReasoningStepStatus.ERROR -> PremiumColorValues.NEON_RED
        else -> androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
    }

    val isActive = step.status == ReasoningStepStatus.IN_PROGRESS

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .graphicsLayer {
                if (isActive && !reducedMotion) {
                    alpha = pulseAnim.value
                }
            }
            .animateContentSize(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Status indicator
        Box(
            modifier = Modifier.size(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            when (step.status) {
                ReasoningStepStatus.COMPLETED -> {
                    // Animated checkmark with stroke trim
                    Canvas(
                        modifier = Modifier.size(20.dp),
                        onDraw = {
                            val center = Offset(size.width / 2f, size.height / 2f)
                            val radius = 8f
                            val progress = checkmarkAnim

                            // Circle background
                            drawCircle(
                                color = statusColor.copy(alpha = 0.2f),
                                center = center,
                                radius = radius,
                            )

                            // Checkmark path
                            val path = androidx.compose.ui.graphics.Path().apply {
                                moveTo(center.x - 3f, center.y)
                                lineTo(center.x - 1f, center.y + 2f)
                                lineTo(center.x + 4f, center.y - 3f)
                            }

                            // Stroke trim animation
                            val pathMeasure = PathMeasure().apply {
                                setPath(path, false)
                            }

                            val length = pathMeasure.length
                            val trimPath = androidx.compose.ui.graphics.Path()

                            pathMeasure.getSegment(
                                startDistance = 0f,
                                stopDistance = length * progress,
                                destination = trimPath,
                                startWithMoveTo = true,
                            )

                            drawPath(
                                path = trimPath,
                                color = statusColor,
                                style = Stroke(width = 2.5f, cap = StrokeCap.Round),
                            )
                        },
                    )
                }
                ReasoningStepStatus.IN_PROGRESS -> {
                    // Pulsing spinner
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .graphicsLayer {
                                rotationZ = progressAnim.value * 360f
                            }
                            .drawBehind {
                                val center = Offset(size.width / 2f, size.height / 2f)
                                val radius = 8f
                                val sweepAngle = 270f * progressAnim.value + 45f
                                drawArc(
                                    color = statusColor,
                                    startAngle = progressAnim.value * 360f,
                                    sweepAngle = sweepAngle,
                                    useCenter = false,
                                    topLeft = Offset(center.x - radius, center.y - radius),
                                    size = Size(radius * 2, radius * 2),
                                    style = Stroke(width = 2.5f, cap = StrokeCap.Round),
                                )
                            },
                    )
                }
                ReasoningStepStatus.ERROR -> {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Error",
                        tint = statusColor,
                        modifier = Modifier.size(20.dp),
                    )
                }
                else -> {
                    // Pending - subtle dot
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(androidx.compose.foundation.shape.CircleShape)
                            .background(statusColor)
                            .graphicsLayer {
                                alpha = if (reducedMotion) 0.5f else 1f
                            },
                    )
                }
            }
        }

        // Step content
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = step.title,
                style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
                color = if (isActive) PremiumColorValues.NEON_BLUE else androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            step.description?.let { desc ->
                Text(
                    text = desc,
                    style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                    color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        // Progress indicator for current step
        if (isActive) {
            Text(
                text = "${(progressAnim.value * 100).toInt()}%",
                style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                color = PremiumColorValues.NEON_BLUE,
                fontWeight = FontWeight.Medium,
            )
        }
    }

    // Divider line between steps
    if (!isLast) {
        Box(
            modifier = Modifier
                .padding(start = 36.dp)
                .height(1.dp)
                .fillMaxWidth()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            PremiumColorValues.NEON_BLUE.copy(alpha = 0.1f),
                            Color.Transparent,
                        ),
                    ),
                ),
        )
    }
}

private val defaultSteps = listOf(
    ReasoningStep(
        id = "analyze",
        title = "Analyzing your request...",
        description = "Understanding the problem and context",
    ),
    ReasoningStep(
        id = "plan",
        title = "Planning approach...",
        description = "Breaking down into actionable steps",
    ),
    ReasoningStep(
        id = "write",
        title = "Writing code...",
        description = "Implementing the solution",
    ),
    ReasoningStep(
        id = "review",
        title = "Reviewing for errors...",
        description = "Checking correctness and best practices",
    ),
)