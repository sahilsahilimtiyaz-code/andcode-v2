package com.yugahashimoto.andcode.core.diagnostics.gradle

object GradleOutputParser {

    private val compileErrorPattern = Regex(
        """e:\s*(.+?):(\d+):(\d+)\s*(?:error:\s*)?(.+)""",
    )
    private val warningPattern = Regex(
        """w:\s*(.+?):(\d+):(\d+)\s*(?:warning:\s*)?(.+)""",
    )
    private val fileLinePattern = Regex(
        """>> (.+?):(\d+):(.+)""",
    )
    private val dependencyPattern = Regex(
        """Could not (?:resolve|find) (?:artifact|dependency) (.+?)[\s\n]""",
        setOf(RegexOption.DOT_MATCHES_ALL),
    )
    private val resourcePattern = Regex(
        """resource (?:method|field|class) (\w+) not found""",
    )
    private val testFailurePattern = Regex(
        """(?:FAILED|FAILURE).*?Test (.+?)\s""",
        setOf(RegexOption.DOT_MATCHES_ALL),
    )
    private val timeoutPattern = Regex(
        """(?i)(?:timeout|timed out).*?(\d+)\s*(?:ms|seconds)""",
    )
    private val buildFailedPattern = Regex(
        """FAILURE: Build failed with an exception""",
    )
    private val taskFailedPattern = Regex(
        """\* What went wrong:?\s*(.+?)(?:\n\n|\n\*|$)""",
        setOf(RegexOption.DOT_MATCHES_ALL),
    )

    fun parse(output: String): List<BuildError> {
        val errors = mutableListOf<BuildError>()

        for (line in output.lines()) {
            compileErrorPattern.find(line)?.let { match ->
                errors.add(
                    BuildError(
                        kind = BuildErrorKind.COMPILE,
                        severity = BuildSeverity.ERROR,
                        message = match.groupValues[4].trim(),
                        filePath = match.groupValues[1],
                        line = match.groupValues[2].toIntOrNull(),
                        column = match.groupValues[3].toIntOrNull(),
                        rawLine = line,
                    )
                )
            }

            warningPattern.find(line)?.let { match ->
                errors.add(
                    BuildError(
                        kind = BuildErrorKind.COMPILE,
                        severity = BuildSeverity.WARNING,
                        message = match.groupValues[4].trim(),
                        filePath = match.groupValues[1],
                        line = match.groupValues[2].toIntOrNull(),
                        column = match.groupValues[3].toIntOrNull(),
                        rawLine = line,
                    )
                )
            }

            dependencyPattern.find(line)?.let { match ->
                errors.add(
                    BuildError(
                        kind = BuildErrorKind.DEPENDENCY,
                        severity = BuildSeverity.ERROR,
                        message = "Could not resolve: ${match.groupValues[1]}",
                        rawLine = line,
                    )
                )
            }

            resourcePattern.find(line)?.let { match ->
                errors.add(
                    BuildError(
                        kind = BuildErrorKind.RESOURCE,
                        severity = BuildSeverity.ERROR,
                        message = "Resource not found: ${match.groupValues[1]}",
                        rawLine = line,
                    )
                )
            }

            testFailurePattern.find(line)?.let { match ->
                errors.add(
                    BuildError(
                        kind = BuildErrorKind.TEST,
                        severity = BuildSeverity.ERROR,
                        message = "Test failed: ${match.groupValues[1]}",
                        rawLine = line,
                    )
                )
            }

            timeoutPattern.find(line)?.let { match ->
                errors.add(
                    BuildError(
                        kind = BuildErrorKind.TIMEOUT,
                        severity = BuildSeverity.ERROR,
                        message = "Build timed out after ${match.groupValues[1]}",
                        rawLine = line,
                    )
                )
            }
        }

        if (buildFailedPattern.containsMatchIn(output)) {
            taskFailedPattern.find(output)?.let { match ->
                val message = match.groupValues[1].trim()
                if (errors.none { it.message == message }) {
                    errors.add(
                        BuildError(
                            kind = BuildErrorKind.UNKNOWN,
                            severity = BuildSeverity.ERROR,
                            message = message,
                            rawLine = message,
                        )
                    )
                }
            }
        }

        return errors
    }
}
