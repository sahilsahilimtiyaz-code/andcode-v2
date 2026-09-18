package com.yugahashimoto.andcode.core.verify

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VerifyPipelineTest {

    @Test
    fun `pipeline runs all steps`() = runTest {
        val stepsRun = mutableListOf<VerifyStep>()
        val pipeline = VerifyPipeline(
            stepExecutor = { step, dir ->
                stepsRun.add(step)
                "BUILD SUCCESSFUL"
            },
        )
        val report = pipeline.run("/tmp/test")
        assertEquals(VerifyStep.entries.size, stepsRun.size)
        assertTrue(report.isSuccessful)
    }

    @Test
    fun `pipeline stops on build failure`() = runTest {
        val stepsRun = mutableListOf<VerifyStep>()
        val pipeline = VerifyPipeline(
            stepExecutor = { step, _ ->
                stepsRun.add(step)
                if (step == VerifyStep.BUILD) "BUILD FAILED" else "ok"
            },
        )
        pipeline.run("/tmp/test")
        assertTrue(stepsRun.contains(VerifyStep.BUILD))
        assertFalse(stepsRun.contains(VerifyStep.REVIEW))
    }

    @Test
    fun `pipeline handles step exception`() = runTest {
        val pipeline = VerifyPipeline(
            stepExecutor = { _, _ -> throw RuntimeException("shell failed") },
        )
        val report = pipeline.run("/tmp/test")
        assertFalse(report.isSuccessful)
        assertTrue(report.results.any { it.error?.contains("shell failed") == true })
    }

    @Test
    fun `pipeline produces correct report`() = runTest {
        val pipeline = VerifyPipeline(
            stepExecutor = { step, _ ->
                when (step) {
                    VerifyStep.LINT -> "/src/A.kt:1:1: warning - Unused [rule]"
                    VerifyStep.FORMAT -> "Everything up-to-date"
                    VerifyStep.TEST -> """<testsuite tests="3" failures="1" errors="0"><testcase name="t1" classname="C" time="0.01"/><testcase name="t2" classname="C" time="0.02"/><testcase name="t3" classname="C" time="0.03"><failure message="fail">trace</failure></testcase></testsuite>"""
                    VerifyStep.BUILD -> "BUILD SUCCESSFUL"
                }
            },
        )
        val report = pipeline.run("/tmp/test")
        assertEquals(4, report.results.size)
        assertEquals(1, report.totalViolations)
        assertEquals(1, report.totalTestFailures)
        assertEquals(3, report.totalTests)
    }

    @Test
    fun `pipeline can skip steps`() = runTest {
        val pipeline = VerifyPipeline(
            stepExecutor = { _, _ -> "ok" },
            steps = listOf(VerifyStep.TEST, VerifyStep.BUILD),
        )
        val report = pipeline.run("/tmp/test")
        assertEquals(2, report.results.size)
    }
}
