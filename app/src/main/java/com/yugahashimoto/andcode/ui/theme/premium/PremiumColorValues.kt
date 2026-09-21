package com.yugahashimoto.andcode.ui.theme.premium

import androidx.compose.ui.graphics.Color

/**
 * Premium color palette for AndCode v2.0.
 * Neon-focused, high-contrast colors for dark-theme-first UI.
 */
object PremiumColorValues {

    // ── Neon accent colors ────────────────────────────────────────────────────

    /** Primary electric blue – main brand accent. */
    val NeonBlue = Color(0xFF00D4FF)

    /** Blue-violet transition. */
    val NeonBlueViolet = Color(0xFF4C8BFF)

    /** Mid violet. */
    val NeonViolet = Color(0xFF9D5CFF)

    /** Violet-purple transition. */
    val NeonVioletPurple = Color(0xFFC44DFF)

    /** Secondary electric purple. */
    val ElectricPurple = Color(0xFFB84CFF)

    /** Success/confirmation green. */
    val NeonGreen = Color(0xFF00FF88)

    /** Warning amber. */
    val NeonAmber = Color(0xFFFFB800)

    /** Error/destructive red. */
    val NeonRed = Color(0xFFFF3366)

    // ── Semantic aliases ──────────────────────────────────────────────────────

    val Primary = NeonBlue
    val Secondary = ElectricPurple
    val Success = NeonGreen
    val Warning = NeonAmber
    val Error = NeonRed

    // ── Surface tints (for glass morphism) ────────────────────────────────────

    val GlassWhite = Color.White.copy(alpha = 0.08f)
    val GlassBorder = Color.White.copy(alpha = 0.15f)
    val GlassHighlight = Color.White.copy(alpha = 0.04f)
}
