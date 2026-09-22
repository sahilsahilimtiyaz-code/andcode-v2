package com.yugahashimoto.andcode.core.diagnostics.gradle

class BuildDoctor(
    private val parser: GradleOutputParser = GradleOutputParser,
    private val fixGenerator: BuildFixGenerator = BuildFixGenerator(),
) {
    fun diagnose(buildOutput: String, durationMillis: Long = 0L): BuildDiagnosticReport {
        val errors = parser.parse(buildOutput)
        val fixes = fixGenerator.generate(errors)

        val errorCount = errors.count { it.severity == BuildSeverity.ERROR }
        val warningCount = errors.count { it.severity == BuildSeverity.WARNING }

        val summary = buildString {
            if (errorCount == 0 && warningCount == 0) {
                append("Build successful with no issues.")
            } else {
                if (errorCount > 0) {
                    append("$errorCount error(s)")
                }
                if (warningCount > 0) {
                    if (isNotEmpty()) append(", ")
                    append("$warningCount warning(s)")
                }
                append(". ")

                val byKind = errors.groupBy { it.kind }
                for ((kind, kindErrors) in byKind) {
                    append("${kind.name}: ${kindErrors.size}. ")
                }
            }
        }

        return BuildDiagnosticReport(
            errors = errors,
            fixes = fixes,
            summary = summary,
            durationMillis = durationMillis,
            totalErrors = errorCount,
            totalWarnings = warningCount,
            isSuccessful = errorCount == 0,
        )
    }

    fun prioritize(fixes: List<BuildFix>): List<BuildFix> {
        return fixes.sortedByDescending { it.confidence }
    }

    fun topFixes(report: BuildDiagnosticReport, count: Int = 3): List<BuildFix> {
        return prioritize(report.fixes).take(count)
    }
}
