package com.yugahashimoto.andcode.core.terminal

data class AutomationScript(
    val id: String,
    val name: String,
    val description: String = "",
    val steps: List<ScriptStep>,
    val variables: Map<String, String> = emptyMap(),
    val createdAt: Long = System.currentTimeMillis(),
    val lastRunAt: Long? = null,
    val runCount: Int = 0,
    val tags: List<String> = emptyList(),
)

data class ScriptStep(
    val id: String,
    val command: String,
    val workingDir: String? = null,
    val timeoutSeconds: Int = 30,
    val continueOnError: Boolean = false,
    val condition: StepCondition? = null,
    val captureOutput: String? = null,
)

sealed class StepCondition {
    data class ExitCode(val expected: Int) : StepCondition()
    data class OutputContains(val text: String) : StepCondition()
    data class OutputNotContains(val text: String) : StepCondition()
    data class FileExists(val path: String) : StepCondition()
    data class VariableEquals(val name: String, val value: String) : StepCondition()
}

data class ScriptRunResult(
    val scriptId: String,
    val startedAt: Long,
    val finishedAt: Long,
    val status: ScriptRunStatus,
    val stepResults: List<StepResult>,
    val capturedOutputs: Map<String, String> = emptyMap(),
) {
    val durationMillis: Long get() = finishedAt - startedAt
}

enum class ScriptRunStatus { SUCCESS, FAILED, CANCELLED, SKIPPED }

data class StepResult(
    val step: ScriptStep,
    val exitCode: Int,
    val output: String,
    val durationMillis: Long,
    val skipped: Boolean = false,
    val error: String? = null,
)

data class CommandTemplate(
    val id: String,
    val name: String,
    val command: String,
    val description: String = "",
    val category: TemplateCategory = TemplateCategory.GENERAL,
    val variables: List<TemplateVariable> = emptyList(),
)

enum class TemplateCategory {
    GENERAL,
    BUILD,
    GIT,
    FILE_MANAGEMENT,
    NETWORK,
    SYSTEM,
    TESTING,
}

data class TemplateVariable(
    val name: String,
    val description: String,
    val defaultValue: String? = null,
    val required: Boolean = true,
)

data class TerminalSession(
    val id: String,
    val name: String,
    val workingDirectory: String,
    val history: List<TerminalEntry> = emptyList(),
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
)

data class TerminalEntry(
    val command: String,
    val output: String,
    val exitCode: Int,
    val timestampMillis: Long = System.currentTimeMillis(),
    val durationMillis: Long = 0L,
    val workingDir: String = "",
)

data class TerminalAutomationState(
    val scripts: List<AutomationScript> = emptyList(),
    val templates: List<CommandTemplate> = emptyList(),
    val sessions: List<TerminalSession> = emptyList(),
    val activeSession: TerminalSession? = null,
    val runningScript: ScriptRunResult? = null,
)
