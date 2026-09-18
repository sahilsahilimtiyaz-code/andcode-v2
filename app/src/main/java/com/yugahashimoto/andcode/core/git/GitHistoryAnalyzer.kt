package com.yugahashimoto.andcode.core.git

class GitHistoryAnalyzer(
    private val shellExecutor: suspend (String) -> String,
) {

    suspend fun log(limit: Int = 20): List<GitLogEntry> {
        val output = shellExecutor(
            "git log --format='%H|%h|%an|%ad|%s' --date=short -n $limit"
        )
        return output.lines().filter { it.isNotBlank() }.mapNotNull { line ->
            val parts = line.split("|", limit = 5)
            if (parts.size >= 5) {
                GitLogEntry(
                    hash = parts[0],
                    shortHash = parts[1],
                    author = parts[2],
                    date = parts[3],
                    subject = parts[4],
                )
            } else null
        }
    }

    suspend fun blame(filePath: String): GitBlame {
        val output = shellExecutor("git blame --porcelain $filePath")
        val lines = output.lines()
        val blameLines = mutableListOf<BlameLine>()
        var i = 0

        while (i < lines.size) {
            val headerPattern = Regex("""^([0-9a-f]+)\s+(\d+)\s+(\d+)(\s+\d+)?$""")
            headerPattern.find(lines[i])?.let { header ->
                val commitHash = header.groupValues[1]
                val lineNum = header.groupValues[3].toIntOrNull() ?: 0
                var author = ""
                var date = ""

                i++
                while (i < lines.size && !lines[i].matches(headerPattern)) {
                    when {
                        lines[i].startsWith("author ") -> author = lines[i].substringAfter("author ")
                        lines[i].startsWith("author-time ") -> {
                            val epoch = lines[i].substringAfter("author-time ").toLongOrNull() ?: 0L
                            date = java.time.Instant.ofEpochSecond(epoch)
                                .atZone(java.time.ZoneId.systemDefault())
                                .toLocalDate().toString()
                        }
                        lines[i].startsWith("\t") -> {
                            blameLines.add(
                                BlameLine(
                                    lineNumber = lineNum,
                                    commitHash = commitHash,
                                    author = author,
                                    date = date,
                                    content = lines[i].substring(1),
                                )
                            )
                        }
                    }
                    i++
                }
            } ?: i++
        }

        return GitBlame(filePath = filePath, lines = blameLines)
    }

    suspend fun diff(ref1: String = "HEAD~1", ref2: String = "HEAD"): String {
        return shellExecutor("git diff $ref1 $ref2 --stat")
    }

    suspend fun status(): Map<String, String> {
        val output = shellExecutor("git status --porcelain")
        return output.lines().filter { it.isNotBlank() }.associate { line ->
            val status = line.substring(0, 2).trim()
            val path = line.substring(3).trim()
            path to status
        }
    }

    suspend fun currentBranch(): String {
        return shellExecutor("git rev-parse --abbrev-ref HEAD").trim()
    }
}
