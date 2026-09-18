package com.yugahashimoto.andcode.core.buildlab

data class BuildRequest(
    val projectId: String,
    val variant: String = "debug",
    val tasks: List<String> = emptyList(),
    val cleanBuild: Boolean = false,
    val extraArgs: List<String> = emptyList(),
    val timestampMillis: Long = System.currentTimeMillis(),
)

data class BuildResult(
    val request: BuildRequest,
    val status: BuildStatus,
    val startedAt: Long,
    val finishedAt: Long,
    val output: String = "",
    val errors: List<BuildError> = emptyList(),
    val taskTimings: List<TaskTiming> = emptyList(),
    val cacheStats: CacheStats = CacheStats(),
    val artifactPaths: List<String> = emptyList(),
) {
    val durationMillis: Long get() = finishedAt - startedAt
}

enum class BuildStatus {
    QUEUED,
    RUNNING,
    SUCCESS,
    FAILED,
    CANCELLED,
    TIMEOUT,
}

data class BuildError(
    val file: String? = null,
    val line: Int? = null,
    val column: Int? = null,
    val message: String,
    val severity: ErrorSeverity,
    val kind: ErrorKind,
    val suggestion: String? = null,
)

enum class ErrorSeverity { ERROR, WARNING, INFO }
enum class ErrorKind {
    COMPILATION,
    DEPENDENCY,
    RESOURCE,
    KOTLIN,
    JAVA,
    KSP,
    LINT,
    FORMAT,
    TEST,
    NETWORK,
    UNKNOWN,
}

data class TaskTiming(
    val name: String,
    val durationMillis: Long,
    val cacheHit: Boolean = false,
)

data class CacheStats(
    val tasksTotal: Int = 0,
    val tasksFromCache: Int = 0,
    val tasksUpToDate: Int = 0,
    val buildCacheHitRate: Double = 0.0,
)

data class BuildProfile(
    val id: String,
    val name: String,
    val variant: String,
    val jvmArgs: List<String> = emptyList(),
    val gradleArgs: List<String> = emptyList(),
    val envVars: Map<String, String> = emptyMap(),
    val timeoutMinutes: Int = 30,
    val enabled: Boolean = true,
)

data class BuildHistoryEntry(
    val result: BuildResult,
    val profileUsed: BuildProfile?,
)

data class BuildLabState(
    val activeBuild: BuildResult? = null,
    val queue: List<BuildRequest> = emptyList(),
    val history: List<BuildHistoryEntry> = emptyList(),
    val profiles: List<BuildProfile> = emptyList(),
    val isMonitoring: Boolean = false,
) {
    val isBuildRunning: Boolean get() = activeBuild?.status == BuildStatus.RUNNING
}
