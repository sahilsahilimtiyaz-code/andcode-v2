package com.yugahashimoto.andcode.core.mission

enum class AgentRole(val displayName: String, val description: String) {
    PLANNER("Planner", "Breaks down tasks into executable steps"),
    CODER("Coder", "Writes and modifies code"),
    DEBUGGER("Debugger", "Identifies and fixes bugs"),
    REVIEWER("Reviewer", "Reviews code quality and patterns"),
    TESTER("Tester", "Writes and runs tests"),
    BUILD_ENGINEER("Build Engineer", "Manages build and compilation"),
}

enum class TaskStatus { PENDING, RUNNING, COMPLETED, FAILED, BLOCKED }

data class AgentTask(
    val id: String,
    val role: AgentRole,
    val prompt: String,
    val dependsOn: List<String> = emptyList(),
    val timeoutMillis: Long = 300_000L,
    val status: TaskStatus = TaskStatus.PENDING,
    val modelId: String? = null,
    val providerId: String? = null,
)

data class AgentAssignment(
    val task: AgentTask,
    val agentId: String,
    val result: MissionStepResult? = null,
    val startedAtMillis: Long? = null,
    val finishedAtMillis: Long? = null,
    val error: String? = null,
) {
    val durationMillis: Long
        get() {
            val start = startedAtMillis ?: return 0L
            val end = finishedAtMillis ?: System.currentTimeMillis()
            return end - start
        }
}

data class OrchestrationConfig(
    val maxConcurrency: Int = 3,
    val defaultTimeoutMillis: Long = 300_000L,
    val retryFailedTasks: Boolean = true,
    val failFast: Boolean = false,
)
