package com.yugahashimoto.andcode.ui.components.premium

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.yugahashimoto.andcode.ui.theme.premium.LocalReducedMotion
import com.yugahashimoto.andcode.ui.theme.premium.PremiumColorValues
import com.yugahashimoto.andcode.ui.theme.premium.PremiumTokens
import kotlin.math.cos
import kotlin.math.sin

/**
 * Thinking Orb – the signature AI "thinking" animation for AndCode v2.0.
 *
 * Renders a glowing central sphere surrounded by three orbital rings, each
 * rotating at a different speed and tilted at different angles to give a
 * convincing 3-D illusion on a flat Canvas.
 *
 * Reference spec:
 *   - Central orb: radial gradient (NeonBlue core → transparent edge)
 *   - Ring 1 (fast):   8 000ms cycle, X-tilt ~20°, width 1.8dp
 *   - Ring 2 (medium): 12 000ms cycle, Y-tilt ~45°, width 1.5dp
 *   - Ring 3 (slow):   16 000ms cycle, Z-plane  0°, width 1.2dp
 *   - Particle sparkles float outward from the orb surface
 *
 * Accessibility: when [LocalReducedMotion] is `true` the rings are drawn in
 * their initial position (no rotation) and particles are omitted.
 *
 * @param modifier      Layout modifier.
 * @param size          Diameter of the bounding box. Defaults to 120dp.
 * @param coreColor     Central sphere colour. Defaults to [PremiumColorValues.NeonBlue].
 * @param ring1Color    Fast ring gradient start.
 * @param ring2Color    Medium ring gradient start.
 * @param ring3Color    Slow ring gradient start.
 * @param showParticles Whether to draw floating particle sparkles.
 */
@Composable
fun ThinkingOrb(
    modifier: Modifier = Modifier,
    size: Dp = 120.dp,
    coreColor: Color = PremiumColorValues.NeonBlue,
    ring1Color: Color = PremiumColorValues.NeonBlue,
    ring2Color: Color = PremiumColorValues.ElectricPurple,
    ring3Color: Color = PremiumColorValues.NeonViolet,
    showParticles: Boolean = true,
) {
    val reducedMotion = LocalReducedMotion.current

    // ── Continuous rotation angles ─────────────────────────────────────────
    val infiniteTransition = rememberInfiniteTransition(label = "thinkingOrb")

    val ring1Angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (reducedMotion) 0f else 360f,
        animationSpec = if (reducedMotion) {
            infiniteRepeatable(tween(Int.MAX_VALUE, easing = LinearEasing), RepeatMode.Restart)
        } else {
            infiniteRepeatable(
                animation = tween(PremiumTokens.OrbRingFast, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            )
        },
        label = "ring1Angle",
    )

    val ring2Angle by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = if (reducedMotion) 360f else 0f,
        animationSpec = if (reducedMotion) {
            infiniteRepeatable(tween(Int.MAX_VALUE, easing = LinearEasing), RepeatMode.Restart)
        } else {
            infiniteRepeatable(
                animation = tween(PremiumTokens.OrbRingMedium, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            )
        },
        label = "ring2Angle",
    )

    val ring3Angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (reducedMotion) 0f else 360f,
        animationSpec = if (reducedMotion) {
            infiniteRepeatable(tween(Int.MAX_VALUE, easing = LinearEasing), RepeatMode.Restart)
        } else {
            infiniteRepeatable(
                animation = tween(PremiumTokens.OrbRingSlow, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            )
        },
        label = "ring3Angle",
    )


    // Gentle pulse for the core glow
    val corePulse by infiniteTransition.animateFloat(
        initialValue = 0.88f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "corePulse",
    )

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val cx = this.size.width / 2f
            val cy = this.size.height / 2f
            val orbRadius = this.size.minDimension * 0.28f
            val outerRadius = this.size.minDimension * 0.46f

            // ── Outer ambient glow ────────────────────────────────────────
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        coreColor.copy(alpha = 0.18f * corePulse),
                        coreColor.copy(alpha = 0.06f * corePulse),
                        Color.Transparent,
                    ),
                    center = Offset(cx, cy),
                    radius = outerRadius * 1.6f,
                ),
                center = Offset(cx, cy),
                radius = outerRadius * 1.6f,
            )

            // ── Ring 3 (background / slowest) – flat plane ─────────────
            drawOrbitalRing(
                cx = cx, cy = cy,
                radiusX = outerRadius * 0.95f,
                radiusY = outerRadius * 0.28f,  // thin Y = near-horizontal ellipse
                rotationDeg = ring3Angle,
                tiltDeg = 15f,
                color = ring3Color,
                strokeWidth = 1.8.dp.toPx(),
                alpha = 0.55f,
            )

            // ── Ring 2 (mid / counter-rotating) – steep tilt ───────────
            drawOrbitalRing(
                cx = cx, cy = cy,
                radiusX = outerRadius * 0.88f,
                radiusY = outerRadius * 0.42f,
                rotationDeg = ring2Angle,
                tiltDeg = 55f,
                color = ring2Color,
                strokeWidth = 2.2.dp.toPx(),
                alpha = 0.70f,
            )

            // ── Core orb (radial gradient sphere illusion) ─────────────
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.9f),
                        coreColor.copy(alpha = 0.95f),
                        coreColor.copy(alpha = 0.6f),
                        ring2Color.copy(alpha = 0.3f),
                        Color.Transparent,
                    ),
                    center = Offset(cx - orbRadius * 0.2f, cy - orbRadius * 0.2f),
                    radius = orbRadius * 1.4f,
                ),
                center = Offset(cx, cy),
                radius = orbRadius * corePulse,
            )

            // ── Ring 1 (foreground / fastest) – moderate tilt ──────────
            drawOrbitalRing(
                cx = cx, cy = cy,
                radiusX = outerRadius * 0.80f,
                radiusY = outerRadius * 0.33f,
                rotationDeg = ring1Angle,
                tiltDeg = 30f,
                color = ring1Color,
                strokeWidth = 2.8.dp.toPx(),
                alpha = 0.85f,
            )

            // ── Particle sparkles ──────────────────────────────────────
            if (showParticles && !reducedMotion) {
                drawOrbParticles(
                    cx = cx,
                    cy = cy,
                    orbitRadius = outerRadius * 1.05f,
                    rotationDeg = ring1Angle * 0.7f,
                    color = coreColor,
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Helper: Orbital Ring
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Draws an elliptical ring to simulate a 3-D orbit.
 *
 * The ring is rendered as a gradient arc (bright at the top, fading at the
 * bottom) and then rotated by [rotationDeg] + a canvas [tiltDeg] skew to
 * give the impression of depth.
 */
private fun DrawScope.drawOrbitalRing(
    cx: Float,
    cy: Float,
    radiusX: Float,
    radiusY: Float,
    rotationDeg: Float,
    tiltDeg: Float,
    color: Color,
    strokeWidth: Float,
    alpha: Float,
) {
    // Draw the elliptical arc rotated to simulate 3-D tilt
    rotate(degrees = tiltDeg, pivot = Offset(cx, cy)) {
        rotate(degrees = rotationDeg, pivot = Offset(cx, cy)) {
            // Top half – bright
            drawArc(
                brush = Brush.sweepGradient(
                    colors = listOf(
                        color.copy(alpha = alpha),
                        color.copy(alpha = alpha * 0.8f),
                        color.copy(alpha = alpha * 0.3f),
                        color.copy(alpha = 0f),
                        color.copy(alpha = 0f),
                        color.copy(alpha = alpha * 0.3f),
                        color.copy(alpha = alpha * 0.8f),
                        color.copy(alpha = alpha),
                    ),
                    center = Offset(cx, cy),
                ),
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(cx - radiusX, cy - radiusY),
                size = Size(radiusX * 2f, radiusY * 2f),
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Helper: Particle sparkles
// ─────────────────────────────────────────────────────────────────────────────

private const val PARTICLE_COUNT = 6

private fun DrawScope.drawOrbParticles(
    cx: Float,
    cy: Float,
    orbitRadius: Float,
    rotationDeg: Float,
    color: Color,
) {
    for (i in 0 until PARTICLE_COUNT) {
        val baseAngle = (360f / PARTICLE_COUNT) * i
        val angle = Math.toRadians((baseAngle + rotationDeg).toDouble())
        val px = cx + (orbitRadius * cos(angle)).toFloat()
        val py = cy + (orbitRadius * sin(angle)).toFloat()
        val particleAlpha = 0.6f * (1f - (i % 3) * 0.2f)
        val particleRadius = 2.5f * (1f - (i % 2) * 0.3f)

        drawCircle(
            color = color.copy(alpha = particleAlpha),
            radius = particleRadius,
            center = Offset(px, py),
        )
    }
}
