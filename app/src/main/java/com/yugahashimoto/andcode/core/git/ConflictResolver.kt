package com.yugahashimoto.andcode.core.git

class ConflictResolver {

    fun resolve(conflict: MergeConflict, strategy: ResolutionStrategy): ConflictResolution {
        val resolved = when (strategy) {
            ResolutionStrategy.TAKE_OURS -> resolveWithOurs(conflict)
            ResolutionStrategy.TAKE_THEIRS -> resolveWithTheirs(conflict)
            ResolutionStrategy.UNION -> resolveWithUnion(conflict)
            ResolutionStrategy.MANUAL -> conflict.rawContent
        }
        return ConflictResolution(
            filePath = conflict.filePath,
            strategy = strategy,
            resolvedContent = resolved,
        )
    }

    fun resolveAll(conflicts: List<MergeConflict>, strategy: ResolutionStrategy): List<ConflictResolution> {
        return conflicts.map { resolve(it, strategy) }
    }

    private fun resolveWithOurs(conflict: MergeConflict): String {
        val lines = conflict.rawContent.lines()
        val result = mutableListOf<String>()
        var i = 0

        while (i < lines.size) {
            if (lines[i].startsWith("<<<<<<<")) {
                i++
                while (i < lines.size && !lines[i].startsWith("=======")) {
                    result.add(lines[i])
                    i++
                }
                i++ // skip separator
                while (i < lines.size && !lines[i].startsWith(">>>>>>>")) {
                    i++ // skip theirs
                }
                i++ // skip end marker
            } else {
                result.add(lines[i])
                i++
            }
        }

        return result.joinToString("\n")
    }

    private fun resolveWithTheirs(conflict: MergeConflict): String {
        val lines = conflict.rawContent.lines()
        val result = mutableListOf<String>()
        var i = 0

        while (i < lines.size) {
            if (lines[i].startsWith("<<<<<<<")) {
                i++ // skip start marker
                while (i < lines.size && !lines[i].startsWith("=======")) {
                    i++ // skip ours
                }
                i++ // skip separator
                while (i < lines.size && !lines[i].startsWith(">>>>>>>")) {
                    result.add(lines[i])
                    i++
                }
                i++ // skip end marker
            } else {
                result.add(lines[i])
                i++
            }
        }

        return result.joinToString("\n")
    }

    private fun resolveWithUnion(conflict: MergeConflict): String {
        val lines = conflict.rawContent.lines()
        val result = mutableListOf<String>()
        var i = 0

        while (i < lines.size) {
            if (lines[i].startsWith("<<<<<<<")) {
                i++ // skip start marker
                while (i < lines.size && !lines[i].startsWith("=======")) {
                    result.add(lines[i])
                    i++
                }
                i++ // skip separator
                while (i < lines.size && !lines[i].startsWith(">>>>>>>")) {
                    result.add(lines[i])
                    i++
                }
                i++ // skip end marker
            } else {
                result.add(lines[i])
                i++
            }
        }

        return result.joinToString("\n")
    }
}
