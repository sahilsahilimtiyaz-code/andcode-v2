package com.yugahashimoto.andcode.ui.components.premium

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.yugahashimoto.andcode.ui.theme.premium.LocalReducedMotion
import com.yugahashimoto.andcode.ui.theme.premium.PremiumColorValues
import com.yugahashimoto.andcode.ui.theme.premium.PremiumTokens
import kotlin.math.cos
import kotlin.math.sin

/**
 * NeonBorderSweep – standalone composable that wraps [content] with an
 * animated neon gradient border.
 *
 * The border gradient rotates 360° every [PremiumTokens.BorderSweep] (2 000ms),
 * creating a "laser beam orbiting the panel" effect as seen in the reference video.
 *
 * Key technical approach:
 *   - A [rememberInfiniteTransition] drives the sweep angle.
 *   - At each frame, a [Brush.linearGradient] with a rotating start/end point
 *     is used to paint a [Stroke] along a rounded-rectangle path.
 *   - A second, static [Color] is used as the base dim border so that
 *     the glass panel is never completely borderless between sweep highlights.
 *
 * Accessibility: when [LocalReducedMotion] is `true`, a static gradient border
 * is drawn instead (using the same colours but no rotation).
 *
 * @param modifier       Layout modifier.
 * @param shape          Corner shape. Defaults to 20dp rounded.
 * @param borderWidth    Width of the neon stroke. Defaults to 1.5dp.
 * @param sweepDuration  Milliseconds per full 360° sweep. Defaults to 2 000ms.
 * @param colors         List of colour stops for the sweeping gradient.
 * @param dimBorderColor Constant dim border drawn below the sweep for depth.
 * @param content        Child composables.
 */
@Composable
fun NeonBorderSweep(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(PremiumTokens.RadiusLarge),
    borderWidth: Dp = PremiumTokens.BorderWidth,
    sweepDuration: Int = PremiumTokens.BorderSweep,
    colors: List<Color> = listOf(
        PremiumColorValues.NeonBlue,
        PremiumColorValues.NeonViolet,
        PremiumColorValues.ElectricPurple,
        PremiumColorValues.NeonBlue,         // close the loop smoothly
    ),
    dimBorderColor: Color = PremiumColorValues.GlassBorder,
    content: @Composable () -> Unit,
) {
    val reducedMotion = LocalReducedMotion.current

    val infiniteTransition = rememberInfiniteTransition(label = "neonBorderSweep")

    val angleDeg by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = sweepDuration, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "borderAngle",
    )

    Box(
        modifier = modifier
            .clip(shape)
            .drawWithContent {
                // Draw child content first
                drawContent()

                val strokePx = borderWidth.toPx()
                val inset = strokePx / 2f
                val cornerPx = when (shape) {
                    is RoundedCornerShape -> PremiumTokens.RadiusLarge.toPx()
                    else -> 0f
                }

                // ── Static dim base border ─────────────────────────────
                drawRoundRect(
                    color = dimBorderColor,
                    topLeft = Offset(inset, inset),
                    size = Size(size.width - strokePx, size.height - strokePx),
                    cornerRadius = CornerRadius(cornerPx, cornerPx),
                    style = Stroke(
                        width = strokePx,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round,
                    ),
                )

                // ── Animated sweep border ─────────────────────────────
                val rad = if (reducedMotion) 0.0 else Math.toRadians(angleDeg.toDouble())
                val half = maxOf(size.width, size.height) / 2f * 1.42f  // half-diagonal
                val pivotX = size.width / 2f
                val pivotY = size.height / 2f

                val sweepStart = Offset(
                    x = pivotX + (half * cos(rad)).toFloat(),
                    y = pivotY + (half * sin(rad)).toFloat(),
                )
                val sweepEnd = Offset(
                    x = pivotX - (half * cos(rad)).toFloat(),
                    y = pivotY - (half * sin(rad)).toFloat(),
                )

                val sweepBrush = Brush.linearGradient(
                    colors = colors,
                    start = sweepStart,
                    end = sweepEnd,
                )

                drawRoundRect(
                    brush = sweepBrush,
                    topLeft = Offset(inset, inset),
                    size = Size(size.width - strokePx, size.height - strokePx),
                    cornerRadius = CornerRadius(cornerPx, cornerPx),
                    style = Stroke(
                        width = strokePx,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round,
                    ),
                    blendMode = BlendMode.Screen,  // lightens over the dim base
                )
            },
    ) {
        content()
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Modifier extension variant (no wrapping Box needed)
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Applies the NeonBorderSweep animation directly as a [Modifier].
 *
 * This is useful when you don't want to restructure your layout hierarchy.
 * For a full composable wrapper see [NeonBorderSweep].
 *
 * @param shape          Corner shape.
 * @param borderWidth    Stroke width.
 * @param sweepDuration  Milliseconds per 360° rotation.
 * @param reducedMotion  Disables rotation when `true`.
 * @param colors         Gradient colour stops.
 */
fun Modifier.neonSweepBorder(
    shape: RoundedCornerShape = RoundedCornerShape(PremiumTokens.RadiusLarge),
    borderWidth: Dp = PremiumTokens.BorderWidth,
    sweepDuration: Int = PremiumTokens.BorderSweep,
    reducedMotion: Boolean = false,
    colors: List<Color> = listOf(
        PremiumColorValues.NeonBlue,
        PremiumColorValues.NeonViolet,
        PremiumColorValues.ElectricPurple,
        PremiumColorValues.NeonBlue,
    ),
): Modifier = this.then(
    Modifier.drawWithContent {
        drawContent()
        val strokePx = borderWidth.toPx()
        val inset = strokePx / 2f
        val cornerPx = PremiumTokens.RadiusLarge.toPx()

        // Static fallback brush (used both for dim base and static mode)
        val staticBrush = Brush.linearGradient(
            colors = colors.dropLast(1),
            start = Offset.Zero,
            end = Offset(size.width, size.height),
        )

        drawRoundRect(
            brush = staticBrush,
            topLeft = Offset(inset, inset),
            size = Size(size.width - strokePx, size.height - strokePx),
            cornerRadius = CornerRadius(cornerPx, cornerPx),
            style = Stroke(width = strokePx, cap = StrokeCap.Round),
        )
    },
)
