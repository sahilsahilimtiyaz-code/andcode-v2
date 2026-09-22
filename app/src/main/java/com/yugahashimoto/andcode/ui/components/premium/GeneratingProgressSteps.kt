package com.yugahashimoto.andcode.ui.components.premium

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yugahashimoto.andcode.core.mission.MissionProgress
import com.yugahashimoto.andcode.core.mission.MissionStatus
import com.yugahashimoto.andcode.core.mission.MissionStep
import com.yugahashimoto.andcode.ui.theme.premium.LocalReducedMotion
import com.yugahashimoto.andcode.ui.theme.premium.PremiumColorValues
import com.yugahashimoto.andcode.ui.theme.premium.PremiumTokens
import kotlin.math.max
import kotlin.math.min

/**
 * Status of an individual step in the progress timeline.
 */
enum class StepExecutionStatus {
    PENDING,
    IN_PROGRESS,
    COMPLETED,
    FAILED,
}

/**
 * Presentation data for a single step in [GeneratingProgressSteps].
 */
data class ProgressStepData(
    val id: String,
    val title: String,
    val status: StepExecutionStatus,
    val progress: Float = when (status) {
        StepExecutionStatus.COMPLETED -> 1f
        StepExecutionStatus.PENDING -> 0f
        StepExecutionStatus.IN_PROGRESS -> 0.5f
        StepExecutionStatus.FAILED -> 0.5f
    },
    val subtitle: String? = null,
)

/**
 * Vertical progress step timeline displaying generation progress.
 *
 * Visual structure:
 * - Vertical sequence of step rows connected by animated gradient lines.
 * - Circular indicators on the left:
 *   - COMPLETED: Neon Green badge with animated checkmark and glow.
 *   - IN_PROGRESS: Pulsating Neon Blue circular badge with active indicator.
 *   - PENDING: Subdued glass circle with subtle border.
 *   - FAILED: Neon Red badge with warning indicator.
 * - Right content:
 *   - Step title and status text ("Completed", "In progress", "Pending").
 *   - 4-segment gradient progress bar ([StepSegmentBar]).
 *
 * @param steps List of step data to render.
 * @param modifier Layout modifier.
 */
@Composable
fun GeneratingProgressSteps(
    steps: List<ProgressStepData>,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        steps.forEachIndexed { index, step ->
            val isLast = index == steps.lastIndex
            val nextStep = steps.getOrNull(index + 1)
            val isConnectorActive = step.status == StepExecutionStatus.COMPLETED &&
                    (nextStep?.status == StepExecutionStatus.COMPLETED || nextStep?.status == StepExecutionStatus.IN_PROGRESS)

            ProgressStepRow(
                step = step,
                isLast = isLast,
                isConnectorActive = isConnectorActive,
            )
        }
    }
}

/**
 * Single step item row with vertical connector line and 4-segment progress bar.
 */
@Composable
private fun ProgressStepRow(
    step: ProgressStepData,
    isLast: Boolean,
    isConnectorActive: Boolean,
    modifier: Modifier = Modifier,
) {
    val reducedMotion = LocalReducedMotion.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // Left Column: Node Icon + Connector Line
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(28.dp),
        ) {
            StepStatusNode(status = step.status)

            if (!isLast) {
                StepConnectorLine(
                    isActive = isConnectorActive,
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 4.dp),
                )
            }
        }

        // Right Column: Title + Status + 4-segment progress bar
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = if (isLast) 4.dp else 16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = step.title,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                    ),
                    color = when (step.status) {
                        StepExecutionStatus.PENDING -> PremiumColorValues.ForegroundMuted
                        else -> PremiumColorValues.ForegroundPrimary
                    },
                )

                StepStatusBadge(status = step.status)
            }

            if (!step.subtitle.isNullOrBlank()) {
                Text(
                    text = step.subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = PremiumColorValues.ForegroundMuted,
                )
            }

            // 4-segment gradient bar
            StepSegmentBar(
                progress = step.progress,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp),
                height = 5.dp,
            )
        }
    }
}

/**
 * Circular status indicator node on the left of each step.
 */
@Composable
private fun StepStatusNode(
    status: StepExecutionStatus,
    size: Dp = 24.dp,
) {
    val reducedMotion = LocalReducedMotion.current

    val infiniteTransition = rememberInfiniteTransition(label = "activeStepPulse")
    val pulseScale by if (reducedMotion || status != StepExecutionStatus.IN_PROGRESS) {
        infiniteTransition.animateFloat(1f, 1f, infiniteRepeatable(tween(Int.MAX_VALUE)), label = "pulseStatic")
    } else {
        infiniteTransition.animateFloat(
            initialValue = 0.92f,
            targetValue = 1.08f,
            animationSpec = infiniteRepeatable(
                animation = tween(800, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "pulseActive",
        )
    }

    // Animated pop scale when transitioning to completed
    val completedScale by animateFloatAsState(
        targetValue = if (status == StepExecutionStatus.COMPLETED) 1f else 0.85f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "completedPop",
    )

    Box(
        modifier = Modifier
            .size(size)
            .graphicsLayer {
                scaleX = if (status == StepExecutionStatus.IN_PROGRESS) pulseScale else completedScale
                scaleY = if (status == StepExecutionStatus.IN_PROGRESS) pulseScale else completedScale
            }
            .drawBehind {
                when (status) {
                    StepExecutionStatus.COMPLETED -> {
                        if (!reducedMotion) {
                            drawCircle(
                                color = PremiumColorValues.NeonGreenGlow,
                                radius = this.size.minDimension * 0.75f,
                            )
                        }
                    }
                    StepExecutionStatus.IN_PROGRESS -> {
                        if (!reducedMotion) {
                            drawCircle(
                                color = PremiumColorValues.NeonBlueGlow,
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
            StepExecutionStatus.COMPLETED -> {
                val checkmarkTrim by animateFloatAsState(
                    targetValue = if (completedScale > 0.95f) 1f else 0f,
                    animationSpec = if (reducedMotion) tween(0) else tween(400, easing = FastOutSlowInEasing),
                    label = "checkmarkTrim",
                )
                Box(
                    modifier = Modifier
                        .size(size)
                        .clip(CircleShape)
                        .background(PremiumColorValues.NeonGreen),
                    contentAlignment = Alignment.Center,
                ) {
                    Canvas(
                        modifier = Modifier
                            .size(15.dp)
                            .graphicsLayer {
                                scaleX = completedScale
                                scaleY = completedScale
                            },
                    ) {
                        val cx = this.size.width / 2f
                        val cy = this.size.height / 2f
                        val checkSize = min(this.size.width, this.size.height) * 0.45f
                        // Checkmark path: from left-middle to center-bottom to right-top
                        val path = android.graphics.Path().apply {
                            moveTo(cx - checkSize * 0.4f, cy)
                            lineTo(cx - checkSize * 0.1f, cy + checkSize * 0.3f)
                            lineTo(cx + checkSize * 0.4f, cy - checkSize * 0.3f)
                        }
                        val pathMeasure = android.graphics.PathMeasure(path, false)
                        val pathLength = pathMeasure.length
                        val stopDistance = pathLength * checkmarkTrim
                        val dstPath = android.graphics.Path()
                        pathMeasure.getSegment(0f, stopDistance, dstPath, true)
                        drawPath(
                            path = dstPath.asComposePath(),
                            color = PremiumColorValues.SpaceBlack,
                            style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
                        )
                    }
                }
            }

            StepExecutionStatus.IN_PROGRESS -> {
                Box(
                    modifier = Modifier
                        .size(size)
                        .clip(CircleShape)
                        .background(PremiumColorValues.NeonBlueTint)
                        .border(
                            width = 2.dp,
                            color = PremiumColorValues.NeonBlue,
                            shape = CircleShape,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(PremiumColorValues.NeonBlue),
                    )
                }
            }

            StepExecutionStatus.FAILED -> {
                Box(
                    modifier = Modifier
                        .size(size)
                        .clip(CircleShape)
                        .background(PremiumColorValues.NeonRedTint)
                        .border(
                            width = 1.5.dp,
                            color = PremiumColorValues.NeonRed,
                            shape = CircleShape,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Failed",
                        tint = PremiumColorValues.NeonRed,
                        modifier = Modifier.size(14.dp),
                    )
                }
            }

            StepExecutionStatus.PENDING -> {
                Box(
                    modifier = Modifier
                        .size(size)
                        .clip(CircleShape)
                        .background(PremiumColorValues.GlassSurfaceLow)
                        .border(
                            width = 1.2.dp,
                            color = PremiumColorValues.GlassBorder,
                            shape = CircleShape,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(PremiumColorValues.ForegroundHint),
                    )
                }
            }
        }
    }
}

/**
 * Animated vertical connector line linking step nodes.
 */
@Composable
private fun StepConnectorLine(
    isActive: Boolean,
    modifier: Modifier = Modifier,
    lineWidth: Dp = 2.dp,
) {
    val reducedMotion = LocalReducedMotion.current

    val activeBrush = Brush.verticalGradient(
        colors = listOf(
            PremiumColorValues.NeonGreen,
            PremiumColorValues.NeonBlue,
        ),
    )

    // Animated fill progress for connector line
    val fillProgress by animateFloatAsState(
        targetValue = if (isActive) 1f else 0f,
        animationSpec = if (reducedMotion) tween(0) else tween(600, easing = FastOutSlowInEasing),
        label = "connectorFill",
    )

    Canvas(
        modifier = modifier
            .width(lineWidth)
            .fillMaxHeight(),
    ) {
        val strokePx = lineWidth.toPx()
        val totalHeight = size.height
        val fillHeight = totalHeight * fillProgress

        // Draw the inactive portion (bottom)
        if (fillProgress < 1f) {
            drawLine(
                color = PremiumColorValues.GlassBorder,
                start = Offset(size.width / 2f, fillHeight),
                end = Offset(size.width / 2f, totalHeight),
                strokeWidth = strokePx,
                cap = StrokeCap.Round,
            )
        }

        // Draw the active gradient portion (top to fillHeight)
        if (fillProgress > 0f) {
            drawLine(
                brush = activeBrush,
                start = Offset(size.width / 2f, 0f),
                end = Offset(size.width / 2f, fillHeight),
                strokeWidth = strokePx,
                cap = StrokeCap.Round,
            )
        }
    }
}

/**
 * Text badge on the right displaying "Completed", "In progress", or "Pending".
 */
@Composable
private fun StepStatusBadge(
    status: StepExecutionStatus,
    modifier: Modifier = Modifier,
) {
    val (label, textColor, badgeColor) = when (status) {
        StepExecutionStatus.COMPLETED -> Triple(
            "Completed",
            PremiumColorValues.NeonGreen,
            PremiumColorValues.NeonGreenTint,
        )
        StepExecutionStatus.IN_PROGRESS -> Triple(
            "In progress",
            PremiumColorValues.NeonBlue,
            PremiumColorValues.NeonBlueTint,
        )
        StepExecutionStatus.FAILED -> Triple(
            "Failed",
            PremiumColorValues.NeonRed,
            PremiumColorValues.NeonRedTint,
        )
        StepExecutionStatus.PENDING -> Triple(
            "Pending",
            PremiumColorValues.ForegroundHint,
            Color.Transparent,
        )
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(PremiumTokens.RadiusSmall))
            .background(badgeColor)
            .padding(horizontal = 7.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (status == StepExecutionStatus.COMPLETED) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(PremiumColorValues.NeonGreen),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = PremiumColorValues.SpaceBlack,
                    modifier = Modifier.size(8.dp),
                )
            }
        }

        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Medium,
                fontSize = 11.sp,
            ),
            color = textColor,
        )
    }
}

/**
 * Maps existing domain [MissionProgress] to a list of [ProgressStepData].
 */
fun MissionProgress.toProgressStepDataList(): List<ProgressStepData> {
    val steps = listOf(
        MissionStep.UNDERSTAND,
        MissionStep.PLAN,
        MissionStep.IMPLEMENT,
        MissionStep.TEST,
        MissionStep.VERIFY,
        MissionStep.COMPLETE,
    )

    return steps.map { step ->
        val stepStatus = when {
            completedSteps.contains(step) -> StepExecutionStatus.COMPLETED
            currentStep == step && status == MissionStatus.RUNNING -> StepExecutionStatus.IN_PROGRESS
            status == MissionStatus.FAILED && currentStep == step -> StepExecutionStatus.FAILED
            else -> StepExecutionStatus.PENDING
        }

        val stepProgress = when (stepStatus) {
            StepExecutionStatus.COMPLETED -> 1f
            StepExecutionStatus.IN_PROGRESS -> 0.6f
            StepExecutionStatus.FAILED -> 0.4f
            StepExecutionStatus.PENDING -> 0f
        }

        ProgressStepData(
            id = step.name,
            title = step.displayName,
            status = stepStatus,
            progress = stepProgress,
        )
    }
}
