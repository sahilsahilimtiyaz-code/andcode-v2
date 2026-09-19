package com.yugahashimoto.andcode.core.workspace

class WorkspaceEngine(
    private val commandRunner: suspend (String, String?, Int) -> Pair<Int, String>,
) {
    private val workspaces = mutableMapOf<String, Workspace>()
    private val recentIds = mutableListOf<String>()
    private val sharedContexts = mutableListOf<SharedContext>()

    fun getWorkspaces(): List<Workspace> = workspaces.values.toList()

    fun getWorkspaceById(id: String): Workspace? = workspaces[id]

    fun getActiveWorkspace(): Workspace? = workspaces.values.find { it.isActive }

    fun getRecentWorkspaces(): List<Workspace> =
        recentIds.mapNotNull { workspaces[it] }

    fun createWorkspace(workspace: Workspace): Workspace {
        val created = workspace.copy(isActive = false)
        workspaces[created.id] = created
        return created
    }

    fun updateWorkspace(id: String, update: (Workspace) -> Workspace): Workspace? {
        val current = workspaces[id] ?: return null
        val updated = update(current)
        workspaces[id] = updated
        return updated
    }

    fun deleteWorkspace(id: String): Boolean {
        val removed = workspaces.remove(id)
        recentIds.remove(id)
        if (removed?.isActive == true) {
            workspaces.values.firstOrNull()?.let { first ->
                workspaces[first.id] = first.copy(isActive = true)
            }
        }
        return removed != null
    }

    fun openWorkspace(id: String): Workspace? {
        val target = workspaces[id] ?: return null
        workspaces.values.forEach { ws ->
            workspaces[ws.id] = ws.copy(isActive = ws.id == id)
        }
        recentIds.remove(id)
        recentIds.add(0, id)
        if (recentIds.size > MAX_RECENT) {
            recentIds.removeAt(recentIds.lastIndex)
        }
        val updated = target.copy(isActive = true, lastOpenedAt = System.currentTimeMillis())
        workspaces[id] = updated
        return updated
    }

    fun setActiveWorkspace(id: String): Workspace? {
        return openWorkspace(id)
    }

    fun addPinnedFile(workspaceId: String, filePath: String): Boolean {
        val ws = workspaces[workspaceId] ?: return false
        if (filePath in ws.pinnedFiles) return false
        workspaces[workspaceId] = ws.copy(pinnedFiles = ws.pinnedFiles + filePath)
        return true
    }

    fun removePinnedFile(workspaceId: String, filePath: String): Boolean {
        val ws = workspaces[workspaceId] ?: return false
        workspaces[workspaceId] = ws.copy(pinnedFiles = ws.pinnedFiles - filePath)
        return true
    }

    fun addRecentFile(workspaceId: String, file: RecentFile) {
        val ws = workspaces[workspaceId] ?: return
        val existing = ws.recentFiles.toMutableList()
        val idx = existing.indexOfFirst { it.path == file.path }
        if (idx >= 0) {
            existing[idx] = existing[idx].copy(
                lastModifiedAt = file.lastModifiedAt,
                openCount = existing[idx].openCount + 1,
            )
        } else {
            existing.add(0, file)
            if (existing.size > MAX_RECENT_FILES) {
                existing.removeAt(existing.lastIndex)
            }
        }
        workspaces[workspaceId] = ws.copy(recentFiles = existing)
    }

    fun createSharedContext(workspaceIds: List<String>, variables: Map<String, String> = emptyMap()): SharedContext {
        val context = SharedContext(
            workspaceIds = workspaceIds,
            sharedVariables = variables,
        )
        sharedContexts.add(context)
        workspaceIds.forEach { wsId ->
            workspaces[wsId]?.let { ws ->
                workspaces[wsId] = ws.copy(sharedContext = context)
            }
        }
        return context
    }

    fun getSharedContexts(): List<SharedContext> = sharedContexts.toList()

    suspend fun detectGitStatus(workspaceId: String): Workspace? {
        val ws = workspaces[workspaceId] ?: return null
        return try {
            val branchResult = commandRunner("git branch --show-current", ws.rootPath, 5_000)
            val statusResult = commandRunner("git status --porcelain", ws.rootPath, 5_000)
            val remoteResult = commandRunner("git remote get-url origin", ws.rootPath, 5_000)
            val dirty = statusResult.second.isNotBlank()
            val branch = branchResult.second.trim().ifBlank { null }
            val remote = remoteResult.second.trim().ifBlank { null }
            val updated = ws.copy(
                gitBranch = branch,
                gitRemoteUrl = remote,
                gitDirty = dirty,
            )
            workspaces[workspaceId] = updated
            updated
        } catch (e: Exception) {
            ws
        }
    }

    suspend fun listFiles(workspaceId: String, path: String = "."): List<String> {
        val ws = workspaces[workspaceId] ?: return emptyList()
        return try {
            val result = commandRunner("find $path -maxdepth 1 -type f", ws.rootPath, 10_000)
            result.second.lines().filter { it.isNotBlank() }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun searchWorkspaces(query: String): List<Workspace> {
        val lowerQuery = query.lowercase()
        return workspaces.values.filter { ws ->
            ws.name.lowercase().contains(lowerQuery) ||
                ws.description.lowercase().contains(lowerQuery) ||
                ws.tags.any { it.lowercase().contains(lowerQuery) }
        }
    }

    fun filterByTag(tag: String): List<Workspace> =
        workspaces.values.filter { tag in it.tags }

    companion object {
        const val MAX_RECENT = 10
        const val MAX_RECENT_FILES = 20
    }
}
