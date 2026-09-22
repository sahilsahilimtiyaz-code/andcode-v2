package com.yugahashimoto.andcode.feature.premium.engines

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class GradleError(
    val line: Int,
    val message: String,
    val severity: String = "error",
    val suggestion: String = ""
)

data class GradleOutputState(
    val errors: List<GradleError> = emptyList(),
    val output: List<String> = emptyList(),
    val isParsing: Boolean = false
)

class GradleOutputParser {
    private val _state = MutableStateFlow(GradleOutputState())
    val state: StateFlow<GradleOutputState> = _state.asStateFlow()

    fun parseOutput(output: String) {
        _state.value = _state.value.copy(isParsing = true)
        val lines = output.lines()
        val errors = mutableListOf<GradleError>()

        for ((index, line) in lines.withIndex()) {
            if (line.contains("error:", ignoreCase = true)) {
                errors.add(
                    GradleError(
                        line = index + 1,
                        message = line.trim(),
                        severity = "error",
                        suggestion = suggestFix(line)
                    )
                )
            } else if (line.contains("warning:", ignoreCase = true)) {
                errors.add(
                    GradleError(
                        line = index + 1,
                        message = line.trim(),
                        severity = "warning",
                        suggestion = suggestFix(line)
                    )
                )
            }
        }

        _state.value = _state.value.copy(
            errors = errors,
            output = lines,
            isParsing = false
        )
    }

    private fun suggestFix(errorLine: String): String {
        return when {
            errorLine.contains("Unresolved reference") -> "Check imports and ensure the dependency is in build.gradle"
            errorLine.contains("Type mismatch") -> "Verify variable types match expected types"
            errorLine.contains("Cannot find symbol") -> "Import the missing class or fix the typo"
            errorLine.contains("Could not resolve") -> "Check internet connection and repository configuration"
            errorLine.contains("Build failed") -> "Review the error above and fix the root cause"
            else -> "Review the error and consult documentation"
        }
    }

    fun clear() {
        _state.value = GradleOutputState()
    }
}
