package com.yugahashimoto.andcode.feature.settings

import com.yugahashimoto.andcode.core.mission.AgentRole

enum class AutonomyLevel(val displayName: String) {
    OFF("Off"),
    SEMI("Semi-Autonomous"),
    FULL("Fully Autonomous"),
}

data class AgentBudgetConfig(
    val maxTokensPerTurn: Long = 8192L,
    val maxTurns: Int = 20,
    val maxCostUsd: Double = 1.0,
)

data class AgentTeamEntry(
    val role: AgentRole,
    val enabled: Boolean = true,
    val autonomyLevel: AutonomyLevel = AutonomyLevel.SEMI,
    val budget: AgentBudgetConfig = AgentBudgetConfig(),
    val preferredProviderId: String? = null,
    val preferredModelId: String? = null,
)

data class AiTeamSettings(
    val agents: Map<AgentRole, AgentTeamEntry> = AgentRole.entries.associateWith { role ->
        AgentTeamEntry(role = role)
    },
    val globalAutonomyLevel: AutonomyLevel = AutonomyLevel.SEMI,
    val enableParallelAgents: Boolean = true,
    val maxConcurrentAgents: Int = 3,
    val autoRetryFailedAgents: Boolean = true,
    val failFastOnAgentFailure: Boolean = false,
) {
    fun getEntry(role: AgentRole): AgentTeamEntry =
        agents[role] ?: AgentTeamEntry(role = role)

    fun isAgentEnabled(role: AgentRole): Boolean = getEntry(role).enabled
}
