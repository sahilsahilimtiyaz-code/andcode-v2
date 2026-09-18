package com.yugahashimoto.andcode.core.mission

import com.yugahashimoto.andcode.runtime.local.LocalRuntimeCommandRunner
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

data class Checkpoint(
    val id: String = UUID.randomUUID().toString(),
    val missionId: String,
    val step: MissionStep,
    val gitCommitHash: String? = null,
    val backedUpFiles: List<String> = emptyList(),
    val timestampMillis: Long = System.currentTimeMillis(),
)

sealed class RollbackResult {
    data class Success(val checkpointId: String) : RollbackResult()
    data class Failed(val reason: String) : RollbackResult()
}

class CheckpointManager(
    private val runtimeDirectory: File,
    private val commandRunner: LocalRuntimeCommandRunner,
    private val checkpointsDir: File = File(runtimeDirectory, "checkpoints"),
) {
    private val checkpointStore = mutableMapOf<String, MutableList<Checkpoint>>()

    suspend fun createCheckpoint(
        missionId: String,
        step: MissionStep,
        workspaceDir: File,
        filesToBackup: List<String> = emptyList(),
    ): Checkpoint = withContext(Dispatchers.IO) {
        val backupDir = File(checkpointsDir, "$missionId/${UUID.randomUUID()}").apply { mkdirs() }
        val backedUp = mutableListOf<String>()

        for (relativePath in filesToBackup) {
            val source = File(workspaceDir, relativePath)
            if (source.isFile) {
                val dest = File(backupDir, relativePath)
                dest.parentFile?.mkdirs()
                source.copyTo(dest, overwrite = true)
                backedUp.add(relativePath)
            }
        }

        val commitHash = runCatching {
            val result = commandRunner.runShell(
                commandText = "cd ${workspaceDir.absolutePath} && git rev-parse HEAD 2>/dev/null || echo ''",
                timeoutSeconds = 10L,
            )
            result.output.trim().takeIf { it.isNotBlank() && it.length == 40 }
        }.getOrNull()

        val checkpoint = Checkpoint(
            missionId = missionId,
            step = step,
            gitCommitHash = commitHash,
            backedUpFiles = backedUp,
        )

        checkpointStore.getOrPut(missionId) { mutableListOf() }.add(checkpoint)
        checkpoint
    }

    suspend fun rollback(
        checkpoint: Checkpoint,
        workspaceDir: File,
    ): RollbackResult = withContext(Dispatchers.IO) {
        try {
            val backupDir = findBackupDir(checkpoint)
                ?: return@withContext RollbackResult.Failed("Backup directory not found for checkpoint ${checkpoint.id}")

            for (relativePath in checkpoint.backedUpFiles) {
                val backup = File(backupDir, relativePath)
                val target = File(workspaceDir, relativePath)
                if (backup.isFile) {
                    target.parentFile?.mkdirs()
                    backup.copyTo(target, overwrite = true)
                }
            }

            if (checkpoint.gitCommitHash != null) {
                commandRunner.runShell(
                    commandText = "cd ${workspaceDir.absolutePath} && git reset --hard ${checkpoint.gitCommitHash} 2>/dev/null || true",
                    timeoutSeconds = 30L,
                )
            }

            RollbackResult.Success(checkpoint.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            RollbackResult.Failed("Rollback failed: ${e.message}")
        }
    }

    fun listCheckpoints(missionId: String): List<Checkpoint> =
        checkpointStore[missionId]?.toList().orEmpty()

    fun getCheckpoint(missionId: String, checkpointId: String): Checkpoint? =
        checkpointStore[missionId]?.firstOrNull { it.id == checkpointId }

    private fun findBackupDir(checkpoint: Checkpoint): File? {
        val missionDir = File(checkpointsDir, checkpoint.missionId)
        return missionDir.listFiles()?.firstOrNull { it.name == checkpoint.id.substringBefore('-') }
            ?: missionDir.listFiles()?.lastOrNull()
    }
}
