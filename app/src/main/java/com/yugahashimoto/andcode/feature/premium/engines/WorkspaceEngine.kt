package com.yugahashimoto.andcode.feature.premium.engines

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class WorkspaceInfo(
    val id: String,
    val name: String,
    val path: String,
    val isActive: Boolean = false,
    val lastModified: Long = System.currentTimeMillis()
)

data class WorkspaceEngineState(
    val workspaces: List<WorkspaceInfo> = emptyList(),
    val activeWorkspaceId: String? = null
)

class WorkspaceEngine {
    private val _state = MutableStateFlow(WorkspaceEngineState())
    val state: StateFlow<WorkspaceEngineState> = _state.asStateFlow()

    fun loadWorkspaces(workspaces: List<WorkspaceInfo>) {
        _state.value = WorkspaceEngineState(workspaces = workspaces)
    }

    fun switchWorkspace(workspaceId: String) {
        val workspaces = _state.value.workspaces.map {
            it.copy(isActive = it.id == workspaceId)
        }
        _state.value = _state.value.copy(
            workspaces = workspaces,
            activeWorkspaceId = workspaceId
        )
    }

    fun addWorkspace(workspace: WorkspaceInfo) {
        _state.value = _state.value.copy(
            workspaces = _state.value.workspaces + workspace
        )
    }

    fun removeWorkspace(workspaceId: String) {
        _state.value = _state.value.copy(
            workspaces = _state.value.workspaces.filter { it.id != workspaceId }
        )
    }

    fun getActiveWorkspace(): WorkspaceInfo? {
        return _state.value.workspaces.find { it.isActive }
    }
}
