package com.yugahashimoto.andcode.ui.theme.premium

import android.content.Context
import android.provider.Settings
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.yugahashimoto.andcode.ui.theme.buildTypography

// ── Composition Locals ────────────────────────────────────────────────────────

/**
 * Provides the active [PremiumGlassColors] down the composition tree.
 *
 * Access via `LocalPremiumColors.current`.
 */
val LocalPremiumColors = staticCompositionLocalOf { PremiumGlassColors() }

/**
 * Provides whether reduced motion is requested (accessibility setting).
 *
 * Wire this up from `LocalContext → Settings` at the root of the app.
 * Composables that animate should read this and opt for instant transitions
 * when `true`.
 */
val LocalReducedMotion = compositionLocalOf { false }

/**
 * Reads the system accessibility "Remove animations" setting and provides it to
 * [LocalReducedMotion].
 *
 * The "Remove animations" setting corresponds to the three animation scales
 * (window, transition, animator duration) all being set to 0, or the
 * AccessibilityManager's reduce motion flag on API 31+.
 *
 * Usage:
 * ```kotlin
 * GlassMorphismTheme {
 *     ProvideReducedMotionFromSystem {
 *         AppContent()
 *     }
 * }
 * ```
 */
@Composable
fun ProvideReducedMotionFromSystem(content: @Composable () -> Unit) {
    val context = LocalContext.current

    var reducedMotion by remember(context) {
        mutableStateOf(false)
    }

    // Read system setting on first composition and whenever context changes
    androidx.compose.runtime.LaunchedEffect(context) {
        reducedMotion = checkSystemReduceMotion(context)
    }

    CompositionLocalProvider(LocalReducedMotion provides reducedMotion) {
        content()
    }
}

private fun checkSystemReduceMotion(context: Context): Boolean {
    val contentResolver = context.contentResolver

    // Check the three animation scales - if all are 0, animations are removed
    val windowScale = Settings.Global.getFloat(contentResolver, Settings.Global.WINDOW_ANIMATION_SCALE, 1f)
    val transitionScale = Settings.Global.getFloat(contentResolver, Settings.Global.TRANSITION_ANIMATION_SCALE, 1f)
    val animatorScale = Settings.Global.getFloat(contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)

    return windowScale == 0f && transitionScale == 0f && animatorScale == 0f
}

// ── Color snapshot ────────────────────────────────────────────────────────────

/**
 * Snapshot of all semantic premium color roles derived from [PremiumColorValues].
 *
 * Passed through the composition via [LocalPremiumColors].
 */
data class PremiumGlassColors(
    // Backgrounds
    val backgroundStart: Color = PremiumColorValues.SpaceBlack,
    val backgroundEnd: Color = PremiumColorValues.SpaceDeep,
    val cardBackground: Color = PremiumColorValues.SpaceCard,

    // Glass surfaces
    val glassSurface: Color = PremiumColorValues.GlassSurface,
    val glassSurfaceHigh: Color = PremiumColorValues.GlassSurfaceHigh,
    val glassSurfaceLow: Color = PremiumColorValues.GlassSurfaceLow,
    val glassBorder: Color = PremiumColorValues.GlassBorder,
    val glassBorderActive: Color = PremiumColorValues.GlassBorderActive,

    // Primary gradient
    val neonBlue: Color = PremiumColorValues.NeonBlue,
    val electricPurple: Color = PremiumColorValues.ElectricPurple,
    val neonViolet: Color = PremiumColorValues.NeonViolet,

    // Status
    val success: Color = PremiumColorValues.NeonGreen,
    val successGlow: Color = PremiumColorValues.NeonGreenGlow,
    val successTint: Color = PremiumColorValues.NeonGreenTint,

    val error: Color = PremiumColorValues.NeonRed,
    val errorGlow: Color = PremiumColorValues.NeonRedGlow,
    val errorTint: Color = PremiumColorValues.NeonRedTint,

    val warning: Color = PremiumColorValues.NeonAmber,
    val warningGlow: Color = PremiumColorValues.NeonAmberGlow,
    val warningTint: Color = PremiumColorValues.NeonAmberTint,

    val info: Color = PremiumColorValues.NeonInfo,

    // Text
    val foreground: Color = PremiumColorValues.ForegroundPrimary,
    val foregroundMuted: Color = PremiumColorValues.ForegroundMuted,
    val foregroundHint: Color = PremiumColorValues.ForegroundHint,
) {
    // ── Gradient Brushes (derived, not stored) ─────────────────────────────
    /** Full left-to-right primary gradient: Neon Blue → Electric Purple. */
    val primaryGradient: Brush
        get() = Brush.horizontalGradient(listOf(neonBlue, neonViolet, electricPurple))

    /** Vertical variant of the primary gradient. */
    val primaryGradientVertical: Brush
        get() = Brush.verticalGradient(listOf(neonBlue, neonViolet, electricPurple))

    /** Radial glow brush centred at the origin (used for orb/glow effects). */
    val orbGlow: Brush
        get() = Brush.radialGradient(
            colors = listOf(
                neonBlue.copy(alpha = 0.9f),
                neonViolet.copy(alpha = 0.5f),
                electricPurple.copy(alpha = 0.0f),
            ),
        )

    /** Success radial glow (Build Successful card). */
    val successGlowBrush: Brush
        get() = Brush.radialGradient(
            colors = listOf(
                success.copy(alpha = 0.4f),
                success.copy(alpha = 0.0f),
            ),
        )

    /** Background gradient covering the whole screen. */
    val backgroundGradient: Brush
        get() = Brush.verticalGradient(listOf(backgroundStart, backgroundEnd))
}

// ── Material3 color scheme ────────────────────────────────────────────────────

private fun buildPremiumColorScheme(colors: PremiumGlassColors) = darkColorScheme(
    primary = colors.neonBlue,
    onPrimary = PremiumColorValues.SpaceBlack,
    primaryContainer = colors.neonBlue.copy(alpha = 0.15f),
    onPrimaryContainer = colors.neonBlue,
    secondary = colors.electricPurple,
    onSecondary = PremiumColorValues.SpaceBlack,
    secondaryContainer = colors.electricPurple.copy(alpha = 0.15f),
    onSecondaryContainer = colors.electricPurple,
    tertiary = colors.neonViolet,
    onTertiary = PremiumColorValues.SpaceBlack,
    tertiaryContainer = colors.neonViolet.copy(alpha = 0.15f),
    onTertiaryContainer = colors.neonViolet,
    background = colors.backgroundStart,
    onBackground = colors.foreground,
    surface = PremiumColorValues.SpaceElevated,
    onSurface = colors.foreground,
    surfaceVariant = PremiumColorValues.SpaceCard,
    onSurfaceVariant = colors.foregroundMuted,
    outline = colors.glassBorder,
    outlineVariant = colors.glassBorderActive,
    error = colors.error,
    onError = PremiumColorValues.SpaceBlack,
    errorContainer = colors.errorTint,
    onErrorContainer = colors.error,
    surfaceContainerLowest = PremiumColorValues.SpaceBlack,
    surfaceContainerLow = PremiumColorValues.SpaceDeep,
    surfaceContainer = PremiumColorValues.SpaceCard,
    surfaceContainerHigh = PremiumColorValues.SpaceElevated,
    surfaceContainerHighest = PremiumColorValues.SpaceElevatedPlus,
    scrim = PremiumColorValues.Scrim,
    inverseSurface = colors.foreground,
    inverseOnSurface = PremiumColorValues.SpaceBlack,
    inversePrimary = colors.neonBlue,
)

// ── Theme composable ──────────────────────────────────────────────────────────

/**
 * Glass-morphism dark theme wrapper for Wave E premium UI.
 *
 * Wrap screens/components that should receive the premium design system with
 * this composable. It provides:
 *   - Material3 [darkColorScheme] tuned to deep-space / neon palette
 *   - [LocalPremiumColors] with [PremiumGlassColors]
 *   - [LocalReducedMotion] toggle for accessibility compliance
 *
 * @param reducedMotion Pass `true` (e.g. from Android accessibility settings)
 *        to suppress all non-essential animations.
 * @param content The child composable tree.
 */
@Composable
fun GlassMorphismTheme(
    reducedMotion: Boolean = false,
    content: @Composable () -> Unit,
) {
    val premiumColors = PremiumGlassColors()
    val colorScheme = buildPremiumColorScheme(premiumColors)
    val typography = buildTypography()

    CompositionLocalProvider(
        LocalPremiumColors provides premiumColors,
        LocalReducedMotion provides reducedMotion,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = typography,
            content = content,
        )
    }
}

// ── Convenience accessor ──────────────────────────────────────────────────────

/** Shorthand to access premium colors from any composable context. */
val premiumColors: PremiumGlassColors
    @Composable get() = LocalPremiumColors.current

/** Shorthand to check reduced motion preference from any composable context. */
val isReducedMotion: Boolean
    @Composable get() = LocalReducedMotion.current
