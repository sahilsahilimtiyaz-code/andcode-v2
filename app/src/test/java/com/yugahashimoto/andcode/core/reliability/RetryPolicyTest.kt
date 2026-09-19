package com.yugahashimoto.andcode.core.reliability

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class RetryPolicyTest {

    @Test
    fun `delay increases with attempt number`() {
        val policy = RetryPolicy(baseDelayMillis = 1000, jitter = false)
        val d1 = policy.delayForAttempt(1)
        val d2 = policy.delayForAttempt(2)
        val d3 = policy.delayForAttempt(3)
        assert(d1 < d2) { "Attempt 2 delay ($d2) should be greater than attempt 1 ($d1)" }
        assert(d2 < d3) { "Attempt 3 delay ($d3) should be greater than attempt 2 ($d2)" }
    }

    @Test
    fun `delay respects max cap`() {
        val policy = RetryPolicy(
            baseDelayMillis = 1000,
            maxDelayMillis = 5000,
            backoffMultiplier = 4.0,
            jitter = false,
        )
        val delay = policy.delayForAttempt(10)
        assert(delay <= 5000) { "Delay ($delay) exceeded max (5000)" }
    }

    @Test
    fun `jitter adds randomness`() {
        val policy = RetryPolicy(baseDelayMillis = 1000, jitter = true)
        val delays = (1..10).map { policy.delayForAttempt(2) }
        val unique = delays.toSet()
        assert(unique.size > 1) { "Jitter should produce varying delays, got: $delays" }
    }

    @Test
    fun `execute succeeds on first attempt`() = runTest {
        val policy = RetryPolicy()
        val result = policy.execute { 42 }
        assertEquals(42, result)
    }

    @Test
    fun `execute retries on failure`() = runTest {
        val policy = RetryPolicy(maxAttempts = 3, baseDelayMillis = 10, jitter = false)
        var attempts = 0
        val result = policy.execute {
            attempts++
            if (attempts < 3) throw RuntimeException("fail")
            "ok"
        }
        assertEquals("ok", result)
        assertEquals(3, attempts)
    }

    @Test
    fun `execute throws after exhausting attempts`() = runTest {
        val policy = RetryPolicy(maxAttempts = 2, baseDelayMillis = 10, jitter = false)
        var threw = false
        try {
            policy.execute { throw RuntimeException("always fail") }
        } catch (e: RuntimeException) {
            threw = true
        }
        assert(threw) { "Expected RuntimeException to be thrown" }
    }
}
