package com.yugahashimoto.andcode.core.reliability

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay as coroutineDelay
import kotlin.math.min
import kotlin.random.Random

data class RetryPolicy(
    val maxAttempts: Int = 3,
    val baseDelayMillis: Long = 1_000L,
    val maxDelayMillis: Long = 30_000L,
    val backoffMultiplier: Double = 2.0,
    val jitter: Boolean = true,
) {
    fun delayForAttempt(attempt: Int): Long {
        val exponential = baseDelayMillis * Math.pow(backoffMultiplier, (attempt - 1).toDouble())
        val capped = min(exponential, maxDelayMillis.toDouble())
        return if (jitter) {
            (capped * (0.5 + Random.nextDouble() * 0.5)).toLong()
        } else {
            capped.toLong()
        }
    }

    suspend fun <T> execute(action: suspend () -> T): T {
        var lastException: Throwable? = null
        repeat(maxAttempts) { attempt ->
            try {
                return action()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                lastException = e
                if (attempt < maxAttempts - 1) {
                    coroutineDelay(delayForAttempt(attempt + 1))
                }
            }
        }
        throw lastException ?: IllegalStateException("Retry failed with no exception")
    }
}
