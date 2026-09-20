package com.yugahashimoto.andcode.feature.premium.engines

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ModelProvider(
    val id: String,
    val name: String,
    val latencyMs: Long = 0,
    val isAvailable: Boolean = true,
    val healthScore: Float = 1f,
    val lastError: String? = null
)

data class FallbackStrategy(
    val id: String,
    val name: String,
    val providers: List<String>,
    val isActive: Boolean = false
)

data class ModelRouterState(
    val providers: List<ModelProvider> = emptyList(),
    val strategies: List<FallbackStrategy> = emptyList(),
    val activeStrategyId: String? = null,
    val currentProviderId: String? = null
)

class ModelRouter {
    private val _state = MutableStateFlow(ModelRouterState())
    val state: StateFlow<ModelRouterState> = _state.asStateFlow()

    init {
        _state.value = ModelRouterState(
            providers = listOf(
                ModelProvider("openai", "OpenAI", 120, true, 0.95f),
                ModelProvider("anthropic", "Anthropic", 150, true, 0.92f),
                ModelProvider("google", "Google AI", 100, true, 0.98f),
                ModelProvider("local", "Local Model", 50, true, 0.85f)
            ),
            strategies = listOf(
                FallbackStrategy("primary", "Primary Only", listOf("openai"), true),
                FallbackStrategy("balanced", "Balanced", listOf("openai", "anthropic", "google")),
                FallbackStrategy("local-first", "Local First", listOf("local", "openai", "anthropic"))
            ),
            activeStrategyId = "primary",
            currentProviderId = "openai"
        )
    }

    fun selectStrategy(strategyId: String) {
        _state.value = _state.value.copy(activeStrategyId = strategyId)
    }

    fun updateProviderHealth(providerId: String, latency: Long, healthy: Boolean, healthScore: Float) {
        val providers = _state.value.providers.map {
            if (it.id == providerId) it.copy(
                latencyMs = latency,
                isAvailable = healthy,
                healthScore = healthScore
            ) else it
        }
        _state.value = _state.value.copy(providers = providers)
    }

    fun selectProvider(providerId: String) {
        _state.value = _state.value.copy(currentProviderId = providerId)
    }

    fun getHealthyProviders(): List<ModelProvider> {
        return _state.value.providers.filter { it.isAvailable }
    }
}
