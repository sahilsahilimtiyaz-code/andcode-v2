package com.yugahashimoto.andcode.feature.premium.engines

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class AgentRole(val displayName: String, val icon: String) {
    PLANNER("Planner", "🧠"),
    CODER("Coder", "💻"),
    DEBUGGER("Debugger", "🐛"),
    REVIEWER("Reviewer", "🔍"),
    TESTER("Tester", "🧪"),
    BUILD_ENGINEER("Build Engineer", "🔧")
}

data class AgentState(
    val role: AgentRole,
    val isActive: Boolean = false,
    val currentTask: String = "",
    val completedTasks: Int = 0
)

data class MissionStep(
    val id: Int,
    val name: String,
    val status: StepStatus = StepStatus.PENDING,
    val assignedAgent: AgentRole? = null,
    val description: String = ""
)

enum class StepStatus {
    PENDING, RUNNING, COMPLETED, FAILED, ROLLED_BACK
}

data class MissionState(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val currentStep: Int = 0,
    val totalSteps: Int = 12,
    val steps: List<MissionStep> = emptyList(),
    val agents: Map<AgentRole, AgentState> = emptyMap(),
    val isRunning: Boolean = false,
    val isCompleted: Boolean = false,
    val hasFailed: Boolean = false,
    val checkpoints: List<Checkpoint> = emptyList()
)

data class Checkpoint(
    val id: String,
    val stepId: Int,
    val timestamp: Long,
    val canRollback: Boolean = true
)

class MissionEngine {
    private val _state = MutableStateFlow(MissionState())
    val state: StateFlow<MissionState> = _state.asStateFlow()

    private val defaultSteps = listOf(
        MissionStep(1, "Analyze Requirements", description = "Parse and understand project requirements"),
        MissionStep(2, "Plan Architecture", assignedAgent = AgentRole.PLANNER, description = "Design system architecture"),
        MissionStep(3, "Setup Project", description = "Initialize project structure"),
        MissionStep(4, "Core Implementation", assignedAgent = AgentRole.CODER, description = "Build core features"),
        MissionStep(5, "Unit Testing", assignedAgent = AgentRole.TESTER, description = "Write and run unit tests"),
        MissionStep(6, "Integration", assignedAgent = AgentRole.CODER, description = "Integrate components"),
        MissionStep(7, "Code Review", assignedAgent = AgentRole.REVIEWER, description = "Review code quality"),
        MissionStep(8, "Bug Fixes", assignedAgent = AgentRole.DEBUGGER, description = "Fix identified issues"),
        MissionStep(9, "Performance Tuning", assignedAgent = AgentRole.CODER, description = "Optimize performance"),
        MissionStep(10, "Build Configuration", assignedAgent = AgentRole.BUILD_ENGINEER, description = "Configure build system"),
        MissionStep(11, "Final Testing", assignedAgent = AgentRole.TESTER, description = "Run comprehensive tests"),
        MissionStep(12, "Deployment", assignedAgent = AgentRole.BUILD_ENGINEER, description = "Prepare for deployment")
    )

    fun initialize(name: String, description: String = "") {
        val agents = AgentRole.entries.associateWith { role ->
            AgentState(role = role)
        }
        _state.value = MissionState(
            id = "mission_${System.currentTimeMillis()}",
            name = name,
            description = description,
            steps = defaultSteps,
            agents = agents,
            totalSteps = defaultSteps.size
        )
    }

    fun startMission() {
        _state.value = _state.value.copy(isRunning = true, hasFailed = false)
    }

    fun advanceStep() {
        val current = _state.value
        if (current.currentStep < current.totalSteps) {
            val updatedSteps = current.steps.toMutableList()
            if (current.currentStep > 0) {
                updatedSteps[current.currentStep - 1] = updatedSteps[current.currentStep - 1].copy(
                    status = StepStatus.COMPLETED
                )
            }
            updatedSteps[current.currentStep] = updatedSteps[current.currentStep].copy(
                status = StepStatus.RUNNING
            )
            _state.value = current.copy(
                currentStep = current.currentStep + 1,
                steps = updatedSteps
            )
        } else {
            completeMission()
        }
    }

    fun completeMission() {
        val current = _state.value
        val updatedSteps = current.steps.map {
            if (it.status == StepStatus.RUNNING) it.copy(status = StepStatus.COMPLETED) else it
        }
        _state.value = current.copy(
            isRunning = false,
            isCompleted = true,
            steps = updatedSteps
        )
    }

    fun failMission(reason: String = "") {
        _state.value = _state.value.copy(
            isRunning = false,
            hasFailed = true
        )
    }

    fun rollbackToCheckpoint(checkpointId: String) {
        val checkpoint = _state.value.checkpoints.find { it.id == checkpointId } ?: return
        val updatedSteps = _state.value.steps.toMutableList()
        for (i in checkpoint.stepId until updatedSteps.size) {
            updatedSteps[i] = updatedSteps[i].copy(status = StepStatus.PENDING)
        }
        _state.value = _state.value.copy(
            currentStep = checkpoint.stepId,
            steps = updatedSteps,
            isRunning = false,
            hasFailed = false
        )
    }

    fun getProgress(): Float {
        val state = _state.value
        return if (state.totalSteps > 0) state.currentStep.toFloat() / state.totalSteps else 0f
    }
}
