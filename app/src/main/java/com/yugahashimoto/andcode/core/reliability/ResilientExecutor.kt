package com.yugahashimoto.andcode.core.reliability

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay

class ResilientExecutor(
    private val retryPolicy: RetryPolicy = RetryPolicy(),
    private val circuitBreakerConfig: CircuitBreakerConfig = CircuitBreakerConfig(),
    private val onError: ((Throwable, Int) -> Unit)? = null,
) {
    private var circuitState = CircuitBreakerState()

    val circuitBreakerState: CircuitState get() = circuitState.state

    fun resetCircuitBreaker() {
        circuitState = CircuitBreakerState()
    }

    suspend fun <T> execute(
        block: suspend () -> T,
    ): ExecutionResult<T> {
        var lastError: Throwable? = null
        var totalDelay = 0L

        for (attempt in 1..retryPolicy.maxAttempts) {
            circuitState = circuitState.checkCooldown(cooldownMillis = circuitBreakerConfig.cooldownMillis)

            if (circuitState.isOpen) {
                return ExecutionResult(
                    value = Result.failure(CircuitBreakerOpenException(circuitState)),
                    attempts = attempt - 1,
                    totalDelayMillis = totalDelay,
                )
            }

            try {
                val result = block()
                circuitState = circuitState.recordSuccess()
                return ExecutionResult(
                    value = Result.success(result),
                    attempts = attempt,
                    totalDelayMillis = totalDelay,
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                lastError = e
                onError?.invoke(e, attempt)
                circuitState = circuitState.recordFailure(failureThreshold = circuitBreakerConfig.failureThreshold)

                val errorClass = classifyError(e)
                when (errorClass) {
                    is ErrorClass.Permanent -> {
                        return ExecutionResult(
                            value = Result.failure(e),
                            attempts = attempt,
                            totalDelayMillis = totalDelay,
                        )
                    }
                    is ErrorClass.RateLimited -> {
                        val retryAfter = parseRetryAfter(e)
                        if (retryAfter != null) {
                            delay(retryAfter)
                            totalDelay += retryAfter
                            continue
                        }
                    }
                    is ErrorClass.Transient -> { }
                }

                if (attempt < retryPolicy.maxAttempts) {
                    val delayMs = retryPolicy.delayForAttempt(attempt)
                    delay(delayMs)
                    totalDelay += delayMs
                }
            }
        }

        return ExecutionResult(
            value = Result.failure(lastError ?: IllegalStateException("Execution failed after ${retryPolicy.maxAttempts} attempts")),
            attempts = retryPolicy.maxAttempts,
            totalDelayMillis = totalDelay,
        )
    }

    private fun parseRetryAfter(throwable: Throwable): Long? {
        val message = throwable.message ?: return null
        val match = Regex("retry[_-]?after[:\\s=]*(\\d+)", RegexOption.IGNORE_CASE)
            .find(message) ?: return null
        val seconds = match.groupValues[1].toLongOrNull() ?: return null
        return (seconds * 1000L).coerceAtMost(retryPolicy.maxDelayMillis)
    }
}

class CircuitBreakerOpenException(
    val breakerState: CircuitBreakerState,
) : IllegalStateException("Circuit breaker is OPEN since ${breakerState.lastFailureTimeMillis}")
