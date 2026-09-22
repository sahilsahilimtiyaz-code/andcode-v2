package com.yugahashimoto.andcode.core.git

data class ConflictRegion(
    val startLine: Int,
    val endLine: Int,
    val oursContent: String,
    val theirsContent: String,
    val baseContent: String? = null,
)

data class MergeConflict(
    val filePath: String,
    val regions: List<ConflictRegion>,
    val rawContent: String,
) {
    val hasConflicts: Boolean get() = regions.isNotEmpty()
    val conflictCount: Int get() = regions.size
}

enum class ResolutionStrategy { TAKE_OURS, TAKE_THEIRS, UNION, MANUAL }

data class ConflictResolution(
    val filePath: String,
    val strategy: ResolutionStrategy,
    val resolvedContent: String,
    val timestampMillis: Long = System.currentTimeMillis(),
)

data class GitCommit(
    val hash: String,
    val shortHash: String,
    val author: String,
    val date: String,
    val message: String,
    val filesChanged: List<String> = emptyList(),
)

data class GitBlame(
    val filePath: String,
    val lines: List<BlameLine>,
)

data class BlameLine(
    val lineNumber: Int,
    val commitHash: String,
    val author: String,
    val date: String,
    val content: String,
)

data class GitLogEntry(
    val hash: String,
    val shortHash: String,
    val author: String,
    val date: String,
    val subject: String,
    val body: String = "",
)
