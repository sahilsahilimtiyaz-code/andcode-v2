package com.yugahashimoto.andcode.ui.theme.premium

import androidx.compose.runtime.CompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.core.os.userReducedMotionEnabled
import androidx.compose.ui.platform.LocalContext

/**
 * CompositionLocal that provides whether the user has reduced motion enabled
 * in system accessibility settings.
 *
 * Usage:
 *   val reducedMotion = LocalReducedMotion.current
 *
 * Reads from Android's "Remove animations" accessibility setting.
 */
val LocalReducedMotion: CompositionLocal<Boolean> = staticCompositionLocalOf {
    val context = LocalContext.current
    context.userReducedMotionEnabled()
}