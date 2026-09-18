package com.yugahashimoto.andcode.core.context

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TokenEstimatorTest {

    @Test
    fun `empty string returns zero`() {
        assertEquals(0, TokenEstimator.estimate(""))
    }

    @Test
    fun `single word estimates correctly`() {
        val tokens = TokenEstimator.estimate("hello")
        assertTrue(tokens in 1..3)
    }

    @Test
    fun `long text has more tokens than short`() {
        val short = TokenEstimator.estimate("hello world")
        val long = TokenEstimator.estimate("hello world this is a much longer sentence with many words")
        assertTrue(long > short)
    }

    @Test
    fun `batch estimation sums correctly`() {
        val tokens = TokenEstimator.estimateBatch(listOf("hello", "world"))
        val single = TokenEstimator.estimate("hello") + TokenEstimator.estimate("world")
        assertEquals(single, tokens)
    }

    @Test
    fun `fitsWithin respects limit`() {
        assertTrue(TokenEstimator.fitsWithin("hi", 100))
        assertFalse(TokenEstimator.fitsWithin("a".repeat(10000), 10))
    }

    @Test
    fun `truncateToTokens shortens text`() {
        val long = "word ".repeat(1000)
        val truncated = TokenEstimator.truncateToTokens(long, 50)
        assertTrue(truncated.length < long.length)
        assertTrue(TokenEstimator.estimate(truncated) <= 60)
    }

    @Test
    fun `truncateToTokens preserves short text`() {
        val short = "hello"
        assertEquals(short, TokenEstimator.truncateToTokens(short, 100))
    }

    @Test
    fun `truncation prefers newline boundary`() {
        val text = "line1\nline2\nline3\nline4\nline5"
        val truncated = TokenEstimator.truncateToTokens(text, 5)
        assertFalse(truncated.endsWith("line"))
    }
}
