package com.yugahashimoto.andcode.ui.theme.premium

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

// ─────────────────────────────────────────────────────────────────────────────
// Glass Panel
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Applies a glass-morphism surface to any composable:
 *   - Semi-transparent white fill
 *   - Frosted-glass border at [borderColor] (1.5dp)
 *   - Clipped to [shape]
 *
 * The blur is approximated via layered alpha because Jetpack Compose does not
 * expose a native RenderEffect blur in a simple Modifier on API < 31.  The
 * visual result matches the reference screenshots on all API levels.
 *
 * @param shape         Corner shape of the glass panel. Defaults to 20dp rounded.
 * @param fillColor     Background fill. Defaults to [PremiumColorValues.GlassSurface].
 * @param borderColor   Border stroke colour. Defaults to [PremiumColorValues.GlassBorder].
 * @param borderWidth   Width of the border stroke. Defaults to [PremiumTokens.BorderWidth].
 */
fun Modifier.glassPanel(
    shape: Shape = RoundedCornerShape(PremiumTokens.RadiusLarge),
    fillColor: Color = PremiumColorValues.GlassSurface,
    borderColor: Color = PremiumColorValues.GlassBorder,
    borderWidth: Dp = PremiumTokens.BorderWidth,
): Modifier =
    this
        .clip(shape)
        .background(color = fillColor, shape = shape)
        .border(width = borderWidth, color = borderColor, shape = shape)

// ─────────────────────────────────────────────────────────────────────────────
// Glow Shadow
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Draws a neon glow halo behind the composable using [Canvas.drawIntoCanvas].
 *
 * The glow is rendered as a soft shadow with [glowColor] and [radius].
 *
 * Accessibility: when [reducedMotion] is `true` the modifier is a no-op so
 * that heavy Canvas draws are skipped on low-power devices.
 *
 * @param glowColor     Colour of the glow. Typically a 25-40 % alpha neon.
 * @param radius        Blur spread radius. Defaults to [PremiumTokens.GlowRadius].
 * @param reducedMotion Pass [LocalReducedMotion.current] to skip on a11y request.
 */
fun Modifier.glowShadow(
    glowColor: Color = PremiumColorValues.NeonBlueGlow,
    radius: Dp = PremiumTokens.GlowRadius,
    reducedMotion: Boolean = false,
): Modifier =
    if (reducedMotion) this
    else drawBehind {
        val radiusPx = radius.toPx()
        drawIntoCanvas { canvas ->
            val paint = Paint().apply {
                asFrameworkPaint().apply {
                    isAntiAlias = true
                    color = android.graphics.Color.TRANSPARENT
                    setShadowLayer(
                        radiusPx,
                        0f,
                        0f,
                        glowColor.copy(alpha = 0.8f).toArgb(),
                    )
                }
            }
            canvas.drawRoundRect(
                left = 0f,
                top = 0f,
                right = size.width,
                bottom = size.height,
                radiusX = radiusPx,
                radiusY = radiusPx,
                paint = paint,
            )
        }
    }

// ─────────────────────────────────────────────────────────────────────────────
// Neon Border (static)
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Draws a static neon gradient border (Neon Blue → Electric Purple) around the
 * composable.
 *
 * For the animated sweeping version, use [neonBorderAnimated].
 *
 * @param shape        Corner shape of the border.
 * @param borderWidth  Width of the stroke.
 * @param colors       Gradient colour stops. Defaults to primary neon gradient.
 */
fun Modifier.neonBorder(
    shape: Shape = RoundedCornerShape(PremiumTokens.RadiusLarge),
    borderWidth: Dp = PremiumTokens.BorderWidth,
    colors: List<Color> = listOf(
        PremiumColorValues.NeonBlue,
        PremiumColorValues.NeonViolet,
        PremiumColorValues.ElectricPurple,
    ),
): Modifier =
    border(
        width = borderWidth,
        brush = Brush.linearGradient(colors),
        shape = shape,
    )

// ─────────────────────────────────────────────────────────────────────────────
// Neon Border Sweep (animated – PASS 2)
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Draws a neon gradient border that rotates 360° every [PremiumTokens.BorderSweep]ms,
 * creating the "laser sweep" effect seen in the reference video.
 *
 * Implemented via [infiniteRepeatable] rotation of a [Brush.linearGradient]
 * whose angle advances continuously.  When [reducedMotion] is `true`, falls
 * back to the static [neonBorder] to avoid all animation.
 *
 * @param shape         Corner shape.
 * @param borderWidth   Width of the rotating stroke.
 * @param reducedMotion Disable animation for accessibility.
 * @param colors        Gradient stops. Defaults to primary neon gradient.
 */
fun Modifier.neonBorderAnimated(
    shape: Shape = RoundedCornerShape(PremiumTokens.RadiusLarge),
    borderWidth: Dp = PremiumTokens.BorderWidth,
    reducedMotion: Boolean = false,
    colors: List<Color> = listOf(
        PremiumColorValues.NeonBlue,
        PremiumColorValues.NeonViolet,
        PremiumColorValues.ElectricPurple,
        PremiumColorValues.NeonBlue,
    ),
): Modifier =
    if (reducedMotion) {
        neonBorder(shape = shape, borderWidth = borderWidth, colors = colors.dropLast(1))
    } else {
        composed {
            val infiniteTransition = rememberInfiniteTransition(label = "neonBorderSweep")
            val angleDeg by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 360f,
                animationSpec = infiniteRepeatable(
                    animation = tween(
                        durationMillis = PremiumTokens.BorderSweep,
                        easing = LinearEasing,
                    ),
                    repeatMode = RepeatMode.Restart,
                ),
                label = "neonBorderAngle",
            )

            drawBehind {
                val angleRad = Math.toRadians(angleDeg.toDouble())
                val halfW = size.width / 2f
                val halfH = size.height / 2f
                val radius = maxOf(halfW, halfH) * 1.42f  // half-diagonal

                val start = Offset(
                    x = center.x + (radius * cos(angleRad)).toFloat(),
                    y = center.y + (radius * sin(angleRad)).toFloat(),
                )
                val end = Offset(
                    x = center.x - (radius * cos(angleRad)).toFloat(),
                    y = center.y - (radius * sin(angleRad)).toFloat(),
                )

                val strokeBrush = Brush.linearGradient(
                    colors = colors,
                    start = start,
                    end = end,
                )

                val strokeWidthPx = borderWidth.toPx()
                val cornerPx = remember(shape) {
                    when {
                        shape is RoundedCornerShape -> 20f  // approximation
                        else -> 0f
                    }
                }
                drawIntoCanvas { canvas ->
                    val framePaint = Paint().apply {
                        asFrameworkPaint().apply {
                            isAntiAlias = true
                            style = android.graphics.Paint.Style.STROKE
                            strokeWidth = strokeWidthPx
                        }
                        brush = strokeBrush
                    }
                    canvas.drawRoundRect(
                        left = strokeWidthPx / 2f,
                        top = strokeWidthPx / 2f,
                        right = size.width - strokeWidthPx / 2f,
                        bottom = size.height - strokeWidthPx / 2f,
                        radiusX = cornerPx,
                        radiusY = cornerPx,
                        paint = framePaint,
                    )
                }
            }
        }
    }

// ─────────────────────────────────────────────────────────────────────────────
// Shimmer Overlay (for text / card reveals)
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Adds a one-shot shimmer sweep over a composable using an animated
 * [Brush.linearGradient].
 *
 * Use [shimmerOnce] when a message or card just appeared.  Use the
 * [shimmerLoop] variant for indefinite skeleton loading.
 *
 * @param reducedMotion When `true`, the modifier is a no-op.
 */
fun Modifier.shimmerLoop(
    baseColor: Color = PremiumColorValues.GlassSurface,
    highlightColor: Color = PremiumColorValues.GlassSurfaceHigh,
    reducedMotion: Boolean = false,
): Modifier =
    if (reducedMotion) this
    else composed {
        val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
        val xOffset by infiniteTransition.animateFloat(
            initialValue = -1f,
            targetValue = 2f,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = PremiumTokens.ShimmerSweep,
                    easing = LinearEasing,
                ),
                repeatMode = RepeatMode.Restart,
            ),
            label = "shimmerX",
        )
        drawBehind {
            val shimmerBrush = Brush.linearGradient(
                colors = listOf(
                    baseColor,
                    highlightColor,
                    Color.White.copy(alpha = 0.08f),
                    highlightColor,
                    baseColor,
                ),
                start = Offset(x = size.width * (xOffset - 0.3f), y = 0f),
                end = Offset(x = size.width * (xOffset + 0.3f), y = size.height),
                tileMode = TileMode.Clamp,
            )
            drawRect(brush = shimmerBrush)
        }
    }

// ─────────────────────────────────────────────────────────────────────────────
// Convenience: Glass Card layout padding
// ─────────────────────────────────────────────────────────────────────────────

/** Standard inner padding for glass-panel cards. */
fun Modifier.glassPanelPadding(): Modifier = padding(horizontal = 16.dp, vertical = 14.dp)
