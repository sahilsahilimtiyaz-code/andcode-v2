package com.yugahashimoto.andcode.ui.theme.premium

import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocal
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext

/**
 * Whether the user has disabled system animations.
 *
 * The default is false. The value is supplied by the theme because reading
 * LocalContext is only valid from a composable context.
 */
// ktlint:disable property-naming
val LOCAL_REDUCED_MOTION: CompositionLocal<Boolean> =
    staticCompositionLocalOf { false }
// ktlint:enable property-naming

@Composable
fun rememberReducedMotion(): Boolean {
    val context = LocalContext.current

    return remember(context) {
        runCatching {
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.TRANSITION_ANIMATION_SCALE,
                1f,
            ) == 0f
        }.getOrDefault(false)
    }
}