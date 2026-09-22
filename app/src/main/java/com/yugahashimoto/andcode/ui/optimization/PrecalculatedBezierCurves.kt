package com.yugahashimoto.andcode.ui.optimization

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.runtime.Immutable

/**
 * Pre-calculated Bézier curve lookup table (LUT) engine for 60/120fps UI rendering.
 *
 * Standard [CubicBezierEasing] solves cubic polynomials iteratively on every frame.
 * On high-refresh 120Hz displays (budget = 8.33ms per frame), repeated polynomial root
 * solving across dozens of concurrent animations causes CPU micro-stutters and frame drops.
 *
 * This engine pre-samples curves into fixed-size float arrays (256 samples) and performs
 * ultra-fast `O(1)` index lookup with linear interpolation.
 */
@Immutable
class FastBezierLut(
    private val samples: FloatArray,
) : Easing {

    override fun transform(fraction: Float): Float {
        if (fraction <= 0f) return samples.first()
        if (fraction >= 1f) return samples.last()

        val maxIndex = samples.size - 1
        val scaled = fraction * maxIndex
        val index = scaled.toInt().coerceIn(0, maxIndex - 1)
        val remainder = scaled - index

        // Fast linear interpolation between pre-sampled entries
        val y0 = samples[index]
        val y1 = samples[index + 1]
        return y0 + remainder * (y1 - y0)
    }

    companion object {
        private const val DEFAULT_SAMPLE_COUNT = 256

        /**
         * Pre-calculates an easing curve into a dense float array of size [sampleCount].
         */
        fun precalculate(easing: Easing, sampleCount: Int = DEFAULT_SAMPLE_COUNT): FastBezierLut {
            val samples = FloatArray(sampleCount)
            val step = 1f / (sampleCount - 1)
            for (i in 0 until sampleCount) {
                val t = (i * step).coerceIn(0f, 1f)
                samples[i] = easing.transform(t)
            }
            return FastBezierLut(samples)
        }
    }
}

/**
 * Pre-computed high-performance easing curves for AndCode v2.0 Premium UI.
 *
 * All curves are pre-computed at application startup into static LUTs.
 */
object PrecalculatedBezierCurves {

    /** Standard Emphasized Decelerate: (0.05, 0.7, 0.1, 1.0) */
    val EmphasizedDecelerate: FastBezierLut = FastBezierLut.precalculate(
        CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f),
    )

    /** Standard Fast Out Slow In: (0.4, 0.0, 0.2, 1.0) */
    val FastOutSlowIn: FastBezierLut = FastBezierLut.precalculate(
        CubicBezierEasing(0.4f, 0f, 0.2f, 1f),
    )

    /** Bouncy Overshoot for badge pops and card springs: (0.34, 1.56, 0.64, 1.0) */
    val BouncyOvershoot: FastBezierLut = FastBezierLut.precalculate(
        CubicBezierEasing(0.34f, 1.56f, 0.64f, 1f),
    )

    /** Snappy Entrance for micro-interactions: (0.2, 0.0, 0.0, 1.0) */
    val SnappyEntrance: FastBezierLut = FastBezierLut.precalculate(
        CubicBezierEasing(0.2f, 0f, 0f, 1f),
    )

    /** Smooth Exit Decelerate: (0.4, 0.0, 1.0, 1.0) */
    val SmoothExit: FastBezierLut = FastBezierLut.precalculate(
        CubicBezierEasing(0.4f, 0f, 1f, 1f),
    )
}
