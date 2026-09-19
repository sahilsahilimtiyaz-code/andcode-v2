package com.yugahashimoto.andcode.core.reliability

import kotlin.math.min
import kotlin.random.Random

enum class CircuitState { CLOSED, OPEN, HALF_OPEN }

data class CircuitBreakerConfig(
    val failureThreshold: Int = 5,
    val cooldownMillis: Long = 60_000L,
    val halfOpenMaxAttempts: Int = 1,
)

data class CircuitBreakerState(
    val state: CircuitState = CircuitState.CLOSED,
    val failureCount: Int = 0,
    val successCount: Int = 0,
    val lastFailureTimeMillis: Long = 0L,
    private val failureThreshold: Int = 5,
    private val resetTimeoutMillis: Long = 60_000L,
    private val successThreshold: Int = 3,
) {
    val isOpen: Boolean get() = state == CircuitState.OPEN

    fun recordSuccess(): CircuitBreakerState = when (state) {
        CircuitState.HALF_OPEN -> if (successCount + 1 >= successThreshold) {
            copy(state = CircuitState.CLOSED, failureCount = 0, successCount = 0)
        } else {
            copy(successCount = successCount + 1)
        }
        CircuitState.OPEN -> this
        CircuitState.CLOSED -> copy(successCount = successCount + 1)
    }

    fun recordFailure(nowMillis: Long = System.currentTimeMillis()): CircuitBreakerState = when (state) {
        CircuitState.HALF_OPEN -> copy(
            state = CircuitState.OPEN,
            failureCount = failureCount + 1,
            lastFailureTimeMillis = nowMillis,
        )
        CircuitState.CLOSED -> {
            val newCount = failureCount + 1
            if (newCount >= failureThreshold) {
                copy(state = CircuitState.OPEN, failureCount = newCount, lastFailureTimeMillis = nowMillis)
            } else {
                copy(failureCount = newCount)
            }
        }
        CircuitState.OPEN -> copy(lastFailureTimeMillis = nowMillis)
    }

    fun checkCooldown(nowMillis: Long = System.currentTimeMillis()): CircuitBreakerState =
        if (state == CircuitState.OPEN && nowMillis - lastFailureTimeMillis >= resetTimeoutMillis) {
            copy(state = CircuitState.HALF_OPEN, successCount = 0)
        } else {
            this
        }

    fun attemptReset(): CircuitBreakerState = checkCooldown()
}

sealed class ErrorClass {
    data object Transient : ErrorClass()
    data object Permanent : ErrorClass()
    data object RateLimited : ErrorClass()
}

fun classifyError(throwable: Throwable): ErrorClass = when {
    throwable is java.net.SocketTimeoutException -> ErrorClass.Transient
    throwable is java.net.ConnectException -> ErrorClass.Transient
    throwable is java.io.InterruptedIOException -> ErrorClass.Transient
    throwable is SecurityException -> ErrorClass.Permanent
    throwable is IllegalArgumentException -> ErrorClass.Permanent
    throwable.message?.contains("429", ignoreCase = true) == true -> ErrorClass.RateLimited
    throwable.message?.contains("rate limit", ignoreCase = true) == true -> ErrorClass.RateLimited
    throwable.message?.contains("quota", ignoreCase = true) == true -> ErrorClass.RateLimited
    else -> ErrorClass.Transient
}

data class ExecutionResult<T>(
    val value: Result<T>,
    val attempts: Int,
    val totalDelayMillis: Long,
)

sealed class RecoveryAction {
    data object Retry : RecoveryAction()
    data object CircuitBreak : RecoveryAction()
    data class Fallback<T>(val value: T) : RecoveryAction()
    data object Propagate : RecoveryAction()
}

data class HealthCheckResult(
    val healthy: Boolean,
    val latencyMillis: Long = 0L,
    val error: String? = null,
    val checkedAtMillis: Long = System.currentTimeMillis(),
)

data class ResourceBudget(
    val maxMemoryBytes: Long = 512L * 1024 * 1024,
    val maxCpuPercent: Double = 80.0,
    val maxOpenFileDescriptors: Int = 256,
    val timeoutMillis: Long = 300_000L,
)
