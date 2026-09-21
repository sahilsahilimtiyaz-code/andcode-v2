package com.yugahashimoto.andcode.ui.theme.premium

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Design-token constants for AndCode v2.0 Premium UI.
 *
 * This is the single source of truth for:
 *   – Animation durations & easings
 *   – Corner radii
 *   – Elevation / blur levels
 *   – Spring physics specs
 *   – Stagger timings
 *
 * Keep values as `val` objects so they can be referenced from both
 * Modifier extensions and Composable animations without reallocation.
 */
object PremiumTokens {

    // ── Durations (milliseconds) ──────────────────────────────────────────────
    /** Micro interaction: icon tap, ripple acknowledge. */
    const val DurationXsFast: Int = 80

    /** Fast state transition: button press feedback. */
    const val DurationFast: Int = 200

    /** Default transition: card expand/collapse, fade. */
    const val DurationMedium: Int = 400

    /** Deliberate motion: sheet enter, screen push. */
    const val DurationSlow: Int = 600

    /** Cinematic motion: splash, onboarding. */
    const val DurationXSlow: Int = 900

    // ── Stagger (milliseconds) ─────────────────────────────────────────────────
    /** Delay between adjacent list items / timeline nodes. */
    const val StaggerItem: Int = 60

    /** Delay between typing indicator dots. */
    const val StaggerDot: Int = 60

    // ── Easing curves ─────────────────────────────────────────────────────────
    /** Standard decelerate – objects entering the screen. */
    val EasingDecelerate: Easing = CubicBezierEasing(0f, 0f, 0.2f, 1f)

    /** Standard accelerate – objects leaving the screen. */
    val EasingAccelerate: Easing = CubicBezierEasing(0.4f, 0f, 1f, 1f)

    /** Emphasised decelerate (M3 standard) – primary transitions. */
    val EasingEmphasized: Easing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)

    /** Snappy spring feel for bouncy reveals. */
    val EasingBouncy: Easing = CubicBezierEasing(0.34f, 1.56f, 0.64f, 1f)

    /** Linear – used only for infinite rotations. */
    val EasingLinear: Easing = CubicBezierEasing(0f, 0f, 1f, 1f)

    // ── Tween specs ───────────────────────────────────────────────────────────
    fun tweenFast(delayMs: Int = 0) =
        androidx.compose.animation.core.tween<Float>(durationMillis = DurationFast, delayMillis = delayMs, easing = EasingDecelerate)

    fun tweenMedium(delayMs: Int = 0) =
        androidx.compose.animation.core.tween<Float>(durationMillis = DurationMedium, delayMillis = delayMs, easing = EasingEmphasized)

    fun tweenSlow(delayMs: Int = 0) =
        androidx.compose.animation.core.tween<Float>(durationMillis = DurationSlow, delayMillis = delayMs, easing = EasingDecelerate)

    // ── Spring specs ──────────────────────────────────────────────────────────
    /** Gentle spring for large surface reveals (cards, sheets). */
    val SpringGentle = androidx.compose.animation.core.spring<Float>(
        dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
        stiffness = androidx.compose.animation.core.Spring.StiffnessLow,
    )

    /** Responsive spring for interactive elements (buttons, toggles). */
    val SpringResponsive = androidx.compose.animation.core.spring<Float>(
        dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
        stiffness = androidx.compose.animation.core.Spring.StiffnessMedium,
    )

    /** Snappy spring for micro-interactions (typing dots, status changes). */
    val SpringSnappy = androidx.compose.animation.core.spring<Float>(
        dampingRatio = androidx.compose.animation.core.Spring.DampingRatioNoBouncy,
        stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow,
    )

    // ── Corner radii ──────────────────────────────────────────────────────────
    /** Chips, badges, small buttons. */
    val RadiusSmall: Dp = 8.dp

    /** Input fields, compact cards. */
    val RadiusMedium: Dp = 16.dp

    /** Standard cards, glass panels. */
    val RadiusLarge: Dp = 20.dp

    /** Prominent panels, bottom sheets, large modals. */
    val RadiusXLarge: Dp = 24.dp

    /** Pill / fully rounded elements (FAB, send button). */
    val RadiusPill: Dp = 50.dp

    // ── Elevation / blur ──────────────────────────────────────────────────────
    /** Glow radius for neon shadows (drawn in Canvas, not Material shadow). */
    val GlowRadius: Dp = 12.dp

    /** Glass blur radius hint (approximated via layered alpha in Compose). */
    val BlurRadius: Dp = 20.dp

    // ── Border ────────────────────────────────────────────────────────────────
    /** Width of the neon gradient border strokes. */
    val BorderWidth: Dp = 1.5.dp

    // ── Animation cycle periods (milliseconds) ────────────────────────────────
    /** Fast orbital ring rotation cycle. */
    const val OrbRingFast: Int = 8_000

    /** Medium orbital ring rotation cycle. */
    const val OrbRingMedium: Int = 12_000

    /** Slow orbital ring rotation cycle. */
    const val OrbRingSlow: Int = 16_000

    /** Typing indicator pulse cycle. */
    const val TypingPulse: Int = 600

    /** Neon border sweep cycle (full 360°). */
    const val BorderSweep: Int = 2_000

    /** Shimmer sweep cycle. */
    const val ShimmerSweep: Int = 1_200
}