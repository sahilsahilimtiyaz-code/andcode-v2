package com.yugahashimoto.andcode.core.reliability

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReliabilityModelsTest {

    @Test
    fun `retry delay first attempt is base delay`() {
        val policy = RetryPolicy(baseDelayMillis = 1000L, jitter = false)
        assertEquals(1000L, policy.delayForAttempt(1))
    }

    @Test
    fun `retry delay doubles each attempt with default multiplier`() {
        val policy = RetryPolicy(baseDelayMillis = 1000L, jitter = false)
        assertEquals(1000L, policy.delayForAttempt(1))
        assertEquals(2000L, policy.delayForAttempt(2))
        assertEquals(4000L, policy.delayForAttempt(3))
    }

    @Test
    fun `retry delay caps at max delay`() {
        val policy = RetryPolicy(baseDelayMillis = 1000L, maxDelayMillis = 5000L, jitter = false)
        assertEquals(5000L, policy.delayForAttempt(10))
    }

    @Test
    fun `retry delay with zero attempt returns zero`() {
        val policy = RetryPolicy(baseDelayMillis = 1000L)
        assertEquals(0L, policy.delayForAttempt(0))
    }

    @Test
    fun `retry delay with jitter varies between calls`() {
        val policy = RetryPolicy(baseDelayMillis = 1000L, maxDelayMillis = 1000L, jitter = true)
        val delays = (1..20).map { policy.delayForAttempt(1) }
        assertTrue("With jitter, delays should vary", delays.distinct().size > 1)
    }

    @Test
    fun `circuit breaker starts closed`() {
        val breaker = CircuitBreakerState()
        assertEquals(CircuitState.CLOSED, breaker.state)
    }

    @Test
    fun `circuit breaker opens after threshold failures`() {
        var breaker = CircuitBreakerState()
        repeat(4) { breaker = breaker.recordFailure() }
        assertEquals(CircuitState.CLOSED, breaker.state)
        breaker = breaker.recordFailure()
        assertEquals(CircuitState.OPEN, breaker.state)
    }

    @Test
    fun `circuit breaker transitions to half-open after cooldown`() {
        var breaker = CircuitBreakerState(
            state = CircuitState.OPEN,
            lastFailureMillis = System.currentTimeMillis() - 120_000L,
        )
        breaker = breaker.attemptReset()
        assertEquals(CircuitState.HALF_OPEN, breaker.state)
    }

    @Test
    fun `circuit breaker closes on success from half-open`() {
        var breaker = CircuitBreakerState(
            state = CircuitState.HALF_OPEN,
        )
        breaker = breaker.recordSuccess()
        assertEquals(CircuitState.CLOSED, breaker.state)
    }

    @Test
    fun `circuit breaker reopens on failure from half-open`() {
        var breaker = CircuitBreakerState(
            state = CircuitState.HALF_OPEN,
            failureCount = 5,
        )
        breaker = breaker.recordFailure()
        assertEquals(CircuitState.OPEN, breaker.state)
    }

    @Test
    fun `circuit breaker success in closed state stays closed`() {
        var breaker = CircuitBreakerState(state = CircuitState.CLOSED, failureCount = 2)
        breaker = breaker.recordSuccess()
        assertEquals(CircuitState.CLOSED, breaker.state)
    }

    @Test
    fun `classifyError returns Transient for socket timeout`() {
        val error = java.net.SocketTimeoutException("timeout")
        assertEquals(ErrorClass.Transient, classifyError(error))
    }

    @Test
    fun `classifyError returns Transient for connect exception`() {
        val error = java.net.ConnectException("connection refused")
        assertEquals(ErrorClass.Transient, classifyError(error))
    }

    @Test
    fun `classifyError returns Permanent for illegal argument`() {
        val error = IllegalArgumentException("bad input")
        assertEquals(ErrorClass.Permanent, classifyError(error))
    }

    @Test
    fun `classifyError returns RateLimited for 429 in message`() {
        val error = RuntimeException("HTTP 429 Too Many Requests")
        assertEquals(ErrorClass.RateLimited, classifyError(error))
    }

    @Test
    fun `classifyError returns RateLimited for rate limit text`() {
        val error = RuntimeException("rate limit exceeded")
        assertEquals(ErrorClass.RateLimited, classifyError(error))
    }

    @Test
    fun `classifyError returns RateLimited for quota text`() {
        val error = RuntimeException("quota exceeded")
        assertEquals(ErrorClass.RateLimited, classifyError(error))
    }

    @Test
    fun `classifyError returns Transient for unknown error`() {
        val error = RuntimeException("something happened")
        assertEquals(ErrorClass.Transient, classifyError(error))
    }

    @Test
    fun `health check result healthy defaults`() {
        val result = HealthCheckResult(healthy = true)
        assertTrue(result.healthy)
        assertEquals(0L, result.latencyMillis)
        assertEquals(null, result.error)
    }

    @Test
    fun `resource budget default values`() {
        val budget = ResourceBudget()
        assertEquals(512L * 1024 * 1024, budget.maxMemoryBytes)
        assertEquals(80.0, budget.maxCpuPercent, 0.01)
        assertEquals(256, budget.maxOpenFileDescriptors)
        assertEquals(300_000L, budget.timeoutMillis)
    }
}
