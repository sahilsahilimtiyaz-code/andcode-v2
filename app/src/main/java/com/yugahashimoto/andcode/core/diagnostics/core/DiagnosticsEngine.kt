package com.yugahashimoto.andcode.core.diagnostics.core

class DiagnosticsEngine(
    private val commandRunner: suspend (String, String?, Int) -> Pair<Int, String>,
) {
    private val reportHistory = mutableListOf<DiagnosticReport>()

    fun getReportHistory(): List<DiagnosticReport> = reportHistory.toList()

    fun getLastReport(): DiagnosticReport? = reportHistory.lastOrNull()

    suspend fun runDiagnostics(): DiagnosticReport {
        val checks = mutableListOf<HealthCheck>()
        checks.add(checkRuntime())
        checks.add(checkDiskSpace())
        checks.add(checkNetwork())
        checks.add(checkProcessHealth())
        checks.add(checkConfiguration())
        checks.add(checkDependencies())

        val overallStatus = when {
            checks.any { it.status == HealthStatus.UNHEALTHY } -> HealthStatus.UNHEALTHY
            checks.any { it.status == HealthStatus.DEGRADED } -> HealthStatus.DEGRADED
            checks.all { it.status == HealthStatus.HEALTHY } -> HealthStatus.HEALTHY
            else -> HealthStatus.UNKNOWN
        }

        val recommendations = generateRecommendations(checks)
        val report = DiagnosticReport(
            checks = checks,
            overallStatus = overallStatus,
            summary = buildSummary(checks, overallStatus),
            recommendations = recommendations,
        )
        reportHistory.add(report)
        if (reportHistory.size > MAX_REPORTS) {
            reportHistory.removeAt(0)
        }
        return report
    }

    suspend fun getSystemInfo(): SystemInfo {
        val osName = runCmd("uname -s")
        val osVersion = runCmd("uname -r")
        val arch = runCmd("uname -m")
        val javaVersion = runCmd("java -version 2>&1 | head -1")
        val diskTotal = runCmd("df -B1 / | tail -1 | awk '{print \$2}'")
        val diskAvail = runCmd("df -B1 / | tail -1 | awk '{print \$4}'")
        val memTotal = runCmd("grep MemTotal /proc/meminfo | awk '{print \$2}'")

        return SystemInfo(
            osName = osName.trim(),
            osVersion = osVersion.trim(),
            arch = arch.trim(),
            javaVersion = javaVersion.trim(),
            totalStorageBytes = diskTotal.trim().toLongOrNull() ?: 0,
            availableStorageBytes = diskAvail.trim().toLongOrNull() ?: 0,
            totalRamBytes = (memTotal.trim().toLongOrNull() ?: 0) * 1024,
            isConnected = checkNetworkReachable(),
        )
    }

    private suspend fun checkRuntime(): HealthCheck {
        val start = System.currentTimeMillis()
        return try {
            val (exitCode, output) = commandRunner("which opencode || which claude || echo none", null, 5_000)
            val hasRuntime = exitCode == 0 && !output.trim().contains("none")
            HealthCheck(
                id = "runtime_check",
                name = "Runtime Availability",
                category = HealthCategory.RUNTIME,
                status = if (hasRuntime) HealthStatus.HEALTHY else HealthStatus.DEGRADED,
                message = if (hasRuntime) "Runtime found: ${output.trim()}" else "No runtime CLI found",
                durationMillis = System.currentTimeMillis() - start,
                suggestion = if (!hasRuntime) "Install a coding agent runtime (OpenCode, Claude Code)" else null,
            )
        } catch (e: Exception) {
            HealthCheck(
                id = "runtime_check",
                name = "Runtime Availability",
                category = HealthCategory.RUNTIME,
                status = HealthStatus.UNHEALTHY,
                message = "Check failed: ${e.message}",
                durationMillis = System.currentTimeMillis() - start,
            )
        }
    }

    private suspend fun checkDiskSpace(): HealthCheck {
        val start = System.currentTimeMillis()
        return try {
            val (exitCode, output) = commandRunner("df -h / | tail -1", null, 5_000)
            val parts = output.trim().split("\\s+".toRegex())
            val usagePercent = parts.getOrElse(4) { "0%" }.removeSuffix("%").toIntOrNull() ?: 0
            val status = when {
                usagePercent >= 98 -> HealthStatus.UNHEALTHY
                usagePercent >= 90 -> HealthStatus.DEGRADED
                else -> HealthStatus.HEALTHY
            }
            HealthCheck(
                id = "disk_check",
                name = "Disk Space",
                category = HealthCategory.STORAGE,
                status = status,
                message = "Disk usage: $usagePercent% (${parts.getOrElse(2) { "?" }} used / ${parts.getOrElse(1) { "?" }} total)",
                durationMillis = System.currentTimeMillis() - start,
                suggestion = if (status != HealthStatus.HEALTHY) "Free disk space by cleaning build outputs or unused files" else null,
            )
        } catch (e: Exception) {
            HealthCheck(
                id = "disk_check",
                name = "Disk Space",
                category = HealthCategory.STORAGE,
                status = HealthStatus.UNKNOWN,
                message = "Check failed: ${e.message}",
                durationMillis = System.currentTimeMillis() - start,
            )
        }
    }

    private suspend fun checkNetwork(): HealthCheck {
        val start = System.currentTimeMillis()
        return try {
            val reachable = checkNetworkReachable()
            HealthCheck(
                id = "network_check",
                name = "Network Connectivity",
                category = HealthCategory.NETWORK,
                status = if (reachable) HealthStatus.HEALTHY else HealthStatus.UNHEALTHY,
                message = if (reachable) "Network reachable" else "Network unreachable",
                durationMillis = System.currentTimeMillis() - start,
                suggestion = if (!reachable) "Check network connection and DNS settings" else null,
            )
        } catch (e: Exception) {
            HealthCheck(
                id = "network_check",
                name = "Network Connectivity",
                category = HealthCategory.NETWORK,
                status = HealthStatus.UNKNOWN,
                message = "Check failed: ${e.message}",
                durationMillis = System.currentTimeMillis() - start,
            )
        }
    }

    private suspend fun checkProcessHealth(): HealthCheck {
        val start = System.currentTimeMillis()
        return try {
            val (_, memInfo) = commandRunner("free -m | grep Mem", null, 5_000)
            val parts = memInfo.trim().split("\\s+".toRegex())
            val total = parts.getOrElse(1) { "0" }.toIntOrNull() ?: 0
            val available = parts.getOrElse(6) { "0" }.toIntOrNull() ?: 0
            val usedPercent = if (total > 0) ((total - available).toDouble() / total * 100).toInt() else 0
            val status = when {
                usedPercent >= 95 -> HealthStatus.UNHEALTHY
                usedPercent >= 85 -> HealthStatus.DEGRADED
                else -> HealthStatus.HEALTHY
            }
            HealthCheck(
                id = "memory_check",
                name = "Memory Usage",
                category = HealthCategory.PROCESS,
                status = status,
                message = "Memory usage: $usedPercent% (${total - available}MB / ${total}MB)",
                details = mapOf("total_mb" to "$total", "available_mb" to "$available", "used_percent" to "$usedPercent"),
                durationMillis = System.currentTimeMillis() - start,
                suggestion = if (status != HealthStatus.HEALTHY) "Close unused apps or increase available memory" else null,
            )
        } catch (e: Exception) {
            HealthCheck(
                id = "memory_check",
                name = "Memory Usage",
                category = HealthCategory.PROCESS,
                status = HealthStatus.UNKNOWN,
                message = "Check failed: ${e.message}",
                durationMillis = System.currentTimeMillis() - start,
            )
        }
    }

    private suspend fun checkConfiguration(): HealthCheck {
        val start = System.currentTimeMillis()
        return try {
            val (_, gitVersion) = commandRunner("git --version", null, 5_000)
            val hasGit = gitVersion.contains("git version")
            val (_, javaVersion) = commandRunner("java -version 2>&1 | head -1", null, 5_000)
            val hasJava = javaVersion.contains("openjdk") || javaVersion.contains("java")
            val allGood = hasGit && hasJava
            HealthCheck(
                id = "config_check",
                name = "Configuration",
                category = HealthCategory.CONFIGURATION,
                status = if (allGood) HealthStatus.HEALTHY else HealthStatus.DEGRADED,
                message = buildString {
                    append("Git: ${if (hasGit) "OK" else "MISSING"}")
                    append(", Java: ${if (hasJava) "OK" else "MISSING"}")
                },
                durationMillis = System.currentTimeMillis() - start,
                suggestion = if (!allGood) "Install missing tools: ${if (!hasGit) "git " else ""}${if (!hasJava) "java" else ""}" else null,
            )
        } catch (e: Exception) {
            HealthCheck(
                id = "config_check",
                name = "Configuration",
                category = HealthCategory.CONFIGURATION,
                status = HealthStatus.UNKNOWN,
                message = "Check failed: ${e.message}",
                durationMillis = System.currentTimeMillis() - start,
            )
        }
    }

    private suspend fun checkDependencies(): HealthCheck {
        val start = System.currentTimeMillis()
        return try {
            val (_, gradleCheck) = commandRunner("which gradle || test -f ./gradlew && echo gradlew-found || echo none", null, 5_000)
            val hasGradle = gradleCheck.trim().contains("gradlew-found") || gradleCheck.trim().contains("/gradle")
            HealthCheck(
                id = "dependency_check",
                name = "Build Dependencies",
                category = HealthCategory.DEPENDENCY,
                status = if (hasGradle) HealthStatus.HEALTHY else HealthStatus.DEGRADED,
                message = if (hasGradle) "Gradle available" else "Gradle not found in PATH",
                durationMillis = System.currentTimeMillis() - start,
                suggestion = if (!hasGradle) "Ensure Gradle or gradlew is available" else null,
            )
        } catch (e: Exception) {
            HealthCheck(
                id = "dependency_check",
                name = "Build Dependencies",
                category = HealthCategory.DEPENDENCY,
                status = HealthStatus.UNKNOWN,
                message = "Check failed: ${e.message}",
                durationMillis = System.currentTimeMillis() - start,
            )
        }
    }

    private fun generateRecommendations(checks: List<HealthCheck>): List<Recommendation> {
        val recs = mutableListOf<Recommendation>()
        var id = 0
        for (check in checks) {
            if (check.status == HealthStatus.HEALTHY) continue
            id++
            val priority = when (check.status) {
                HealthStatus.UNHEALTHY -> RecommendationPriority.HIGH
                HealthStatus.DEGRADED -> RecommendationPriority.MEDIUM
                else -> RecommendationPriority.LOW
            }
            recs.add(
                Recommendation(
                    id = "rec_$id",
                    priority = priority,
                    category = check.category,
                    title = check.name,
                    description = check.message,
                    action = check.suggestion,
                    autoFixAvailable = false,
                )
            )
        }
        return recs.sortedByDescending { it.priority.ordinal }
    }

    private fun buildSummary(checks: List<HealthCheck>, overall: HealthStatus): String {
        val healthy = checks.count { it.status == HealthStatus.HEALTHY }
        val total = checks.size
        return when (overall) {
            HealthStatus.HEALTHY -> "All $total checks passed."
            HealthStatus.DEGRADED -> "${total - healthy} of $total checks degraded."
            HealthStatus.UNHEALTHY -> "${checks.count { it.status == HealthStatus.UNHEALTHY }} critical issue(s) found."
            HealthStatus.UNKNOWN -> "Unable to determine system health."
        }
    }

    private suspend fun checkNetworkReachable(): Boolean {
        return try {
            val (exitCode, _) = commandRunner("curl -s -o /dev/null -w '%{http_code}' --max-time 5 https://google.com", null, 10_000)
            exitCode == 0
        } catch (e: Exception) {
            false
        }
    }

    private suspend fun runCmd(cmd: String, dir: String? = null, timeout: Int = 5_000): String {
        return try {
            val (_, output) = commandRunner(cmd, dir, timeout)
            output
        } catch (e: Exception) {
            ""
        }
    }

    companion object {
        const val MAX_REPORTS = 20
    }
}
