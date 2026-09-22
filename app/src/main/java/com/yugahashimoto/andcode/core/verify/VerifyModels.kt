package com.yugahashimoto.andcode.core.verify

enum class VerifyStep(val displayName: String) {
    LINT("Lint"),
    FORMAT("Format"),
    TEST("Test"),
    BUILD("Build"),
}

enum class VerifySeverity { ERROR, WARNING, INFO }

data class LintViolation(
    val file: String,
    val line: Int,
    val column: Int = 0,
    val rule: String,
    val message: String,
    val severity: VerifySeverity,
)

data class TestResult(
    val name: String,
    val className: String,
    val passed: Boolean,
    val durationMillis: Long = 0L,
    val errorMessage: String? = null,
    val stackTrace: String? = null,
)

data class StepResult(
    val step: VerifyStep,
    val success: Boolean,
    val output: String = "",
    val durationMillis: Long = 0L,
    val violations: List<LintViolation> = emptyList(),
    val testResults: List<TestResult> = emptyList(),
    val error: String? = null,
)

data class VerifyReport(
    val results: List<StepResult> = emptyList(),
) {
    val totalViolations: Int get() = results.sumOf { it.violations.size }
    val totalTestFailures: Int get() = results.sumOf { r -> r.testResults.count { !it.passed } }
    val totalTests: Int get() = results.sumOf { it.testResults.size }
    val isSuccessful: Boolean get() = results.all { it.success }
    val durationMillis: Long get() = results.sumOf { it.durationMillis }
}
