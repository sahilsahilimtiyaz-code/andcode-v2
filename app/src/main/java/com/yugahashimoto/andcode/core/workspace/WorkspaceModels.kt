package com.yugahashimoto.andcode.core.workspace

data class Workspace(
    val id: String,
    val name: String,
    val rootPath: String,
    val description: String = "",
    val type: WorkspaceType = WorkspaceType.LOCAL,
    val createdAt: Long = System.currentTimeMillis(),
    val lastOpenedAt: Long? = null,
    val isActive: Boolean = false,
    val tags: List<String> = emptyList(),
    val gitRemoteUrl: String? = null,
    val gitBranch: String? = null,
    val gitDirty: Boolean = false,
    val fileCount: Int = 0,
    val totalSizeBytes: Long = 0,
    val pinnedFiles: List<String> = emptyList(),
    val recentFiles: List<RecentFile> = emptyList(),
    val sharedContext: SharedContext? = null,
)

enum class WorkspaceType { LOCAL, REMOTE, IMPORTED, TEMPLATE }

data class RecentFile(
    val path: String,
    val name: String,
    val lastModifiedAt: Long,
    val openCount: Int = 1,
)

data class SharedContext(
    val workspaceIds: List<String>,
    val sharedVariables: Map<String, String> = emptyMap(),
    val sharedOutputs: Map<String, String> = emptyMap(),
    val createdAt: Long = System.currentTimeMillis(),
)

data class WorkspaceTask(
    val id: String,
    val workspaceId: String,
    val name: String,
    val command: String,
    val status: TaskStatus = TaskStatus.PENDING,
    val startedAt: Long? = null,
    val finishedAt: Long? = null,
    val output: String = "",
    val exitCode: Int? = null,
)

enum class TaskStatus { PENDING, RUNNING, SUCCESS, FAILED, CANCELLED }

data class WorkspaceDiff(
    val workspaceId: String,
    val addedFiles: List<String> = emptyList(),
    val modifiedFiles: List<String> = emptyList(),
    val deletedFiles: List<String> = emptyList(),
    val timestampMillis: Long = System.currentTimeMillis(),
)

data class WorkspaceState(
    val workspaces: List<Workspace> = emptyList(),
    val activeWorkspace: Workspace? = null,
    val recentWorkspaces: List<String> = emptyList(),
    val sharedContexts: List<SharedContext> = emptyList(),
)
