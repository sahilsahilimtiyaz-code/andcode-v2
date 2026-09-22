package com.yugahashimoto.andcode.feature.settings

import com.yugahashimoto.andcode.core.mission.AgentRole
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AiTeamSettingsViewModelTest {

    private lateinit var viewModel: AiTeamSettingsViewModel

    @Before
    fun setup() {
        viewModel = AiTeamSettingsViewModel()
    }

    @Test
    fun `initial state has all agents enabled with SEMI autonomy`() {
        val settings = viewModel.settings.value
        AgentRole.entries.forEach { role ->
            val entry = settings.getEntry(role)
            assertTrue(entry.enabled)
            assertEquals(AutonomyLevel.SEMI, entry.autonomyLevel)
        }
    }

    @Test
    fun `toggleAgent disables agent`() {
        viewModel.toggleAgent(AgentRole.CODER, false)
        val entry = viewModel.settings.value.getEntry(AgentRole.CODER)
        assertFalse(entry.enabled)
    }

    @Test
    fun `setAutonomyLevel changes level`() {
        viewModel.setAutonomyLevel(AgentRole.PLANNER, AutonomyLevel.FULL)
        val entry = viewModel.settings.value.getEntry(AgentRole.PLANNER)
        assertEquals(AutonomyLevel.FULL, entry.autonomyLevel)
    }

    @Test
    fun `setBudget updates budget config`() {
        val budget = AgentBudgetConfig(maxTokensPerTurn = 16384, maxTurns = 10, maxCostUsd = 0.5)
        viewModel.setBudget(AgentRole.TESTER, budget)
        val entry = viewModel.settings.value.getEntry(AgentRole.TESTER)
        assertEquals(16384L, entry.budget.maxTokensPerTurn)
        assertEquals(10, entry.budget.maxTurns)
    }

    @Test
    fun `setPreferredModel updates provider and model`() {
        viewModel.setPreferredModel(AgentRole.CODER, "anthropic", "claude-sonnet")
        val entry = viewModel.settings.value.getEntry(AgentRole.CODER)
        assertEquals("anthropic", entry.preferredProviderId)
        assertEquals("claude-sonnet", entry.preferredModelId)
    }

    @Test
    fun `setMaxConcurrentAgents clamps to valid range`() {
        viewModel.setMaxConcurrentAgents(10)
        assertEquals(6, viewModel.settings.value.maxConcurrentAgents)
        viewModel.setMaxConcurrentAgents(0)
        assertEquals(1, viewModel.settings.value.maxConcurrentAgents)
    }

    @Test
    fun `resetToDefaults restores initial state`() {
        viewModel.toggleAgent(AgentRole.BUILD_ENGINEER, false)
        viewModel.setGlobalAutonomyLevel(AutonomyLevel.FULL)
        viewModel.resetToDefaults()
        val settings = viewModel.settings.value
        assertTrue(settings.isAgentEnabled(AgentRole.BUILD_ENGINEER))
        assertEquals(AutonomyLevel.SEMI, settings.globalAutonomyLevel)
    }

    @Test
    fun `getEntry returns default for missing role`() {
        val settings = AiTeamSettings(agents = emptyMap())
        val entry = settings.getEntry(AgentRole.CODER)
        assertNotNull(entry)
        assertEquals(AgentRole.CODER, entry.role)
    }

    @Test
    fun `isAgentEnabled delegates to entry`() {
        val settings = AiTeamSettings(
            agents = mapOf(AgentRole.CODER to AgentTeamEntry(role = AgentRole.CODER, enabled = false))
        )
        assertFalse(settings.isAgentEnabled(AgentRole.CODER))
    }
}
