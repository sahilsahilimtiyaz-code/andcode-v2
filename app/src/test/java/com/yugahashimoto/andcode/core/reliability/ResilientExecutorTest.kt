package com.yugahashimoto.andcode.core.reliability

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class ResilientExecutorTest {

    @Test
    fun `successful execution returns value`() = runTest {
        val executor = ResilientExecutor(retryPolicy = RetryPolicy(maxAttempts = 3, jitter = false))
        val result = executor.execute { 42 }
        assertTrue(result.value.isSuccess)
        assertEquals(42, result.value.getOrNull())
        assertEquals(1, result.attempts)
    }

    @Test
    fun `retries on transient failure`() = runTest {
        var attempts = 0
        val executor = ResilientExecutor(
            retryPolicy = RetryPolicy(maxAttempts = 3, baseDelayMillis = 10L, jitter = false),
        )
        val result = executor.execute {
            attempts++
            if (attempts < 3) throw java.net.SocketTimeoutException("timeout")
            "recovered"
        }
        assertTrue(result.value.isSuccess)
        assertEquals("recovered", result.value.getOrNull())
        assertEquals(3, result.attempts)
    }

    @Test
    fun `fails after max attempts exhausted`() = runTest {
        val executor = ResilientExecutor(
            retryPolicy = RetryPolicy(maxAttempts = 3, baseDelayMillis = 10L, jitter = false),
        )
        val result = executor.execute {
            throw java.net.SocketTimeoutException("always fail")
        }
        assertTrue(result.value.isFailure)
        assertEquals(3, result.attempts)
    }

    @Test
    fun `does not retry on permanent error`() = runTest {
        var attempts = 0
        val executor = ResilientExecutor(
            retryPolicy = RetryPolicy(maxAttempts = 3, baseDelayMillis = 10L, jitter = false),
        )
        val result = executor.execute {
            attempts++
            throw IllegalArgumentException("permanent")
        }
        assertTrue(result.value.isFailure)
        assertEquals(1, attempts)
    }

    @Test
    fun `circuit breaker opens and blocks execution`() = runTest {
        val executor = ResilientExecutor(
            retryPolicy = RetryPolicy(maxAttempts = 1, baseDelayMillis = 10L, jitter = false),
            circuitBreakerConfig = CircuitBreakerConfig(failureThreshold = 2),
        )
        repeat(2) {
            executor.execute<Unit> { throw java.net.SocketTimeoutException("fail") }
        }
        assertEquals(CircuitState.OPEN, executor.circuitBreakerState)
        val result = executor.execute { "should not run" }
        assertTrue(result.value.isFailure)
        assertTrue(result.value.exceptionOrNull() is CircuitBreakerOpenException)
    }

    @Test
    fun `circuit breaker reset allows execution again`() = runTest {
        val executor = ResilientExecutor(
            retryPolicy = RetryPolicy(maxAttempts = 1, baseDelayMillis = 10L, jitter = false),
            circuitBreakerConfig = CircuitBreakerConfig(failureThreshold = 1),
        )
        executor.execute<Unit> { throw java.net.SocketTimeoutException("fail") }
        assertEquals(CircuitState.OPEN, executor.circuitBreakerState)
        executor.resetCircuitBreaker()
        assertEquals(CircuitState.CLOSED, executor.circuitBreakerState)
        val result = executor.execute { "works" }
        assertTrue(result.value.isSuccess)
    }

    @Test
    fun `onError callback is invoked on failure`() = runTest {
        var capturedError: Throwable? = null
        var capturedAttempt = 0
        val executor = ResilientExecutor(
            retryPolicy = RetryPolicy(maxAttempts = 2, baseDelayMillis = 10L, jitter = false),
            onError = { error, attempt ->
                capturedError = error
                capturedAttempt = attempt
            },
        )
        executor.execute<Unit> { throw java.net.SocketTimeoutException("fail") }
        assertTrue(capturedError is java.net.SocketTimeoutException)
        assertEquals(2, capturedAttempt)
    }

    @Test
    fun `cancellation exception propagates immediately`() = runTest {
        val executor = ResilientExecutor(
            retryPolicy = RetryPolicy(maxAttempts = 3, baseDelayMillis = 10L, jitter = false),
        )
        try {
            executor.execute {
                throw CancellationException("cancelled")
            }
            fail("Should have thrown CancellationException")
        } catch (e: CancellationException) {
            assertEquals("cancelled", e.message)
        }
    }

    @Test
    fun `tracks total delay across retries`() = runTest {
        var attempts = 0
        val executor = ResilientExecutor(
            retryPolicy = RetryPolicy(maxAttempts = 3, baseDelayMillis = 50L, jitter = false),
        )
        val result = executor.execute {
            attempts++
            if (attempts < 3) throw java.net.SocketTimeoutException("fail")
            "ok"
        }
        assertTrue(result.totalDelayMillis > 0)
    }
}
