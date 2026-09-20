package com.yugahashimoto.andcode.feature.premium.engines

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ConflictSide {
    OURS, THEIRS, UNION
}

data class ConflictHunk(
    val id: String,
    val filePath: String,
    val startLine: Int,
    val endLine: Int,
    val oursContent: String,
    val theirsContent: String,
    val resolvedContent: String? = null,
    val resolution: ConflictSide? = null
)

data class ConflictResolverState(
    val conflicts: List<ConflictHunk> = emptyList(),
    val currentConflictIndex: Int = 0,
    val isResolving: Boolean = false
)

class ConflictResolver {
    private val _state = MutableStateFlow(ConflictResolverState())
    val state: StateFlow<ConflictResolverState> = _state.asStateFlow()

    fun loadConflicts(conflicts: List<ConflictHunk>) {
        _state.value = ConflictResolverState(conflicts = conflicts)
    }

    fun resolveCurrent(side: ConflictSide) {
        val current = _state.value
        if (current.currentConflictIndex >= current.conflicts.size) return

        val conflict = current.conflicts[current.currentConflictIndex]
        val resolved = when (side) {
            ConflictSide.OURS -> conflict.oursContent
            ConflictSide.THEIRS -> conflict.theirsContent
            ConflictSide.UNION -> "${conflict.oursContent}\n${conflict.theirsContent}"
        }

        val updated = current.conflicts.toMutableList()
        updated[current.currentConflictIndex] = conflict.copy(
            resolvedContent = resolved,
            resolution = side
        )

        _state.value = current.copy(
            conflicts = updated,
            currentConflictIndex = current.currentConflictIndex + 1
        )
    }

    fun resolveById(conflictId: String, side: ConflictSide) {
        val current = _state.value
        val index = current.conflicts.indexOfFirst { it.id == conflictId }
        if (index == -1) return

        val conflict = current.conflicts[index]
        val resolved = when (side) {
            ConflictSide.OURS -> conflict.oursContent
            ConflictSide.THEIRS -> conflict.theirsContent
            ConflictSide.UNION -> "${conflict.oursContent}\n${conflict.theirsContent}"
        }

        val updated = current.conflicts.toMutableList()
        updated[index] = conflict.copy(resolvedContent = resolved, resolution = side)
        _state.value = current.copy(conflicts = updated)
    }

    fun getResolvedCount(): Int = _state.value.conflicts.count { it.resolution != null }
    fun getTotalCount(): Int = _state.value.conflicts.size

    fun isAllResolved(): Boolean = _state.value.conflicts.all { it.resolution != null }
}
