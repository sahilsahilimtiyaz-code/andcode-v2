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
    const val DURATION_XS_FAST: Int = 80

    /** Fast state transition: button press feedback. */
    const val DURATION_FAST: Int = 200

    /** Default transition: card expand/collapse, fade. */
    const val DURATION_MEDIUM: Int = 400

    /** Deliberate motion: sheet enter, screen push. */
    const val DURATION_SLOW: Int = 600

    /** Cinematic motion: splash, onboarding. */
    const val DURATION_XSLOW: Int = 900

    // ── Stagger (milliseconds) ─────────────────────────────────────────────────
    /** Delay between adjacent list items / timeline nodes. */
    const val STAGGER_ITEM: Int = 60

    /** Delay between typing indicator dots. */
    const val STAGGER_DOT: Int = 60

    // ── Easing curves ─────────────────────────────────────────────────────────
    /** Standard decelerate – objects entering the screen. */
    val EASING_DECELERATE: Easing = CubicBezierEasing(0f, 0f, 0.2f, 1f)

    /** Standard accelerate – objects leaving the screen. */
    val EASING_ACCELERATE: Easing = CubicBezierEasing(0.4f, 0f, 1f, 1f)

    /** Emphasised decelerate (M3 standard) – primary transitions. */
    val EASING_EMPHASIZED: Easing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)

    /** Snappy spring feel for bouncy reveals. */
    val EASING_BOUNCY: Easing = CubicBezierEasing(0.34f, 1.56f, 0.64f, 1f)

    /** Linear – used only for infinite rotations. */
    val EASING_LINEAR: Easing = CubicBezierEasing(0f, 0f, 1f, 1f)

    // ── Tween specs ───────────────────────────────────────────────────────────
    fun tweenFast(delayMs: Int = 0) =
        androidx.compose.animation.core.tween<Float>(durationMillis = DURATION_FAST, delayMillis = delayMs, easing = EASING_DECELERATE)

    fun tweenMedium(delayMs: Int = 0) =
        androidx.compose.animation.core.tween<Float>(durationMillis = DURATION_MEDIUM, delayMillis = delayMs, easing = EASING_EMPHASIZED)

    fun tweenSlow(delayMs: Int = 0) =
        androidx.compose.animation.core.tween<Float>(durationMillis = DURATION_SLOW, delayMillis = delayMs, easing = EASING_DECELERATE)

    // ── Spring specs ──────────────────────────────────────────────────────────
    /** Gentle spring for large surface reveals (cards, sheets). */
    val SPRING_GENTLE = androidx.compose.animation.core.spring<Float>(
        dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
        stiffness = androidx.compose.animation.core.Spring.StiffnessLow,
    )

    /** Responsive spring for interactive elements (buttons, toggles). */
    val SPRING_RESPONSIVE = androidx.compose.animation.core.spring<Float>(
        dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
        stiffness = androidx.compose.animation.core.Spring.StiffnessMedium,
    )

    /** Snappy spring for micro-interactions (typing dots, status changes). */
    val SPRING_SNAPPY = androidx.compose.animation.core.spring<Float>(
        dampingRatio = androidx.compose.animation.core.Spring.DampingRatioNoBouncy,
        stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow,
    )

    // ── Corner radii ──────────────────────────────────────────────────────────
    /** Chips, badges, small buttons. */
    val RADIUS_SMALL: Dp = 8.dp

    /** Input fields, compact cards. */
    val RADIUS_MEDIUM: Dp = 16.dp

    /** Standard cards, glass panels. */
    val RADIUS_LARGE: Dp = 20.dp

    /** Prominent panels, bottom sheets, large modals. */
    val RADIUS_XLARGE: Dp = 24.dp

    /** Pill / fully rounded elements (FAB, send button). */
    val RADIUS_PILL: Dp = 50.dp

    // ── Elevation / blur ──────────────────────────────────────────────────────
    /** Glow radius for neon shadows (drawn in Canvas, not Material shadow). */
    val GLOW_RADIUS: Dp = 12.dp

    /** Glass blur radius hint (approximated via layered alpha in Compose). */
    val BLUR_RADIUS: Dp = 20.dp

    // ── Border ────────────────────────────────────────────────────────────────
    /** Width of the neon gradient border strokes. */
    val BORDER_WIDTH: Dp = 1.5.dp

    // ── Animation cycle periods (milliseconds) ────────────────────────────────
    /** Fast orbital ring rotation cycle. */
    const val ORB_RING_FAST: Int = 8_000

    /** Medium orbital ring rotation cycle. */
    const val ORB_RING_MEDIUM: Int = 12_000

    /** Slow orbital ring rotation cycle. */
    const val ORB_RING_SLOW: Int = 16_000

    /** Typing indicator pulse cycle. */
    const val TYPING_PULSE: Int = 600

    /** Neon border sweep cycle (full 360°). */
    const val BORDER_SWEEP: Int = 2_000

    /** Shimmer sweep cycle. */
    const val SHIMMER_SWEEP: Int = 1_200
}