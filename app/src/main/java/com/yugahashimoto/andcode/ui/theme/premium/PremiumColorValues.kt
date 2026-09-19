package com.yugahashimoto.andcode.ui.theme.premium

import androidx.compose.ui.graphics.Color

/**
 * Premium neon color palette for AndCode v2.0 "The Biggest Upgrade".
 *
 * All raw color constants live here. No hardcoded hex values are permitted
 * anywhere else in the UI layer – import from this object instead.
 *
 * Naming convention:
 *   NeonXxx   – vivid neon hue used for gradients / glows
 *   GlassXxx  – semi-transparent tints for glass-morphism surfaces
 *   SpaceXxx  – deep-space background tones
 */
object PremiumColorValues {

    // ── Background / Space ────────────────────────────────────────────────────
    /** Pure deep-space black, used as the darkest background stop. */
    val SpaceBlack = Color(0xFF000000)

    /** Midnight blue-black – lightest background stop for the gradient. */
    val SpaceDeep = Color(0xFF0A0A12)

    /** Dark card backdrop behind glass surfaces. */
    val SpaceCard = Color(0xFF0D0D1A)

    /** Slightly elevated surface used for nested panels. */
    val SpaceElevated = Color(0xFF12121F)

    /** Most elevated space surface – used for highest Material3 container tier. */
    val SpaceElevatedPlus = Color(0xFF1A1A2E)


    // ── Primary / Neon Blue ───────────────────────────────────────────────────
    /** Neon Cyan-Blue – primary gradient start, typing dots, glow aura. */
    val NeonBlue = Color(0xFF00D4FF)

    /** Slightly softened Neon Blue for readable text on dark surfaces. */
    val NeonBlueSoft = Color(0xFF29DAFF)

    /** Neon Blue at 25 % alpha – used for outer glow / shadow layers. */
    val NeonBlueGlow = Color(0x4000D4FF)

    /** Neon Blue at 12 % alpha – used for subtle glass tints. */
    val NeonBlueTint = Color(0x2000D4FF)

    /** Neon Blue-Violet step – intermediate stop in 5-dot typing gradient. */
    val NeonBlueViolet = Color(0xFF4DA8FF)

    // ── Secondary / Electric Purple ───────────────────────────────────────────
    /** Electric Purple – primary gradient end, neon border highlight. */
    val ElectricPurple = Color(0xFFB84CFF)

    /** Softer Purple variant for readable headings. */
    val ElectricPurpleSoft = Color(0xFFC56EFF)

    /** Electric Purple at 25 % alpha – glow layer. */
    val ElectricPurpleGlow = Color(0x40B84CFF)

    /** Electric Purple at 12 % alpha – subtle glass tint. */
    val ElectricPurpleTint = Color(0x20B84CFF)

    /** Violet-Purple step – intermediate stop in 5-dot typing gradient. */
    val NeonVioletPurple = Color(0xFFD060FF)

    // ── Mid-gradient ──────────────────────────────────────────────────────────
    /** Violet mid-stop between NeonBlue and ElectricPurple. */
    val NeonViolet = Color(0xFF7C5CFC)

    /** Bright magenta accent used in progress-bar segment highlights. */
    val NeonMagenta = Color(0xFFE040FB)

    // ── Status: Success ───────────────────────────────────────────────────────
    /** Neon Green – success icon, checkmarks, "Build Successful" card. */
    val NeonGreen = Color(0xFF00EE88)

    /** Neon Green at 25 % alpha – card background glow. */
    val NeonGreenGlow = Color(0x4000EE88)

    /** Neon Green at 15 % alpha – card surface fill. */
    val NeonGreenTint = Color(0x2600EE88)

    // ── Status: Error ─────────────────────────────────────────────────────────
    /** Neon Red – error borders, shake animation accent. */
    val NeonRed = Color(0xFFFF4757)

    /** Neon Red at 25 % alpha – error card glow. */
    val NeonRedGlow = Color(0x40FF4757)

    /** Neon Red at 15 % alpha – error card surface fill. */
    val NeonRedTint = Color(0x26FF4757)

    // ── Status: Warning ───────────────────────────────────────────────────────
    /** Neon Amber – warning pulse accent. */
    val NeonAmber = Color(0xFFFFB84D)

    /** Neon Amber at 25 % alpha – warning glow. */
    val NeonAmberGlow = Color(0x40FFB84D)

    /** Neon Amber at 15 % alpha – warning surface fill. */
    val NeonAmberTint = Color(0x26FFB84D)

    // ── Status: Info ─────────────────────────────────────────────────────────
    /** Cyan-blue used for info cards – same hue as NeonBlue for consistency. */
    val NeonInfo = Color(0xFF00D4FF)

    // ── Glass-morphism ────────────────────────────────────────────────────────
    /** Primary glass surface fill: white at 8 % alpha. */
    val GlassSurface = Color(0x14FFFFFF)

    /** Lighter glass variant: white at 10 % alpha. */
    val GlassSurfaceHigh = Color(0x1AFFFFFF)

    /** Darker glass variant for deeply nested panels: white at 6 % alpha. */
    val GlassSurfaceLow = Color(0x0FFFFFFF)

    /** Glass border: white at 18 % alpha for 1.5dp stroke. */
    val GlassBorder = Color(0x2EFFFFFF)

    /** Glass border active/hover state: white at 28 % alpha. */
    val GlassBorderActive = Color(0x47FFFFFF)

    // ── Foreground ────────────────────────────────────────────────────────────
    /** Primary text on dark glass surfaces. */
    val ForegroundPrimary = Color(0xFFECEDEE)

    /** Secondary / muted text. */
    val ForegroundMuted = Color(0xFF8A909E)

    /** Extra-muted placeholder / hint text. */
    val ForegroundHint = Color(0xFF4E5361)

    // ── Overlay ───────────────────────────────────────────────────────────────
    /** Semi-transparent black scrim used for modal backdrops. */
    val Scrim = Color(0xCC000000)
}
