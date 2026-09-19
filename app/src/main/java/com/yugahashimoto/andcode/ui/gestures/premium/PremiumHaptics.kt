package com.yugahashimoto.andcode.ui.gestures.premium

import android.view.HapticFeedbackConstants
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView

/**
 * Premium haptic feedback controller for gesture navigation.
 *
 * Provides tactile feedback for:
 * - Swipe threshold crossing (archive/dismiss)
 * - Long-press context menu invocation
 * - Two-finger project switch transitions
 * - Action selection and completion
 */
class PremiumHaptics(
    private val composeHaptics: HapticFeedback,
    private val androidView: android.view.View?,
) {
    /** Tick when a gesture crosses its trigger threshold. */
    fun triggerThreshold() {
        try {
            androidView?.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                ?: composeHaptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        } catch (_: Throwable) {
            // Safe fallback in PRoot/custom environment
        }
    }

    /** Subtle click when an item or menu action is tapped. */
    fun triggerSelection() {
        try {
            androidView?.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                ?: composeHaptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        } catch (_: Throwable) {
            // Safe fallback
        }
    }

    /** Stronger buzz when a long-press activates a context menu. */
    fun triggerLongPress() {
        try {
            androidView?.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                ?: composeHaptics.performHapticFeedback(HapticFeedbackType.LongPress)
        } catch (_: Throwable) {
            // Safe fallback
        }
    }

    /** Double pulse on success / completion. */
    fun triggerSuccess() {
        try {
            androidView?.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                ?: composeHaptics.performHapticFeedback(HapticFeedbackType.LongPress)
        } catch (_: Throwable) {
            // Safe fallback
        }
    }
}

/**
 * Remembers a [PremiumHaptics] instance in the current composition.
 */
@Composable
fun rememberPremiumHaptics(): PremiumHaptics {
    val hapticFeedback = LocalHapticFeedback.current
    val view = LocalView.current
    return remember(hapticFeedback, view) {
        PremiumHaptics(hapticFeedback, view)
    }
}
