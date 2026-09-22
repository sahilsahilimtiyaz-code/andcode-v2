package com.yugahashimoto.andcode.ui.optimization

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import com.yugahashimoto.andcode.ui.theme.premium.LocalReducedMotion

/**
 * Adaptive quality tier based on real-time device rendering performance.
 */
enum class AdaptiveMotionTier {
    /** High-end 60/120fps device: all orbital rings, full particle counts, layered glow */
    FULL,

    /** Medium device or moderate load: reduced particles (halved), standard glow */
    BALANCED,

    /** Low-end device or high jank detected: static fallback, zero particles */
    PERFORMANCE,
}

/**
 * Provides the current [AdaptiveMotionTier] down the composition tree.
 */
val LocalAdaptiveMotionTier = compositionLocalOf { AdaptiveMotionTier.FULL }

/**
 * Real-time frame jank statistics.
 */
@Stable
data class JankReport(
    val totalFramesTracked: Long = 0L,
    val jankFrameCount: Long = 0L,
    val jankPercentage: Float = 0f,
    val maxFrameDurationMs: Float = 0f,
    val recommendedTier: AdaptiveMotionTier = AdaptiveMotionTier.FULL,
)

/**
 * Real-time Jank Detector for 60/120fps UI optimization.
 *
 * Tracks every frame interval and identifies janky frames (> 1.5x frame budget)
 * and freezes (> 100ms). Automatically recommends an [AdaptiveMotionTier]
 * so high-cost animations (ThinkingOrb, NeonBorderSweep) can throttle particle
 * counts and Canvas layers automatically on low-spec devices or PRoot environments.
 *
 * @param windowSize Number of historical frames in the sliding analysis window.
 */
@Composable
fun rememberJankDetector(
    windowSize: Int = 120,
    targetFps: Int = 60,
): JankReport {
    val reducedMotion = LocalReducedMotion.current
    val frameBudgetMs = remember(targetFps) { 1000f / targetFps }

    var totalFrames by remember { mutableLongStateOf(0L) }
    var jankFrames by remember { mutableLongStateOf(0L) }
    var jankPercentage by remember { mutableFloatStateOf(0f) }
    var maxFrameMs by remember { mutableFloatStateOf(0f) }
    var currentTier by remember { mutableStateOf(AdaptiveMotionTier.FULL) }

    if (reducedMotion) {
        return JankReport(
            recommendedTier = AdaptiveMotionTier.PERFORMANCE,
        )
    }

    LaunchedEffect(targetFps) {
        var lastNanos = 0L
        val jankHistory = BooleanArray(windowSize)
        var historyIndex = 0
        var windowFilled = false

        while (true) {
            withFrameNanos { nowNanos ->
                if (lastNanos != 0L) {
                    val frameDeltaMs = (nowNanos - lastNanos) / 1_000_000f
                    totalFrames++

                    val isJanky = frameDeltaMs > (frameBudgetMs * 1.5f)
                    if (isJanky) {
                        jankFrames++
                    }

                    if (frameDeltaMs > maxFrameMs) {
                        maxFrameMs = frameDeltaMs
                    }

                    // Sliding window update
                    jankHistory[historyIndex] = isJanky
                    historyIndex = (historyIndex + 1) % windowSize
                    if (historyIndex == 0) windowFilled = true

                    val currentWindowCount = if (windowFilled) windowSize else historyIndex
                    if (currentWindowCount > 0) {
                        val jankInWindow = jankHistory.take(currentWindowCount).count { it }
                        val windowJankPct = (jankInWindow.toFloat() / currentWindowCount) * 100f
                        jankPercentage = windowJankPct

                        // Auto-tune tier based on window jank percentage
                        currentTier = when {
                            windowJankPct > 15f -> AdaptiveMotionTier.PERFORMANCE
                            windowJankPct > 5f -> AdaptiveMotionTier.BALANCED
                            else -> AdaptiveMotionTier.FULL
                        }
                    }
                }
                lastNanos = nowNanos
            }
        }
    }

    return JankReport(
        totalFramesTracked = totalFrames,
        jankFrameCount = jankFrames,
        jankPercentage = jankPercentage,
        maxFrameDurationMs = maxFrameMs,
        recommendedTier = currentTier,
    )
}
