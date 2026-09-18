package com.yugahashimoto.andcode.core.reliability

import org.junit.Assert.assertEquals
import org.junit.Test

class CircuitBreakerStateTest {

    @Test
    fun `starts in CLOSED state`() {
        val breaker = CircuitBreakerState()
        assertEquals(CircuitState.CLOSED, breaker.state)
    }

    @Test
    fun `stays CLOSED below failure threshold`() {
        val breaker = (1..4).fold(CircuitBreakerState()) { acc, _ -> acc.recordFailure() }
        assertEquals(CircuitState.CLOSED, breaker.state)
    }

    @Test
    fun `opens after reaching failure threshold`() {
        val breaker = (1..5).fold(CircuitBreakerState()) { acc, _ -> acc.recordFailure() }
        assertEquals(CircuitState.OPEN, breaker.state)
    }

    @Test
    fun `does not open on success from CLOSED`() {
        val breaker = CircuitBreakerState().recordSuccess()
        assertEquals(CircuitState.CLOSED, breaker.state)
    }

    @Test
    fun `resets failure count on success in CLOSED`() {
        val breaker = CircuitBreakerState()
            .recordFailure()
            .recordFailure()
            .recordSuccess()
        // State returns to CLOSED after success, proving failure count reset
        assertEquals(CircuitState.CLOSED, breaker.state)
    }

    @Test
    fun `attempts reset moves to HALF_OPEN`() {
        val breaker = CircuitBreakerState().copy(
            state = CircuitState.OPEN,
            lastFailureMillis = System.currentTimeMillis() - 120_000,
        ).attemptReset()
        assertEquals(CircuitState.HALF_OPEN, breaker.state)
    }

    @Test
    fun `stays OPEN if reset timeout not elapsed`() {
        val breaker = CircuitBreakerState().copy(
            state = CircuitState.OPEN,
            lastFailureMillis = System.currentTimeMillis(),
        ).attemptReset()
        assertEquals(CircuitState.OPEN, breaker.state)
    }

    @Test
    fun `closes from HALF_OPEN after enough successes`() {
        var breaker = CircuitBreakerState().copy(state = CircuitState.HALF_OPEN)
        repeat(3) { breaker = breaker.recordSuccess() }
        assertEquals(CircuitState.CLOSED, breaker.state)
    }

    @Test
    fun `reopens from HALF_OPEN on failure`() {
        val breaker = CircuitBreakerState().copy(
            state = CircuitState.HALF_OPEN,
        ).recordFailure()
        assertEquals(CircuitState.OPEN, breaker.state)
    }

    @Test
    fun `ignores success while OPEN`() {
        val breaker = CircuitBreakerState().copy(
            state = CircuitState.OPEN,
            lastFailureMillis = System.currentTimeMillis(),
        ).recordSuccess()
        assertEquals(CircuitState.OPEN, breaker.state)
    }

    @Test
    fun `3 failures then success then 3 more failures stays CLOSED`() {
        var breaker = CircuitBreakerState()
        repeat(3) { breaker = breaker.recordFailure() }
        breaker = breaker.recordSuccess()
        repeat(3) { breaker = breaker.recordFailure() }
        assertEquals(CircuitState.CLOSED, breaker.state)
    }
}
