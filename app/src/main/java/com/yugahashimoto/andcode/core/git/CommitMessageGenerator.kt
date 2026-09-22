package com.yugahashimoto.andcode.core.git

class CommitMessageGenerator(
    private val shellExecutor: suspend (String) -> String,
) {

    suspend fun generate(stagedDiff: String? = null): String {
        val diff = stagedDiff ?: shellExecutor("git diff --cached")
        val status = shellExecutor("git status --porcelain --cached")

        val changedFiles = parseChangedFiles(status)
        val groups = groupByType(changedFiles)

        val prefix = determinePrefix(groups)
        val scope = determineScope(changedFiles)
        val description = generateDescription(groups, diff)

        return buildString {
            append(prefix)
            if (scope != null) append("($scope)")
            append(": ")
            append(description)
        }
    }

    private fun parseChangedFiles(status: String): List<ChangedFile> {
        return status.lines().filter { it.isNotBlank() }.mapNotNull { line ->
            if (line.length < 4) return@mapNotNull null
            val code = line.substring(0, 2).trim()
            val path = line.substring(3).trim()
            ChangedFile(
                path = path,
                status = code,
            )
        }
    }

    private fun groupByType(files: List<ChangedFile>): Map<String, List<ChangedFile>> {
        return files.groupBy { file ->
            when {
                file.path.endsWith(".kt") || file.path.endsWith(".java") -> "code"
                file.path.endsWith(".xml") -> "resources"
                file.path.contains("build.gradle") || file.path.contains("build.gradle.kts") -> "build"
                file.path.endsWith(".md") || file.path.endsWith(".txt") -> "docs"
                file.path.contains("test") || file.path.contains("Test") -> "test"
                else -> "other"
            }
        }
    }

    private fun determinePrefix(groups: Map<String, List<ChangedFile>>): String {
        val hasNewFiles = groups.values.flatten().any { it.status == "A" }
        val hasDeletedFiles = groups.values.flatten().any { it.status == "D" }
        val hasModifiedFiles = groups.values.flatten().any { it.status == "M" }

        return when {
            hasNewFiles && !hasModifiedFiles && !hasDeletedFiles -> "feat"
            hasDeletedFiles && !hasModifiedFiles -> "chore"
            groups.containsKey("test") && groups.size == 1 -> "test"
            groups.containsKey("docs") && groups.size == 1 -> "docs"
            groups.containsKey("build") && groups.size == 1 -> "build"
            hasModifiedFiles -> "fix"
            else -> "chore"
        }
    }

    private fun determineScope(files: List<ChangedFile>): String? {
        val packages = files.map { file ->
            val parts = file.path.split("/")
            if (parts.size >= 3) parts[2] else null
        }.filterNotNull().distinct()

        return when {
            packages.size == 1 -> packages[0]
            packages.size <= 3 -> packages.joinToString(",")
            else -> null
        }
    }

    private fun generateDescription(groups: Map<String, List<ChangedFile>>, diff: String): String {
        val fileCount = groups.values.flatten().size
        val codeCount = groups["code"]?.size ?: 0
        val testCount = groups["test"]?.size ?: 0
        val buildCount = groups["build"]?.size ?: 0

        return when {
            fileCount == 1 -> {
                val file = groups.values.flatten().first()
                val name = file.path.substringAfterLast("/").substringBefore(".")
                "update $name"
            }
            codeCount > 0 && testCount > 0 -> "add tests and update $codeCount code file(s)"
            codeCount > 0 -> "update $codeCount file(s)"
            buildCount > 0 -> "update build configuration"
            else -> "update $fileCount file(s)"
        }
    }

    data class ChangedFile(
        val path: String,
        val status: String,
    )
}
