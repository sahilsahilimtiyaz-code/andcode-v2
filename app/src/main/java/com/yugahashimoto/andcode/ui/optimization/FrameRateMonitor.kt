package com.yugahashimoto.andcode.ui.optimization

import android.content.Context
import android.os.Build
import android.view.Display
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yugahashimoto.andcode.ui.theme.premium.LocalReducedMotion
import com.yugahashimoto.andcode.ui.theme.premium.PremiumColorValues
import com.yugahashimoto.andcode.ui.theme.premium.PremiumTokens
import com.yugahashimoto.andcode.ui.theme.premium.glassPanel

/**
 * Snapshot of real-time display refresh rate and frame rendering statistics.
 */
@Stable
data class FrameRateStats(
    val currentFps: Float = 60f,
    val targetFps: Int = 60,
    val frameDurationMs: Float = 16.6f,
    val frameBudgetMs: Float = 16.6f,
    val is120HzSupported: Boolean = false,
    val droppedFrames: Long = 0L,
) {
    val isSmooth: Boolean
        get() = currentFps >= (targetFps * 0.92f)
}

/**
 * Monitors real-time frame rates and detects 60Hz/90Hz/120Hz display modes.
 *
 * Runs a low-overhead `withFrameNanos` loop that calculates frame intervals
 * and smoothed FPS using an exponential moving average.
 *
 * @param sampleIntervalFrames Number of frames between FPS stat updates (default: 15).
 */
@Composable
fun rememberFrameRateMonitor(
    sampleIntervalFrames: Int = 15,
): FrameRateStats {
    val context = LocalContext.current
    val reducedMotion = LocalReducedMotion.current

    // Query hardware display target refresh rate
    val targetFps = remember(context) {
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
        val display = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            context.display ?: wm?.defaultDisplay
        } else {
            @Suppress("DEPRECATION")
            wm?.defaultDisplay
        }
        val rate = display?.refreshRate?.toInt() ?: 60
        when {
            rate >= 115 -> 120
            rate >= 85 -> 90
            else -> 60
        }
    }

    val frameBudgetMs = remember(targetFps) { 1000f / targetFps }

    var currentFps by remember { mutableFloatStateOf(targetFps.toFloat()) }
    var currentFrameDurationMs by remember { mutableFloatStateOf(frameBudgetMs) }
    var totalDroppedFrames by remember { mutableLongStateOf(0L) }

    LaunchedEffect(reducedMotion, targetFps) {
        if (reducedMotion) return@LaunchedEffect

        var lastFrameNanos = 0L
        var frameCount = 0
        var accumulatedNanos = 0L

        while (true) {
            withFrameNanos { nowNanos ->
                if (lastFrameNanos != 0L) {
                    val frameDeltaNanos = nowNanos - lastFrameNanos
                    accumulatedNanos += frameDeltaNanos
                    frameCount++

                    val frameDeltaMs = frameDeltaNanos / 1_000_000f
                    if (frameDeltaMs > (frameBudgetMs * 1.5f)) {
                        totalDroppedFrames++
                    }

                    if (frameCount >= sampleIntervalFrames) {
                        val avgFrameDurationNanos = accumulatedNanos.toDouble() / frameCount
                        val calculatedFps = (1_000_000_000.0 / avgFrameDurationNanos).toFloat()

                        // Exponential moving average filter (alpha = 0.3)
                        currentFps = currentFps * 0.7f + calculatedFps * 0.3f
                        currentFrameDurationMs = (avgFrameDurationNanos / 1_000_000.0).toFloat()

                        frameCount = 0
                        accumulatedNanos = 0L
                    }
                }
                lastFrameNanos = nowNanos
            }
        }
    }

    return FrameRateStats(
        currentFps = currentFps,
        targetFps = targetFps,
        frameDurationMs = currentFrameDurationMs,
        frameBudgetMs = frameBudgetMs,
        is120HzSupported = targetFps >= 115,
        droppedFrames = totalDroppedFrames,
    )
}

/**
 * Frosted glass HUD badge showing real-time frame rate.
 *
 * Useful for performance inspection and testing 60/120fps smoothness.
 */
@Composable
fun FrameRateBadge(
    stats: FrameRateStats,
    modifier: Modifier = Modifier,
) {
    val fpsColor = when {
        stats.currentFps >= (stats.targetFps * 0.95f) -> PremiumColorValues.NeonGreen
        stats.currentFps >= (stats.targetFps * 0.80f) -> PremiumColorValues.NeonAmber
        else -> PremiumColorValues.NeonRed
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(PremiumTokens.RadiusSmall))
            .glassPanel(
                shape = RoundedCornerShape(PremiumTokens.RadiusSmall),
                fillColor = PremiumColorValues.SpaceCard.copy(alpha = 0.85f),
                borderColor = PremiumColorValues.GlassBorder,
                borderWidth = 0.8.dp,
            )
            .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            // Indicator dot
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(fpsColor),
            )

            Text(
                text = "${stats.currentFps.toInt()} FPS",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp,
                ),
                color = fpsColor,
            )

            Text(
                text = "• ${String.format("%.1f", stats.frameDurationMs)}ms",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                ),
                color = PremiumColorValues.ForegroundHint,
            )
        }
    }
}
