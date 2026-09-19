package com.yugahashimoto.andcode.core.terminal

class TerminalAutomationEngine(
    private val commandRunner: suspend (String, String?, Int) -> Pair<Int, String>,
) {
    private val scripts = mutableMapOf<String, AutomationScript>()
    private val templates = mutableListOf<CommandTemplate>()
    private val sessions = mutableMapOf<String, TerminalSession>()

    init {
        templates.addAll(defaultTemplates())
    }

    fun getScripts(): List<AutomationScript> = scripts.values.toList()

    fun saveScript(script: AutomationScript) {
        scripts[script.id] = script
    }

    fun deleteScript(scriptId: String) {
        scripts.remove(scriptId)
    }

    fun getTemplates(): List<CommandTemplate> = templates.toList()

    fun getTemplateById(id: String): CommandTemplate? = templates.find { it.id == id }

    fun getSessions(): List<TerminalSession> = sessions.values.toList()

    fun getOrCreateSession(id: String, name: String, workingDir: String): TerminalSession {
        return sessions.getOrPut(id) {
            TerminalSession(id = id, name = name, workingDirectory = workingDir)
        }
    }

    suspend fun runScript(
        script: AutomationScript,
        workingDir: String,
        variables: Map<String, String> = emptyMap(),
        onStepComplete: ((Int, StepResult) -> Unit)? = null,
    ): ScriptRunResult {
        val startTime = System.currentTimeMillis()
        val allVars = script.variables + variables
        val stepResults = mutableListOf<StepResult>()
        val capturedOutputs = mutableMapOf<String, String>()
        var finalStatus = ScriptRunStatus.SUCCESS

        for ((index, step) in script.steps.withIndex()) {
            val resolvedCommand = resolveVariables(step.command, allVars)
            val resolvedDir = step.workingDir?.let { resolveVariables(it, allVars) } ?: workingDir

            val lastExitCode = stepResults.lastOrNull()?.exitCode
            if (step.condition != null && !evaluateCondition(step.condition!!, allVars, capturedOutputs, lastExitCode)) {
                val skipped = StepResult(
                    step = step,
                    exitCode = 0,
                    output = "Skipped: condition not met",
                    durationMillis = 0,
                    skipped = true,
                )
                stepResults.add(skipped)
                continue
            }

            val stepStart = System.currentTimeMillis()
            try {
                val (exitCode, output) = commandRunner(resolvedCommand, resolvedDir, step.timeoutSeconds * 1000)
                val duration = System.currentTimeMillis() - stepStart
                val result = StepResult(
                    step = step,
                    exitCode = exitCode,
                    output = output,
                    durationMillis = duration,
                )
                stepResults.add(result)

                step.captureOutput?.let { varName ->
                    capturedOutputs[varName] = output
                }

                onStepComplete?.invoke(index, result)

                if (exitCode != 0 && !step.continueOnError) {
                    finalStatus = ScriptRunStatus.FAILED
                    break
                }
            } catch (e: Exception) {
                val result = StepResult(
                    step = step,
                    exitCode = -1,
                    output = "",
                    durationMillis = System.currentTimeMillis() - stepStart,
                    error = e.message,
                )
                stepResults.add(result)
                onStepComplete?.invoke(index, result)

                if (!step.continueOnError) {
                    finalStatus = ScriptRunStatus.FAILED
                    break
                }
            }
        }

        val runResult = ScriptRunResult(
            scriptId = script.id,
            startedAt = startTime,
            finishedAt = System.currentTimeMillis(),
            status = finalStatus,
            stepResults = stepResults,
            capturedOutputs = capturedOutputs,
        )

        scripts[script.id]?.let { existing ->
            scripts[script.id] = existing.copy(
                lastRunAt = startTime,
                runCount = existing.runCount + 1,
            )
        }

        return runResult
    }

    private fun resolveVariables(template: String, variables: Map<String, String>): String {
        var result = template
        for ((key, value) in variables) {
            result = result.replace("\${$key}", value)
        }
        return result
    }

    private fun evaluateCondition(
        condition: StepCondition,
        variables: Map<String, String>,
        capturedOutputs: Map<String, String>,
        lastExitCode: Int?,
    ): Boolean = when (condition) {
        is StepCondition.ExitCode -> lastExitCode == condition.expected
        is StepCondition.OutputContains -> {
            capturedOutputs.values.any { it.contains(condition.text) }
        }
        is StepCondition.OutputNotContains -> {
            capturedOutputs.values.none { it.contains(condition.text) }
        }
        is StepCondition.FileExists -> {
            val resolved = resolveVariables(condition.path, variables)
            java.io.File(resolved).exists()
        }
        is StepCondition.VariableEquals -> {
            variables[condition.name] == condition.value
        }
    }

    companion object {
        private fun defaultTemplates() = listOf(
            CommandTemplate(
                id = "git_status",
                name = "Git Status",
                command = "git status",
                description = "Show working tree status",
                category = TemplateCategory.GIT,
            ),
            CommandTemplate(
                id = "git_diff",
                name = "Git Diff",
                command = "git diff",
                description = "Show unstaged changes",
                category = TemplateCategory.GIT,
            ),
            CommandTemplate(
                id = "git_log",
                name = "Git Log",
                command = "git log --oneline -10",
                description = "Show last 10 commits",
                category = TemplateCategory.GIT,
            ),
            CommandTemplate(
                id = "gradle_build",
                name = "Gradle Build",
                command = "./gradlew assembleDebug",
                description = "Build debug APK",
                category = TemplateCategory.BUILD,
            ),
            CommandTemplate(
                id = "gradle_clean",
                name = "Gradle Clean",
                command = "./gradlew clean",
                description = "Clean build outputs",
                category = TemplateCategory.BUILD,
            ),
            CommandTemplate(
                id = "gradle_test",
                name = "Gradle Test",
                command = "./gradlew testDebugUnitTest",
                description = "Run unit tests",
                category = TemplateCategory.TESTING,
            ),
            CommandTemplate(
                id = "disk_usage",
                name = "Disk Usage",
                command = "df -h",
                description = "Show disk space usage",
                category = TemplateCategory.SYSTEM,
            ),
            CommandTemplate(
                id = "find_files",
                name = "Find Files",
                command = "find . -name \"*.kt\" -type f",
                description = "Find all Kotlin files",
                category = TemplateCategory.FILE_MANAGEMENT,
                variables = listOf(
                    TemplateVariable("pattern", "File name pattern", "*.kt"),
                ),
            ),
            CommandTemplate(
                id = "list_dir",
                name = "List Directory",
                command = "ls -la",
                description = "List directory contents with details",
                category = TemplateCategory.FILE_MANAGEMENT,
            ),
            CommandTemplate(
                id = "network_check",
                name = "Network Check",
                command = "curl -s -o /dev/null -w '%{http_code}' https://google.com",
                description = "Check network connectivity",
                category = TemplateCategory.NETWORK,
            ),
        )
    }
}
