package com.yugahashimoto.andcode.core.mission

import com.yugahashimoto.andcode.core.reliability.RecoveryManager
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
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

class MissionEngine(
    private val executors: Map<MissionStep, MissionStepExecutor> = emptyMap(),
    private val recoveryManager: RecoveryManager? = null,
    private val onStepStarted: ((MissionStep) -> Unit)? = null,
    private val onStepCompleted: ((MissionStep, MissionStepResult) -> Unit)? = null,
    private val onMissionCompleted: ((String, MissionStatus) -> Unit)? = null,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
) {
    private val mutex = Mutex()
    private val mutableProgress = MutableStateFlow(MissionProgress(missionId = ""))
    val progress: StateFlow<MissionProgress> = mutableProgress.asStateFlow()

    private var executionJob: Job? = null
    private val executionHistory = mutableMapOf<String, MutableList<StepExecutionRecord>>()

    suspend fun execute(
        missionId: String,
        workspace: String,
        agentId: String? = null,
        config: MissionConfig = MissionConfig(),
        steps: List<MissionStep> = MissionStep.ORDERED,
        startFrom: MissionStep? = null,
    ): MissionProgress = mutex.withLock {
        val startingPoint = startFrom ?: steps.first()
        mutableProgress.value = MissionProgress(
            missionId = missionId,
            status = MissionStatus.RUNNING,
            currentStep = startingPoint,
            startedAtMillis = System.currentTimeMillis(),
        )

        recoveryManager?.markRunning(missionId, steps.size)
        executionHistory[missionId] = mutableListOf()

        executionJob = scope.launch {
            try {
                runMissionLoop(missionId, workspace, agentId, config, steps)
            } catch (e: CancellationException) {
                updateStatus(MissionStatus.CANCELLED)
                throw e
            } catch (e: Throwable) {
                updateStatus(MissionStatus.FAILED, error = e.message)
            }
        }
        executionJob?.join()
        return mutableProgress.value
    }

    private suspend fun runMissionLoop(
        missionId: String,
        workspace: String,
        agentId: String?,
        config: MissionConfig,
        steps: List<MissionStep>,
    ) {
        val context = MissionContext(
            missionId = missionId,
            workspace = workspace,
            agentId = agentId,
            config = config,
        )

        for (step in steps) {
            val current = mutableProgress.value
            if (current.status != MissionStatus.RUNNING) break

            mutableProgress.value = current.copy(currentStep = step)
            onStepStarted?.invoke(step)

            val result = executeStepWithRetry(step, context, config)

            val now = System.currentTimeMillis()
            val record = StepExecutionRecord(
                step = step,
                result = result,
                startedAtMillis = now - 1000,
                finishedAtMillis = now,
            )
            executionHistory[missionId]?.add(record)

            onStepCompleted?.invoke(step, result)

            val updatedResults = mutableProgress.value.stepResults + (step to result)
            val updatedCompleted = mutableProgress.value.completedSteps + step
            mutableProgress.value = mutableProgress.value.copy(
                stepResults = updatedResults,
                completedSteps = updatedCompleted,
                elapsedMillis = now - (mutableProgress.value.startedAtMillis ?: now),
            )

            when {
                result is MissionStepResult.Failed && !result.recoverable -> {
                    updateStatus(MissionStatus.FAILED, error = result.reason)
                    recoveryManager?.markFailed(missionId, result.reason)
                    onMissionCompleted?.invoke(missionId, MissionStatus.FAILED)
                    return
                }
                result is MissionStepResult.Failed && config.retryOnFailure -> {
                    recoveryManager?.markFailed(missionId, result.reason)
                }
                result is MissionStepResult.Timeout -> {
                    updateStatus(MissionStatus.FAILED, error = "Step ${step.displayName} timed out after ${result.afterMillis}ms")
                    recoveryManager?.markFailed(missionId, "Timeout: ${step.displayName}")
                    onMissionCompleted?.invoke(missionId, MissionStatus.FAILED)
                    return
                }
            }

            if (step == MissionStep.CHECKPOINT && config.pauseOnCheckpoint) {
                mutableProgress.value = mutableProgress.value.copy(status = MissionStatus.PAUSED)
                return
            }
        }

        updateStatus(MissionStatus.COMPLETED)
        recoveryManager?.markComplete(missionId)
        onMissionCompleted?.invoke(missionId, MissionStatus.COMPLETED)
    }

    private suspend fun executeStepWithRetry(
        step: MissionStep,
        context: MissionContext,
        config: MissionConfig,
    ): MissionStepResult {
        val executor = executors[step] ?: return MissionStepResult.Skipped
        var lastResult: MissionStepResult = MissionStepResult.Skipped

        for (attempt in 0..config.maxRetriesPerStep) {
            val timeout = config.timeoutFor(step)
            val result = withTimeoutOrNull(timeout) {
                withContext(Dispatchers.IO) {
                    executor.execute(context.copy(previousResults = mutableProgress.value.stepResults))
                }
            } ?: MissionStepResult.Timeout(timeout)

            lastResult = result
            if (result is MissionStepResult.Success || result is MissionStepResult.Skipped) {
                return result
            }
            if (result is MissionStepResult.Failed && !result.recoverable) {
                return result
            }
            if (attempt < config.maxRetriesPerStep) {
                delay(computeRetryDelay(attempt))
            }
        }
        return lastResult
    }

    suspend fun pause(): MissionProgress = mutex.withLock {
        val current = mutableProgress.value
        if (current.status == MissionStatus.RUNNING) {
            mutableProgress.value = current.copy(status = MissionStatus.PAUSED)
            executionJob?.cancel()
        }
        return mutableProgress.value
    }

    suspend fun resume(): MissionProgress = mutex.withLock {
        val current = mutableProgress.value
        if (current.status == MissionStatus.PAUSED) {
            mutableProgress.value = current.copy(status = MissionStatus.RUNNING)
        }
        return mutableProgress.value
    }

    suspend fun cancel(): MissionProgress = mutex.withLock {
        executionJob?.cancel()
        mutableProgress.value = mutableProgress.value.copy(status = MissionStatus.CANCELLED)
        return mutableProgress.value
    }

    fun getHistory(missionId: String): List<StepExecutionRecord> =
        executionHistory[missionId]?.toList().orEmpty()

    fun destroy() {
        executionJob?.cancel()
        scope.cancel()
    }

    private fun updateStatus(status: MissionStatus, error: String? = null) {
        val current = mutableProgress.value
        mutableProgress.value = current.copy(
            status = status,
            error = error,
            elapsedMillis = System.currentTimeMillis() - (current.startedAtMillis ?: System.currentTimeMillis()),
        )
    }

    private fun computeRetryDelay(attempt: Int): Long =
        (1000L * Math.pow(2.0, attempt.toDouble())).toLong().coerceAtMost(30_000L)
}
