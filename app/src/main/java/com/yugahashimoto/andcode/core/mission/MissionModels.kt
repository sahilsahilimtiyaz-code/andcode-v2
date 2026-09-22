package com.yugahashimoto.andcode.core.mission

enum class MissionStep(val index: Int, val displayName: String) {
    UNDERSTAND(0, "Understand"),
    PLAN(1, "Plan"),
    CHECKPOINT(2, "Checkpoint"),
    IMPLEMENT(3, "Implement"),
    FORMAT(4, "Format"),
    ANALYZE(5, "Analyze"),
    TEST(6, "Test"),
    BUILD(7, "Build"),
    INSTALL(8, "Install"),
    VERIFY(9, "Verify"),
    REVIEW(10, "Review"),
    COMPLETE(11, "Complete");

    companion object {
        val ORDERED = entries.toList()
        fun fromIndex(index: Int): MissionStep? = ORDERED.getOrNull(index)
    }
}

enum class MissionStatus { PENDING, RUNNING, PAUSED, FAILED, COMPLETED, CANCELLED }

sealed class MissionStepResult {
    data object Success : MissionStepResult()
    data object Skipped : MissionStepResult()
    data class Failed(val reason: String, val recoverable: Boolean = true) : MissionStepResult()
    data class Timeout(val afterMillis: Long) : MissionStepResult()
}

data class MissionConfig(
    val stepTimeoutMillis: Map<MissionStep, Long> = emptyMap(),
    val maxTotalDurationMillis: Long = 3_600_000L,
    val retryOnFailure: Boolean = true,
    val maxRetriesPerStep: Int = 2,
    val pauseOnCheckpoint: Boolean = false,
) {
    fun timeoutFor(step: MissionStep): Long =
        stepTimeoutMillis[step] ?: DEFAULT_STEP_TIMEOUT

    companion object {
        const val DEFAULT_STEP_TIMEOUT = 300_000L
    }
}

data class MissionProgress(
    val missionId: String,
    val status: MissionStatus = MissionStatus.PENDING,
    val currentStep: MissionStep = MissionStep.UNDERSTAND,
    val completedSteps: Set<MissionStep> = emptySet(),
    val stepResults: Map<MissionStep, MissionStepResult> = emptyMap(),
    val startedAtMillis: Long? = null,
    val elapsedMillis: Long = 0L,
    val totalSteps: Int = MissionStep.ORDERED.size,
    val error: String? = null,
) {
    val stepIndex: Int get() = currentStep.index
    val progressPercent: Int
        get() = ((completedSteps.size.toDouble() / totalSteps) * 100).toInt()
    val isTerminal: Boolean
        get() = status == MissionStatus.COMPLETED ||
                status == MissionStatus.FAILED ||
                status == MissionStatus.CANCELLED
}

data class MissionContext(
    val missionId: String,
    val workspace: String,
    val agentId: String? = null,
    val config: MissionConfig = MissionConfig(),
    val previousResults: Map<MissionStep, MissionStepResult> = emptyMap(),
    val metadata: Map<String, String> = emptyMap(),
)

data class StepExecutionRecord(
    val step: MissionStep,
    val result: MissionStepResult,
    val startedAtMillis: Long,
    val finishedAtMillis: Long,
    val retryCount: Int = 0,
) {
    val durationMillis: Long get() = finishedAtMillis - startedAtMillis
}
