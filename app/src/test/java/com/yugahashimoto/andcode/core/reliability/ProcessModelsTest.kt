package com.yugahashimoto.andcode.core.reliability

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProcessModelsTest {

    @Test
    fun `CREATED can transition to STARTING`() {
        assertTrue(ProcessState.CREATED.canTransitionTo(ProcessState.STARTING))
    }

    @Test
    fun `CREATED can transition to STOPPED`() {
        assertTrue(ProcessState.CREATED.canTransitionTo(ProcessState.STOPPED))
    }

    @Test
    fun `CREATED cannot transition to RUNNING`() {
        assertFalse(ProcessState.CREATED.canTransitionTo(ProcessState.RUNNING))
    }

    @Test
    fun `STARTING can transition to RUNNING`() {
        assertTrue(ProcessState.STARTING.canTransitionTo(ProcessState.RUNNING))
    }

    @Test
    fun `STARTING can transition to FAILED`() {
        assertTrue(ProcessState.STARTING.canTransitionTo(ProcessState.FAILED))
    }

    @Test
    fun `RUNNING can transition to STOPPING`() {
        assertTrue(ProcessState.RUNNING.canTransitionTo(ProcessState.STOPPING))
    }

    @Test
    fun `RUNNING can transition to FAILED`() {
        assertTrue(ProcessState.RUNNING.canTransitionTo(ProcessState.FAILED))
    }

    @Test
    fun `RUNNING can transition to RESTARTING`() {
        assertTrue(ProcessState.RUNNING.canTransitionTo(ProcessState.RESTARTING))
    }

    @Test
    fun `STOPPING can transition to STOPPED`() {
        assertTrue(ProcessState.STOPPING.canTransitionTo(ProcessState.STOPPED))
    }

    @Test
    fun `STOPPED can transition to STARTING`() {
        assertTrue(ProcessState.STOPPED.canTransitionTo(ProcessState.STARTING))
    }

    @Test
    fun `STOPPED can transition to RESTARTING`() {
        assertTrue(ProcessState.STOPPED.canTransitionTo(ProcessState.RESTARTING))
    }

    @Test
    fun `FAILED can transition to STARTING`() {
        assertTrue(ProcessState.FAILED.canTransitionTo(ProcessState.STARTING))
    }

    @Test
    fun `FAILED can transition to RESTARTING`() {
        assertTrue(ProcessState.FAILED.canTransitionTo(ProcessState.RESTARTING))
    }

    @Test
    fun `RESTARTING can transition to STARTING`() {
        assertTrue(ProcessState.RESTARTING.canTransitionTo(ProcessState.STARTING))
    }

    @Test
    fun `RESTARTING can transition to FAILED`() {
        assertTrue(ProcessState.RESTARTING.canTransitionTo(ProcessState.FAILED))
    }

    @Test
    fun `validateTransition returns true for valid transitions`() {
        assertTrue(validateTransition(ProcessState.CREATED, ProcessState.STARTING))
    }

    @Test
    fun `validateTransition returns false for invalid transitions`() {
        assertFalse(validateTransition(ProcessState.CREATED, ProcessState.RUNNING))
    }

    @Test
    fun `computeNextRestartDelay doubles exponentially`() {
        val delay0 = computeNextRestartDelay(0, 1000L)
        val delay1 = computeNextRestartDelay(1, 1000L)
        val delay2 = computeNextRestartDelay(2, 1000L)
        assertEquals(1000L, delay0)
        assertEquals(2000L, delay1)
        assertEquals(4000L, delay2)
    }

    @Test
    fun `computeNextRestartDelay caps at max`() {
        val delay = computeNextRestartDelay(20, 1000L, maxDelayMillis = 10_000L)
        assertEquals(10_000L, delay)
    }

    @Test
    fun `ProcessConfig default values`() {
        val config = ProcessConfig()
        assertEquals(300_000L, config.timeoutMillis)
        assertEquals(3, config.maxRestartAttempts)
        assertEquals(5_000L, config.restartBackoffMillis)
    }

    @Test
    fun `ProcessInfo default state is CREATED`() {
        val info = ProcessInfo()
        assertEquals(ProcessState.CREATED, info.state)
        assertEquals(null, info.pid)
        assertEquals(0, info.restartCount)
    }

    @Test
    fun `ExitReason Normal carries exit code`() {
        val reason = ExitReason.Normal(0)
        assertTrue(reason is ExitReason.Normal)
        assertEquals(0, reason.exitCode)
    }

    @Test
    fun `ExitReason TimedOut carries duration`() {
        val reason = ExitReason.TimedOut(60_000L)
        assertTrue(reason is ExitReason.TimedOut)
        assertEquals(60_000L, reason.afterMillis)
    }
}
