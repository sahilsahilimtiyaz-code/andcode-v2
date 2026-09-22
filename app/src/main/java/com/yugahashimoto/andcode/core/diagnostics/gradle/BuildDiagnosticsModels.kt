package com.yugahashimoto.andcode.core.diagnostics.gradle

enum class BuildErrorKind {
    COMPILE,
    LINT,
    DEPENDENCY,
    RESOURCE,
    TEST,
    TIMEOUT,
    UNKNOWN,
}

enum class BuildSeverity {
    ERROR,
    WARNING,
    INFO,
}

data class BuildError(
    val kind: BuildErrorKind,
    val severity: BuildSeverity,
    val message: String,
    val filePath: String? = null,
    val line: Int? = null,
    val column: Int? = null,
    val rule: String? = null,
    val rawLine: String = "",
)

data class BuildFix(
    val errorIndex: Int,
    val description: String,
    val suggestion: String,
    val confidence: Double = 0.5,
)

data class BuildDiagnosticReport(
    val errors: List<BuildError> = emptyList(),
    val fixes: List<BuildFix> = emptyList(),
    val summary: String = "",
    val durationMillis: Long = 0L,
    val totalErrors: Int = errors.count { it.severity == BuildSeverity.ERROR },
    val totalWarnings: Int = errors.count { it.severity == BuildSeverity.WARNING },
    val isSuccessful: Boolean = totalErrors == 0,
)
