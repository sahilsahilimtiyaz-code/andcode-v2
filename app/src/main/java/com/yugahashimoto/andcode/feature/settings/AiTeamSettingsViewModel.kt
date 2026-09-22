package com.yugahashimoto.andcode.feature.settings

import com.yugahashimoto.andcode.core.mission.AgentRole
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class AiTeamSettingsViewModel(
    private val initialSettings: AiTeamSettings = AiTeamSettings(),
) {
    private val mutableSettings = MutableStateFlow(initialSettings)
    val settings: StateFlow<AiTeamSettings> = mutableSettings.asStateFlow()

    fun toggleAgent(role: AgentRole, enabled: Boolean) {
        mutableSettings.update { state ->
            val entry = state.getEntry(role)
            state.copy(agents = state.agents + (role to entry.copy(enabled = enabled)))
        }
    }

    fun setAutonomyLevel(role: AgentRole, level: AutonomyLevel) {
        mutableSettings.update { state ->
            val entry = state.getEntry(role)
            state.copy(agents = state.agents + (role to entry.copy(autonomyLevel = level)))
        }
    }

    fun setBudget(role: AgentRole, budget: AgentBudgetConfig) {
        mutableSettings.update { state ->
            val entry = state.getEntry(role)
            state.copy(agents = state.agents + (role to entry.copy(budget = budget)))
        }
    }

    fun setPreferredModel(role: AgentRole, providerId: String?, modelId: String?) {
        mutableSettings.update { state ->
            val entry = state.getEntry(role)
            state.copy(agents = state.agents + (role to entry.copy(
                preferredProviderId = providerId,
                preferredModelId = modelId,
            )))
        }
    }

    fun setGlobalAutonomyLevel(level: AutonomyLevel) {
        mutableSettings.update { it.copy(globalAutonomyLevel = level) }
    }

    fun setMaxConcurrentAgents(max: Int) {
        mutableSettings.update { it.copy(maxConcurrentAgents = max.coerceIn(1, 6)) }
    }

    fun setEnableParallelAgents(enabled: Boolean) {
        mutableSettings.update { it.copy(enableParallelAgents = enabled) }
    }

    fun setAutoRetryFailedAgents(enabled: Boolean) {
        mutableSettings.update { it.copy(autoRetryFailedAgents = enabled) }
    }

    fun setFailFastOnAgentFailure(enabled: Boolean) {
        mutableSettings.update { it.copy(failFastOnAgentFailure = enabled) }
    }

    fun resetToDefaults() {
        mutableSettings.value = AiTeamSettings()
    }
}
