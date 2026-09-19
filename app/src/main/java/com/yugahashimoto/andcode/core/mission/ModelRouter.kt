package com.yugahashimoto.andcode.core.mission

import com.yugahashimoto.andcode.core.api.ProviderCatalog
import com.yugahashimoto.andcode.core.reliability.CircuitBreakerState
import com.yugahashimoto.andcode.core.reliability.CircuitState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class RoutingReason { HEALTHY, FALLBACK, RATE_LIMITED, CAPACITY, DEFAULT }

data class ProviderHealth(
    val providerId: String,
    val latencyMs: Long = 0L,
    val errorCount: Int = 0,
    val successCount: Int = 0,
    val lastCheckedMillis: Long = 0L,
    val circuitState: CircuitState = CircuitState.CLOSED,
) {
    val errorRate: Double
        get() {
            val total = errorCount + successCount
            return if (total > 0) errorCount.toDouble() / total else 0.0
        }
    val isHealthy: Boolean
        get() = circuitState != CircuitState.OPEN && errorRate < 0.5
}

data class RoutingDecision(
    val providerId: String,
    val modelId: String,
    val reason: RoutingReason,
    val availableContext: Long = 0L,
)

class ModelRouter(
    private val healthOverrides: Map<String, ProviderHealth> = emptyMap(),
) {
    private val mutableHealth = MutableStateFlow<Map<String, ProviderHealth>>(emptyMap())
    val providerHealth: StateFlow<Map<String, ProviderHealth>> = mutableHealth.asStateFlow()

    private val circuitBreakers = mutableMapOf<String, CircuitBreakerState>()

    fun recordSuccess(providerId: String) {
        val current = mutableHealth.value[providerId] ?: ProviderHealth(providerId)
        mutableHealth.value = mutableHealth.value + (providerId to current.copy(
            successCount = current.successCount + 1,
            lastCheckedMillis = System.currentTimeMillis(),
        ))
        circuitBreakers[providerId] = (circuitBreakers[providerId] ?: CircuitBreakerState()).recordSuccess()
    }

    fun recordFailure(providerId: String) {
        val current = mutableHealth.value[providerId] ?: ProviderHealth(providerId)
        mutableHealth.value = mutableHealth.value + (providerId to current.copy(
            errorCount = current.errorCount + 1,
            lastCheckedMillis = System.currentTimeMillis(),
        ))
        val breaker = (circuitBreakers[providerId] ?: CircuitBreakerState()).recordFailure()
        circuitBreakers[providerId] = breaker
        val updated = mutableHealth.value[providerId] ?: current
        mutableHealth.value = mutableHealth.value + (providerId to updated.copy(
            circuitState = breaker.state,
        ))
    }

    fun recordRateLimit(providerId: String) {
        recordFailure(providerId)
    }

    fun selectModel(
        catalog: ProviderCatalog,
        preferredProviderId: String? = null,
        preferredModelId: String? = null,
        requiredContextTokens: Long = 0L,
    ): RoutingDecision {
        val connected = catalog.connected.toSet()
        if (connected.isEmpty()) {
            return RoutingDecision(
                providerId = preferredProviderId ?: "opencode",
                modelId = preferredModelId ?: "default",
                reason = RoutingReason.DEFAULT,
            )
        }

        if (preferredProviderId != null && preferredModelId != null && preferredProviderId in connected) {
            val provider = catalog.all.firstOrNull { it.id == preferredProviderId }
            val model = provider?.models?.get(preferredModelId)
            if (model != null) {
                val health = getHealth(preferredProviderId)
                if (health.isHealthy) {
                    val contextOk = requiredContextTokens <= 0L || (model.limit?.context ?: 0L) >= requiredContextTokens
                    if (contextOk) {
                        return RoutingDecision(
                            providerId = preferredProviderId,
                            modelId = preferredModelId,
                            reason = RoutingReason.HEALTHY,
                            availableContext = model.limit?.context ?: 0L,
                        )
                    }
                }
            }
        }

        for (provider in catalog.all.filter { it.id in connected }) {
            val health = getHealth(provider.id)
            if (!health.isHealthy) continue

            for ((modelId, model) in provider.models) {
                val contextOk = requiredContextTokens <= 0L || (model.limit?.context ?: 0L) >= requiredContextTokens
                if (contextOk) {
                    return RoutingDecision(
                        providerId = provider.id,
                        modelId = modelId,
                        reason = RoutingReason.FALLBACK,
                        availableContext = model.limit?.context ?: 0L,
                    )
                }
            }
        }

        for (provider in catalog.all.filter { it.id in connected }) {
            for ((modelId, model) in provider.models) {
                return RoutingDecision(
                    providerId = provider.id,
                    modelId = modelId,
                    reason = RoutingReason.CAPACITY,
                    availableContext = model.limit?.context ?: 0L,
                )
            }
        }

        return RoutingDecision(
            providerId = connected.firstOrNull() ?: "opencode",
            modelId = preferredModelId ?: "default",
            reason = RoutingReason.DEFAULT,
        )
    }

    fun getHealth(providerId: String): ProviderHealth =
        healthOverrides[providerId]
            ?: mutableHealth.value[providerId]
            ?: ProviderHealth(providerId)

    fun resetCircuitBreaker(providerId: String) {
        circuitBreakers.remove(providerId)
        val current = mutableHealth.value[providerId] ?: return
        mutableHealth.value = mutableHealth.value + (providerId to current.copy(circuitState = CircuitState.CLOSED))
    }
}
