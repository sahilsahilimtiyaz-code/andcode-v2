package com.yugahashimoto.andcode.core.reliability

import kotlin.math.min

enum class ProcessState {
    CREATED,
    STARTING,
    RUNNING,
    STOPPING,
    STOPPED,
    FAILED,
    RESTARTING;

    fun canTransitionTo(next: ProcessState): Boolean = when (this) {
        CREATED -> next == STARTING || next == STOPPED
        STARTING -> next == RUNNING || next == FAILED || next == STOPPING
        RUNNING -> next == STOPPING || next == FAILED || next == RESTARTING
        STOPPING -> next == STOPPED || next == FAILED
        STOPPED -> next == STARTING || next == RESTARTING
        FAILED -> next == STARTING || next == RESTARTING
        RESTARTING -> next == STARTING || next == STOPPED || next == FAILED
    }
}

data class ProcessConfig(
    val timeoutMillis: Long = 300_000L,
    val maxMemoryBytes: Long = 512L * 1024 * 1024,
    val maxRestartAttempts: Int = 3,
    val restartBackoffMillis: Long = 5_000L,
    val healthCheckIntervalMillis: Long = 30_000L,
    val healthCheckTimeoutMillis: Long = 10_000L,
)

sealed class ExitReason {
    data class Normal(val exitCode: Int) : ExitReason()
    data class Crashed(val exitCode: Int, val signal: Int? = null) : ExitReason()
    data class TimedOut(val afterMillis: Long) : ExitReason()
    data class ResourceLimit(val resource: String, val limit: Long, val actual: Long) : ExitReason()
    data class Killed(val signal: Int) : ExitReason()
    data class Unknown(val message: String? = null) : ExitReason()
}

data class ProcessInfo(
    val pid: Long? = null,
    val state: ProcessState = ProcessState.CREATED,
    val startedAtMillis: Long? = null,
    val exitReason: ExitReason? = null,
    val restartCount: Int = 0,
    val lastHealthCheck: HealthCheckResult? = null,
    val resourceUsage: ResourceUsage = ResourceUsage(),
)

data class ResourceUsage(
    val memoryBytes: Long = 0L,
    val cpuPercent: Double = 0.0,
    val openFileDescriptors: Int = 0,
    val uptimeMillis: Long = 0L,
)

fun validateTransition(from: ProcessState, to: ProcessState): Boolean = from.canTransitionTo(to)

fun computeNextRestartDelay(
    restartCount: Int,
    baseDelayMillis: Long,
    maxDelayMillis: Long = 120_000L,
): Long {
    val exponential = baseDelayMillis * Math.pow(2.0, restartCount.toDouble())
    return min(exponential.toLong(), maxDelayMillis)
}
