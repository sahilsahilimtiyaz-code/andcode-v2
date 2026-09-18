package com.yugahashimoto.andcode.core.git

object ConflictDetector {

    private val conflictStart = Regex("""^<<<<<<<\s*(.*)$""", RegexOption.MULTILINE)
    private val conflictSeparator = Regex("""^=======\s*$""", RegexOption.MULTILINE)
    private val conflictEnd = Regex("""^>>>>>>>\s*(.*)$""", RegexOption.MULTILINE)

    fun detect(filePath: String, content: String): MergeConflict {
        val regions = mutableListOf<ConflictRegion>()
        val lines = content.lines()

        var i = 0
        while (i < lines.size) {
            if (lines[i].startsWith("<<<<<<<")) {
                val startLine = i + 1
                val oursLines = mutableListOf<String>()
                i++

                while (i < lines.size && !lines[i].startsWith("=======")) {
                    oursLines.add(lines[i])
                    i++
                }
                i // skip separator

                val theirsLines = mutableListOf<String>()
                i++
                while (i < lines.size && !lines[i].startsWith(">>>>>>>")) {
                    theirsLines.add(lines[i])
                    i++
                }

                regions.add(
                    ConflictRegion(
                        startLine = startLine,
                        endLine = i + 1,
                        oursContent = oursLines.joinToString("\n"),
                        theirsContent = theirsLines.joinToString("\n"),
                    )
                )
            }
            i++
        }

        return MergeConflict(
            filePath = filePath,
            regions = regions,
            rawContent = content,
        )
    }

    fun detectInFiles(fileContents: Map<String, String>): List<MergeConflict> {
        return fileContents.mapNotNull { (path, content) ->
            val conflict = detect(path, content)
            if (conflict.hasConflicts) conflict else null
        }
    }

    fun hasConflictMarkers(content: String): Boolean {
        return content.contains("<<<<<<<") &&
            content.contains("=======") &&
            content.contains(">>>>>>>")
    }
}
