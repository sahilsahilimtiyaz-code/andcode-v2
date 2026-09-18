package com.yugahashimoto.andcode.core.reliability

import com.yugahashimoto.andcode.data.local.MissionStateDao
import com.yugahashimoto.andcode.data.local.MissionStateEntity
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

sealed class RecoveryResult {
    data class Recovered(val missionId: String, val restoredStep: Int) : RecoveryResult()
    data class Failed(val missionId: String, val reason: String) : RecoveryResult()
    data class NotNeeded(val missionId: String) : RecoveryResult()
}

class RecoveryManager(
    private val missionStateDao: MissionStateDao,
    private val processSupervisor: ProcessSupervisor,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
) {
    private val mutableRecoveryState = MutableStateFlow<Map<String, RecoveryResult>>(emptyMap())
    val recoveryState: StateFlow<Map<String, RecoveryResult>> = mutableRecoveryState.asStateFlow()

    suspend fun attemptRecovery(missionId: String): RecoveryResult {
        return try {
            val result = withContext(Dispatchers.IO) { doRecovery(missionId) }
            mutableRecoveryState.value = mutableRecoveryState.value + (missionId to result)
            result
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            val result = RecoveryResult.Failed(missionId, "Recovery exception: ${e.message}")
            mutableRecoveryState.value = mutableRecoveryState.value + (missionId to result)
            result
        }
    }

    private suspend fun doRecovery(missionId: String): RecoveryResult {
        val state = missionStateDao.getById(missionId)
            ?: return RecoveryResult.Failed(missionId, "No persisted state found")

        return when (state.status) {
            "COMPLETED" -> RecoveryResult.NotNeeded(missionId)
            "CANCELLED" -> RecoveryResult.NotNeeded(missionId)
            "FAILED" -> {
                val checkpoint = state.checkpoint
                if (checkpoint != null) {
                    rollbackToCheckpoint(state)
                } else {
                    RecoveryResult.Failed(missionId, "Failed with no checkpoint to recover from")
                }
            }
            "RUNNING", "PENDING" -> {
                val checkpoint = state.checkpoint
                if (checkpoint != null) {
                    rollbackToCheckpoint(state)
                } else {
                    val now = System.currentTimeMillis()
                    missionStateDao.upsert(state.copy(
                        status = "PENDING",
                        currentStep = 0,
                        updatedAt = now,
                    ))
                    RecoveryResult.Recovered(missionId, 0)
                }
            }
            else -> RecoveryResult.Failed(missionId, "Unknown status: ${state.status}")
        }
    }

    private suspend fun rollbackToCheckpoint(state: MissionStateEntity): RecoveryResult {
        val checkpoint = state.checkpoint ?: return RecoveryResult.Failed(
            state.missionId, "Checkpoint data is null",
        )
        val step = checkpoint.toIntOrNull() ?: 0
        val now = System.currentTimeMillis()
        val updated = state.copy(
            status = "PENDING",
            currentStep = step,
            checkpoint = null,
            updatedAt = now,
        )
        missionStateDao.upsert(updated)
        return RecoveryResult.Recovered(state.missionId, step)
    }

    suspend fun persistCheckpoint(missionId: String, step: Int) {
        val now = System.currentTimeMillis()
        val existing = missionStateDao.getById(missionId)
        if (existing != null) {
            missionStateDao.upsert(existing.copy(
                checkpoint = step.toString(),
                updatedAt = now,
            ))
        } else {
            missionStateDao.upsert(MissionStateEntity(
                missionId = missionId,
                currentStep = step,
                checkpoint = step.toString(),
                createdAt = now,
                updatedAt = now,
            ))
        }
    }

    suspend fun markRunning(missionId: String, totalSteps: Int = 12) {
        val now = System.currentTimeMillis()
        val existing = missionStateDao.getById(missionId)
        if (existing != null) {
            missionStateDao.upsert(existing.copy(
                status = "RUNNING",
                totalSteps = totalSteps,
                updatedAt = now,
            ))
        } else {
            missionStateDao.upsert(MissionStateEntity(
                missionId = missionId,
                totalSteps = totalSteps,
                status = "RUNNING",
                createdAt = now,
                updatedAt = now,
            ))
        }
    }

    suspend fun markComplete(missionId: String) {
        val now = System.currentTimeMillis()
        val existing = missionStateDao.getById(missionId) ?: return
        missionStateDao.upsert(existing.copy(
            status = "COMPLETED",
            currentStep = existing.totalSteps,
            updatedAt = now,
        ))
    }

    suspend fun markFailed(missionId: String, error: String) {
        val now = System.currentTimeMillis()
        val existing = missionStateDao.getById(missionId) ?: return
        val escaped = error.replace("\\", "\\\\").replace("\"", "\\\"")
        val raw = existing.errorHistory.trim()
        val errors = if (raw.isEmpty() || raw == "[]") {
            """["$escaped"]"""
        } else {
            raw.dropLast(1) + ",\"$escaped\"]"
        }
        missionStateDao.upsert(existing.copy(
            status = "FAILED",
            errorHistory = errors,
            updatedAt = now,
        ))
    }

    suspend fun cancel(missionId: String) {
        val now = System.currentTimeMillis()
        val existing = missionStateDao.getById(missionId) ?: return
        if (existing.status == "COMPLETED") return
        missionStateDao.upsert(existing.copy(
            status = "CANCELLED",
            updatedAt = now,
        ))
    }

    suspend fun getMissionState(missionId: String): MissionStateEntity? =
        missionStateDao.getById(missionId)
}
