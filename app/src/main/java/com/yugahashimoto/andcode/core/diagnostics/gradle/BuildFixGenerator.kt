package com.yugahashimoto.andcode.core.diagnostics.gradle

class BuildFixGenerator {

    fun generate(errors: List<BuildError>): List<BuildFix> {
        return errors.mapIndexed { index, error ->
            generateFix(index, error)
        }
    }

    private fun generateFix(index: Int, error: BuildError): BuildFix {
        return when (error.kind) {
            BuildErrorKind.COMPILE -> generateCompileFix(index, error)
            BuildErrorKind.DEPENDENCY -> generateDependencyFix(index, error)
            BuildErrorKind.RESOURCE -> generateResourceFix(index, error)
            BuildErrorKind.LINT -> generateLintFix(index, error)
            BuildErrorKind.TEST -> generateTestFix(index, error)
            BuildErrorKind.TIMEOUT -> BuildFix(
                errorIndex = index,
                description = "Build timed out",
                suggestion = "Increase timeout or reduce build scope. Check for infinite loops or heavy tasks.",
                confidence = 0.3,
            )
            BuildErrorKind.UNKNOWN -> BuildFix(
                errorIndex = index,
                description = "Unknown build error",
                suggestion = "Check the build output for more details.",
                confidence = 0.2,
            )
        }
    }

    private fun generateCompileFix(index: Int, error: BuildError): BuildFix {
        val msg = error.message.lowercase()

        return when {
            "unresolved reference" in msg -> {
                val reference = error.message.substringAfter("Unresolved reference: ").substringBefore(" ").trim()
                BuildFix(
                    errorIndex = index,
                    description = "Unresolved reference: $reference",
                    suggestion = "Check if '$reference' is imported correctly or exists in the current scope.",
                    confidence = 0.7,
                )
            }
            "type mismatch" in msg -> BuildFix(
                errorIndex = index,
                description = "Type mismatch",
                suggestion = "Check the expected type and add a cast or conversion if needed.",
                confidence = 0.5,
            )
            "cannot find" in msg || "not found" in msg -> {
                val name = error.message.substringAfter("Cannot find ").substringBefore(" ").trim()
                BuildFix(
                    errorIndex = index,
                    description = "Symbol not found: $name",
                    suggestion = "Add the missing import or check the symbol name for typos.",
                    confidence = 0.6,
                )
            }
            "override" in msg -> BuildFix(
                errorIndex = index,
                description = "Override mismatch",
                suggestion = "Ensure the overridden function signature matches the parent class/interface.",
                confidence = 0.5,
            )
            "visibility" in msg || "is private" in msg -> BuildFix(
                errorIndex = index,
                description = "Visibility issue",
                suggestion = "Change the visibility modifier (e.g., private -> public) or use an accessor.",
                confidence = 0.6,
            )
            else -> BuildFix(
                errorIndex = index,
                description = "Compile error: ${error.message.take(80)}",
                suggestion = "Review the code at line ${error.line ?: "?"} in ${error.filePath ?: "unknown file"}.",
                confidence = 0.3,
            )
        }
    }

    private fun generateDependencyFix(index: Int, error: BuildError): BuildFix {
        val msg = error.message.lowercase()
        return when {
            "could not resolve" in msg -> BuildFix(
                errorIndex = index,
                description = "Dependency resolution failed",
                suggestion = "Check repository URLs, network connectivity, and version catalog. Try: ./gradlew --refresh-dependencies",
                confidence = 0.4,
            )
            "could not find" in msg -> BuildFix(
                errorIndex = index,
                description = "Artifact not found",
                suggestion = "Verify the dependency coordinates (group:artifact:version) and ensure the repository is configured.",
                confidence = 0.4,
            )
            else -> BuildFix(
                errorIndex = index,
                description = "Dependency issue",
                suggestion = "Check build.gradle for dependency configuration errors.",
                confidence = 0.3,
            )
        }
    }

    private fun generateResourceFix(index: Int, error: BuildError): BuildFix {
        return BuildFix(
            errorIndex = index,
            description = "Resource not found",
            suggestion = "Check res/ directory for the missing resource. Ensure the resource name matches exactly.",
            confidence = 0.5,
        )
    }

    private fun generateLintFix(index: Int, error: BuildError): BuildFix {
        return BuildFix(
            errorIndex = index,
            description = "Lint violation: ${error.rule ?: "unknown"}",
            suggestion = "Follow the lint rule suggestion. Run: ./gradlew lint for details.",
            confidence = 0.4,
        )
    }

    private fun generateTestFix(index: Int, error: BuildError): BuildFix {
        return BuildFix(
            errorIndex = index,
            description = "Test failure",
            suggestion = "Review the test case, check assertions, and verify test data. Run the specific test for detailed output.",
            confidence = 0.3,
        )
    }
}
