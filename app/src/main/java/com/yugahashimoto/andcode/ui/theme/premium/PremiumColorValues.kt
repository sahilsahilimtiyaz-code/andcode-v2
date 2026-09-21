package com.yugahashimoto.andcode.ui.theme.premium

import androidx.compose.ui.graphics.Color

/**
 * Premium color palette for AndCode v2.0.
 * Neon-focused, high-contrast colors for dark-theme-first UI.
 */
object PremiumColorValues {

    // ── Neon accent colors ────────────────────────────────────────────────────

    /** Primary electric blue – main brand accent. */
    val NEON_BLUE = Color(0xFF00D4FF)

    /** Blue-violet transition. */
    val NEON_BLUE_VIOLET = Color(0xFF4C8BFF)

    /** Mid violet. */
    val NEON_VIOLET = Color(0xFF9D5CFF)

    /** Violet-purple transition. */
    val NEON_VIOLET_PURPLE = Color(0xFFC44DFF)

    /** Secondary electric purple. */
    val ELECTRIC_PURPLE = Color(0xFFB84CFF)

    /** Success/confirmation green. */
    val NEON_GREEN = Color(0xFF00FF88)

    /** Warning amber. */
    val NEON_AMBER = Color(0xFFFFB800)

    /** Error/destructive red. */
    val NEON_RED = Color(0xFFFF3366)

    // ── Semantic aliases ──────────────────────────────────────────────────────

    val PRIMARY = NEON_BLUE
    val SECONDARY = ELECTRIC_PURPLE
    val SUCCESS = NEON_GREEN
    val WARNING = NEON_AMBER
    val ERROR = NEON_RED

    // ── Surface tints (for glass morphism) ────────────────────────────────────

    val GLASS_WHITE = Color.White.copy(alpha = 0.08f)
    val GLASS_BORDER = Color.White.copy(alpha = 0.15f)
    val GLASS_HIGHLIGHT = Color.White.copy(alpha = 0.04f)
}