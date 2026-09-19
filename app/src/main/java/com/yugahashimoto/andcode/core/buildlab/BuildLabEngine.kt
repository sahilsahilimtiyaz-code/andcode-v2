package com.yugahashimoto.andcode.core.buildlab

class BuildLabEngine(
    private val commandRunner: (List<String>, Long) -> Pair<Int, String>,
) {
    private val history = mutableListOf<BuildHistoryEntry>()
    private val profiles = mutableListOf<BuildProfile>()
    private var activeBuild: BuildResult? = null

    fun getProfiles(): List<BuildProfile> = profiles.toList()

    fun addProfile(profile: BuildProfile) {
        profiles.removeAll { it.id == profile.id }
        profiles.add(profile)
    }

    fun removeProfile(profileId: String) {
        profiles.removeAll { it.id == profileId }
    }

    fun getHistory(): List<BuildHistoryEntry> = history.toList()

    fun getActiveBuild(): BuildResult? = activeBuild

    suspend fun executeBuild(
        request: BuildRequest,
        profile: BuildProfile? = null,
    ): BuildResult {
        val startTime = System.currentTimeMillis()
        val effectiveProfile = profile ?: profiles.firstOrNull { it.enabled } ?: defaultProfile()

        val tasks = request.tasks.ifEmpty {
            listOf(
                if (request.cleanBuild) "clean" else null,
                "assemble${request.variant.replaceFirstChar { it.uppercase() }}",
            ).filterNotNull()
        }

        val command = buildList {
            add("./gradlew")
            addAll(tasks)
            addAll(request.extraArgs)
            addAll(effectiveProfile.gradleArgs)
            if (effectiveProfile.jvmArgs.isNotEmpty()) {
                add("-Dorg.gradle.jvmargs=${effectiveProfile.jvmArgs.joinToString(" ")}")
            }
        }

        val timeout = effectiveProfile.timeoutMinutes * 60_000L
        val result = BuildResult(
            request = request,
            status = BuildStatus.RUNNING,
            startedAt = startTime,
            finishedAt = startTime,
        )
        activeBuild = result

        return try {
            val (exitCode, output) = commandRunner(command, timeout)
            val finishedAt = System.currentTimeMillis()
            val parsed = parseBuildOutput(output, exitCode)
            val buildResult = parsed.copy(
                request = request,
                startedAt = startTime,
                finishedAt = finishedAt,
            )
            activeBuild = null
            history.add(0, BuildHistoryEntry(buildResult, effectiveProfile))
            if (history.size > MAX_HISTORY) {
                history.removeAt(history.lastIndex)
            }
            buildResult
        } catch (e: Exception) {
            val failedResult = BuildResult(
                request = request,
                status = BuildStatus.TIMEOUT,
                startedAt = startTime,
                finishedAt = System.currentTimeMillis(),
                output = e.message ?: "Unknown error",
            )
            activeBuild = null
            history.add(0, BuildHistoryEntry(failedResult, effectiveProfile))
            failedResult
        }
    }

    fun cancelBuild(): Boolean {
        val current = activeBuild ?: return false
        if (current.status != BuildStatus.RUNNING) return false
        activeBuild = current.copy(status = BuildStatus.CANCELLED, finishedAt = System.currentTimeMillis())
        return true
    }

    fun clearHistory() {
        history.clear()
    }

    private fun parseBuildOutput(output: String, exitCode: Int): BuildResult {
        val errors = mutableListOf<BuildError>()
        val taskTimings = mutableListOf<TaskTiming>()
        var tasksTotal = 0
        var tasksFromCache = 0
        var tasksUpToDate = 0

        for (line in output.lines()) {
            val trimmed = line.trim()

            if (trimmed.contains("BUILD SUCCESSFUL")) {
                val cacheMatch = Regex("(\\d+) task(s)? cached").find(trimmed)
                if (cacheMatch != null) {
                    tasksFromCache = cacheMatch.groupValues[1].toIntOrNull() ?: 0
                }
            }

            if (trimmed.startsWith("> Task :")) {
                tasksTotal++
                val taskName = trimmed.removePrefix("> Task :").trim()
                val cached = trimmed.contains("FROM-CACHE") || trimmed.contains("up-to-date")
                val upToDate = trimmed.contains("up-to-date")
                if (upToDate) tasksUpToDate++
                taskTimings.add(TaskTiming(taskName, 0L, cached || upToDate))
            }

            val errorMatch = Regex("(?:error:|FAILURE:)(.+)", RegexOption.IGNORE_CASE).find(trimmed)
            if (errorMatch != null) {
                val message = errorMatch.groupValues[1].trim()
                errors.add(
                    BuildError(
                        message = message,
                        severity = ErrorSeverity.ERROR,
                        kind = classifyError(message),
                    )
                )
            }

            val fileErrorMatch = Regex("(.+\\.kt|.+\\.java):(\\d+)(?::(\\d+))?:\\s*(error|warning):\\s*(.+)").find(trimmed)
            if (fileErrorMatch != null) {
                val (file, line, col, sev, msg) = fileErrorMatch.destructured
                errors.add(
                    BuildError(
                        file = file,
                        line = line.toIntOrNull(),
                        column = col.toIntOrNull(),
                        message = msg,
                        severity = if (sev == "error") ErrorSeverity.ERROR else ErrorSeverity.WARNING,
                        kind = classifyError(msg),
                    )
                )
            }
        }

        val status = when {
            output.contains("BUILD SUCCESSFUL") -> BuildStatus.SUCCESS
            exitCode != 0 -> BuildStatus.FAILED
            else -> BuildStatus.FAILED
        }

        val totalTasks = maxOf(tasksTotal, 1)
        val cacheHitRate = (tasksFromCache + tasksUpToDate).toDouble() / totalTasks

        return BuildResult(
            request = BuildRequest(""),
            status = status,
            startedAt = 0L,
            finishedAt = 0L,
            output = output,
            errors = errors,
            taskTimings = taskTimings,
            cacheStats = CacheStats(
                tasksTotal = tasksTotal,
                tasksFromCache = tasksFromCache,
                tasksUpToDate = tasksUpToDate,
                buildCacheHitRate = cacheHitRate,
            ),
        )
    }

    private fun classifyError(message: String): ErrorKind = when {
        message.contains("Unresolved reference", ignoreCase = true) -> ErrorKind.KOTLIN
        message.contains("Type mismatch", ignoreCase = true) -> ErrorKind.KOTLIN
        message.contains("Cannot find symbol", ignoreCase = true) -> ErrorKind.JAVA
        message.contains("AAPT2", ignoreCase = true) -> ErrorKind.RESOURCE
        message.contains("Duplicate class", ignoreCase = true) -> ErrorKind.DEPENDENCY
        message.contains("Could not resolve", ignoreCase = true) -> ErrorKind.DEPENDENCY
        message.contains("Could not download", ignoreCase = true) -> ErrorKind.NETWORK
        message.contains("KSP", ignoreCase = true) -> ErrorKind.KSP
        message.contains("lint", ignoreCase = true) -> ErrorKind.LINT
        message.contains("detekt", ignoreCase = true) -> ErrorKind.LINT
        message.contains("spotless", ignoreCase = true) -> ErrorKind.FORMAT
        message.contains("test", ignoreCase = true) && message.contains("fail", ignoreCase = true) -> ErrorKind.TEST
        else -> ErrorKind.UNKNOWN
    }

    private fun defaultProfile() = BuildProfile(
        id = "default",
        name = "Default",
        variant = "debug",
    )

    companion object {
        const val MAX_HISTORY = 50
    }
}
