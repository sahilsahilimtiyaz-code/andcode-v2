package com.yugahashimoto.andcode.core.verify

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TestResultParserTest {

    @Test
    fun `parseXml extracts test cases`() {
        val xml = """<testsuite tests="2" failures="0" errors="0">
            <testcase name="testAdd" classname="MathTest" time="0.01"/>
            <testcase name="testSub" classname="MathTest" time="0.005"/>
        </testsuite>"""
        val results = TestResultParser.parseXml(xml)
        assertEquals(2, results.size)
        assertTrue(results.all { it.passed })
    }

    @Test
    fun `parseXml detects failures`() {
        val xml = """<testsuite tests="1" failures="1" errors="0">
            <testcase name="testFail" classname="MyTest" time="0.1">
                <failure message="expected 1 but was 2">stack trace here</failure>
            </testcase>
        </testsuite>"""
        val results = TestResultParser.parseXml(xml)
        assertEquals(1, results.size)
        assertFalse(results[0].passed)
        assertEquals("expected 1 but was 2", results[0].errorMessage)
    }

    @Test
    fun `parseSummary extracts counts`() {
        val xml = """<testsuite tests="10" failures="2" errors="0"/>"""
        val (tests, failures) = TestResultParser.parseSummary(xml)
        assertEquals(10, tests)
        assertEquals(2, failures)
    }

    @Test
    fun `parseTextOutput handles go-style output`() {
        val output = "PASS math.TestAdd\nFAIL math.TestDivide (divide by zero)"
        val results = TestResultParser.parseTextOutput(output)
        assertEquals(2, results.size)
        assertTrue(results[0].passed)
        assertFalse(results[1].passed)
    }

    @Test
    fun `parseXml handles empty suite`() {
        val results = TestResultParser.parseXml("<testsuite tests=\"0\" failures=\"0\" errors=\"0\"/>")
        assertEquals(0, results.size)
    }

    @Test
    fun `stackTrace is truncated`() {
        val longTrace = "x".repeat(1000)
        val xml = """<testsuite tests="1" failures="1" errors="0">
            <testcase name="t" classname="C" time="0">
                <failure message="err">$longTrace</failure>
            </testcase>
        </testsuite>"""
        val results = TestResultParser.parseXml(xml)
        assertNotNull(results[0].stackTrace)
        assertTrue(results[0].stackTrace!!.length <= 500)
    }
}
