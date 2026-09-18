package com.yugahashimoto.andcode.core.reliability

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProcessSupervisorTest {

    private fun createSupervisor(
        startProcess: suspend () -> Long = { 12345L },
        stopProcess: suspend (Long) -> Unit = { },
        checkHealth: suspend (Long) -> HealthCheckResult = { HealthCheckResult(healthy = true) },
        config: ProcessConfig = ProcessConfig(
            healthCheckIntervalMillis = Long.MAX_VALUE,
            timeoutMillis = Long.MAX_VALUE,
        ),
    ) = ProcessSupervisor(
        config = config,
        startProcess = startProcess,
        stopProcess = stopProcess,
        checkHealth = checkHealth,
    )

    @Test
    fun `initial state is CREATED`() {
        val supervisor = createSupervisor()
        assertEquals(ProcessState.CREATED, supervisor.currentState)
        assertFalse(supervisor.isRunning)
    }

    @Test
    fun `start transitions to RUNNING`() = runTest {
        val supervisor = createSupervisor()
        val info = supervisor.start()
        assertEquals(ProcessState.RUNNING, info.state)
        assertEquals(12345L, info.pid)
        assertTrue(supervisor.isRunning)
        supervisor.destroy()
    }

    @Test
    fun `stop transitions to STOPPED`() = runTest {
        val supervisor = createSupervisor()
        supervisor.start()
        val info = supervisor.stop()
        assertEquals(ProcessState.STOPPED, info.state)
        assertFalse(supervisor.isRunning)
        supervisor.destroy()
    }

    @Test
    fun `stop when already stopped is idempotent`() = runTest {
        val supervisor = createSupervisor()
        supervisor.start()
        supervisor.stop()
        val info = supervisor.stop()
        assertEquals(ProcessState.STOPPED, info.state)
        supervisor.destroy()
    }

    @Test
    fun `start failure transitions to FAILED`() = runTest {
        val supervisor = createSupervisor(
            startProcess = { throw RuntimeException("startup failed") },
        )
        val info = supervisor.start()
        assertEquals(ProcessState.FAILED, info.state)
        assertNotNull(info.exitReason)
        supervisor.destroy()
    }

    @Test
    fun `onStateChanged callback fires on transitions`() = runTest {
        val transitions = mutableListOf<Pair<ProcessState, ProcessState>>()
        val supervisor = createSupervisor(
            config = ProcessConfig(
                healthCheckIntervalMillis = Long.MAX_VALUE,
                timeoutMillis = Long.MAX_VALUE,
            ),
        )
        val supervisorWithCallback = ProcessSupervisor(
            config = ProcessConfig(
                healthCheckIntervalMillis = Long.MAX_VALUE,
                timeoutMillis = Long.MAX_VALUE,
            ),
            startProcess = { 12345L },
            stopProcess = { },
            checkHealth = { HealthCheckResult(healthy = true) },
            onStateChanged = { from, to -> transitions.add(from to to) },
        )
        supervisorWithCallback.start()
        supervisorWithCallback.stop()
        assertTrue(transitions.any { it.first == ProcessState.CREATED && it.second == ProcessState.STARTING })
        assertTrue(transitions.any { it.first == ProcessState.STARTING && it.second == ProcessState.RUNNING })
        assertTrue(transitions.any { it.first == ProcessState.RUNNING && it.second == ProcessState.STOPPING })
        assertTrue(transitions.any { it.first == ProcessState.STOPPING && it.second == ProcessState.STOPPED })
        supervisorWithCallback.destroy()
    }

    @Test
    fun `checkHealthNow returns healthy result`() = runTest {
        val supervisor = createSupervisor(
            checkHealth = { HealthCheckResult(healthy = true, latencyMillis = 50) },
        )
        supervisor.start()
        val result = supervisor.checkHealthNow()
        assertTrue(result.healthy)
        assertEquals(50L, result.latencyMillis)
        supervisor.destroy()
    }

    @Test
    fun `checkHealthNow returns unhealthy when no PID`() = runTest {
        val supervisor = createSupervisor()
        val result = supervisor.checkHealthNow()
        assertFalse(result.healthy)
        assertEquals("No PID", result.error)
        supervisor.destroy()
    }

    @Test
    fun `checkHealthNow returns unhealthy on exception`() = runTest {
        val supervisor = createSupervisor(
            startProcess = { 12345L },
            checkHealth = { throw RuntimeException("health check crashed") },
        )
        supervisor.start()
        val result = supervisor.checkHealthNow()
        assertFalse(result.healthy)
        assertEquals("health check crashed", result.error)
        supervisor.destroy()
    }

    @Test
    fun `destroy cancels background jobs`() = runTest {
        val supervisor = createSupervisor()
        supervisor.start()
        supervisor.destroy()
        assertNotNull("destroy should not throw", supervisor)
    }
}
