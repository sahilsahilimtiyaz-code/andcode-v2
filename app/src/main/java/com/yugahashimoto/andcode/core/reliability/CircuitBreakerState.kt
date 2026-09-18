package com.yugahashimoto.andcode.core.reliability

enum class CircuitState { CLOSED, OPEN, HALF_OPEN }

data class CircuitBreakerState(
    val state: CircuitState = CircuitState.CLOSED,
    private val failureCount: Int = 0,
    private val successCount: Int = 0,
    private val lastFailureMillis: Long = 0L,
    private val failureThreshold: Int = 5,
    private val resetTimeoutMillis: Long = 60_000L,
    private val successThreshold: Int = 3,
) {
    fun recordSuccess(): CircuitBreakerState = when (state) {
        CircuitState.HALF_OPEN -> if (successCount + 1 >= successThreshold) {
            copy(state = CircuitState.CLOSED, failureCount = 0, successCount = 0)
        } else {
            copy(successCount = successCount + 1)
        }
        CircuitState.OPEN -> this
        CircuitState.CLOSED -> copy(failureCount = 0, successCount = 0)
    }

    fun recordFailure(): CircuitBreakerState = when (state) {
        CircuitState.HALF_OPEN -> copy(
            state = CircuitState.OPEN,
            failureCount = failureCount + 1,
            successCount = 0,
            lastFailureMillis = System.currentTimeMillis(),
        )
        CircuitState.CLOSED -> if (failureCount + 1 >= failureThreshold) {
            copy(
                state = CircuitState.OPEN,
                failureCount = failureCount + 1,
                lastFailureMillis = System.currentTimeMillis(),
            )
        } else {
            copy(failureCount = failureCount + 1)
        }
        CircuitState.OPEN -> this
    }

    fun attemptReset(): CircuitBreakerState = when (state) {
        CircuitState.OPEN -> if (System.currentTimeMillis() - lastFailureMillis >= resetTimeoutMillis) {
            copy(state = CircuitState.HALF_OPEN, successCount = 0)
        } else {
            this
        }
        else -> this
    }
}
