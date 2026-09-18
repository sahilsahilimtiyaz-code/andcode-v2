package com.yugahashimoto.andcode.core.reliability

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

class ProcessSupervisor(
    private val config: ProcessConfig = ProcessConfig(),
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
    private val startProcess: suspend () -> Long,
    private val stopProcess: suspend (pid: Long) -> Unit,
    private val checkHealth: suspend (pid: Long) -> HealthCheckResult,
    private val getMemoryUsage: suspend (pid: Long) -> Long = { 0L },
    private val onStateChanged: ((ProcessState, ProcessState) -> Unit)? = null,
    private val onExit: ((ExitReason) -> Unit)? = null,
) {
    private val mutex = Mutex()
    private val mutableInfo = MutableStateFlow(ProcessInfo())
    val info: StateFlow<ProcessInfo> = mutableInfo.asStateFlow()

    private var healthCheckJob: Job? = null
    private var timeoutJob: Job? = null

    val currentState: ProcessState get() = mutableInfo.value.state
    val isRunning: Boolean get() = currentState == ProcessState.RUNNING

    suspend fun start(): ProcessInfo = mutex.withLock {
        val current = mutableInfo.value
        if (current.state == ProcessState.RUNNING || current.state == ProcessState.STARTING) {
            return current
        }

        transitionTo(ProcessState.STARTING)
        try {
            val pid = withContext(Dispatchers.IO) { startProcess() }
            mutableInfo.value = mutableInfo.value.copy(
                pid = pid,
                startedAtMillis = System.currentTimeMillis(),
                exitReason = null,
            )
            transitionTo(ProcessState.RUNNING)
            startHealthChecks(pid)
            startTimeoutWatchdog(pid)
        } catch (e: CancellationException) {
            transitionTo(ProcessState.FAILED)
            throw e
        } catch (e: Throwable) {
            mutableInfo.value = mutableInfo.value.copy(
                exitReason = ExitReason.Unknown(e.message),
            )
            transitionTo(ProcessState.FAILED)
        }
        return mutableInfo.value
    }

    suspend fun stop(): ProcessInfo = mutex.withLock {
        val current = mutableInfo.value
        if (current.state == ProcessState.STOPPED || current.state == ProcessState.STOPPING) {
            return current
        }

        cancelBackgroundJobs()
        transitionTo(ProcessState.STOPPING)

        val pid = current.pid
        if (pid != null) {
            try {
                withContext(Dispatchers.IO) { stopProcess(pid) }
            } catch (_: Throwable) { }
        }

        mutableInfo.value = mutableInfo.value.copy(
            exitReason = ExitReason.Normal(0),
        )
        transitionTo(ProcessState.STOPPED)
        return mutableInfo.value
    }

    suspend fun restart(): ProcessInfo {
        mutex.lock()
        try {
            val current = mutableInfo.value
            if (current.restartCount >= config.maxRestartAttempts) {
                mutableInfo.value = mutableInfo.value.copy(
                    exitReason = ExitReason.Unknown("Max restart attempts (${config.maxRestartAttempts}) exceeded"),
                )
                transitionTo(ProcessState.FAILED)
                return mutableInfo.value
            }

            cancelBackgroundJobs()
            transitionTo(ProcessState.RESTARTING)
            val backoff = computeNextRestartDelay(current.restartCount, config.restartBackoffMillis)
            delay(backoff)
            mutableInfo.value = mutableInfo.value.copy(
                restartCount = current.restartCount + 1,
                pid = null,
                startedAtMillis = null,
            )
        } finally {
            mutex.unlock()
        }
        return start()
    }

    suspend fun checkHealthNow(): HealthCheckResult {
        val pid = mutableInfo.value.pid ?: return HealthCheckResult(healthy = false, error = "No PID")
        val result = try {
            withTimeoutOrNull(config.healthCheckTimeoutMillis) { checkHealth(pid) }
                ?: HealthCheckResult(healthy = false, error = "Health check timed out")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            HealthCheckResult(healthy = false, error = e.message)
        }
        mutableInfo.value = mutableInfo.value.copy(lastHealthCheck = result)
        return result
    }

    fun destroy() {
        healthCheckJob?.cancel()
        timeoutJob?.cancel()
        scope.cancel()
    }

    private fun transitionTo(newState: ProcessState) {
        val oldState = mutableInfo.value.state
        if (!oldState.canTransitionTo(newState)) return
        mutableInfo.value = mutableInfo.value.copy(state = newState)
        onStateChanged?.invoke(oldState, newState)
    }

    private fun startHealthChecks(pid: Long) {
        healthCheckJob?.cancel()
        healthCheckJob = scope.launch {
            while (true) {
                delay(config.healthCheckIntervalMillis)
                val result = try {
                    withTimeoutOrNull(config.healthCheckTimeoutMillis) { checkHealth(pid) }
                        ?: HealthCheckResult(healthy = false, error = "Health check timed out")
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Throwable) {
                    HealthCheckResult(healthy = false, error = e.message)
                }
                mutableInfo.value = mutableInfo.value.copy(lastHealthCheck = result)
                if (!result.healthy && currentState == ProcessState.RUNNING) {
                    mutex.withLock {
                        if (currentState == ProcessState.RUNNING) {
                            mutableInfo.value = mutableInfo.value.copy(
                                exitReason = ExitReason.Unknown("Health check failed: ${result.error}"),
                            )
                            transitionTo(ProcessState.FAILED)
                            onExit?.invoke(mutableInfo.value.exitReason ?: ExitReason.Unknown("Health check failed"))
                        }
                    }
                    break
                }
            }
        }
    }

    private fun startTimeoutWatchdog(pid: Long) {
        timeoutJob?.cancel()
        timeoutJob = scope.launch {
            delay(config.timeoutMillis)
            mutex.withLock {
                if (currentState == ProcessState.RUNNING) {
                    mutableInfo.value = mutableInfo.value.copy(
                        exitReason = ExitReason.TimedOut(config.timeoutMillis),
                    )
                    transitionTo(ProcessState.FAILED)
                    onExit?.invoke(ExitReason.TimedOut(config.timeoutMillis))
                }
            }
        }
    }

    private fun cancelBackgroundJobs() {
        healthCheckJob?.cancel()
        healthCheckJob = null
        timeoutJob?.cancel()
        timeoutJob = null
    }
}
