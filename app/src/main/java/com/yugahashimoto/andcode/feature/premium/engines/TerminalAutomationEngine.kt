package com.yugahashimoto.andcode.feature.premium.engines

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class TerminalCommand(
    val id: String,
    val command: String,
    val output: String = "",
    val exitCode: Int? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val isRunning: Boolean = false
)

data class TerminalState(
    val commands: List<TerminalCommand> = emptyList(),
    val currentCommand: String = "",
    val isExecuting: Boolean = false,
    val secretMaskingEnabled: Boolean = true
)

class TerminalAutomationEngine {
    private val _state = MutableStateFlow(TerminalState())
    val state: StateFlow<TerminalState> = _state.asStateFlow()

    private val sensitivePatterns = listOf(
        Regex("password=\\S+", RegexOption.IGNORE_CASE),
        Regex("token=\\S+", RegexOption.IGNORE_CASE),
        Regex("key=\\S+", RegexOption.IGNORE_CASE),
        Regex("secret=\\S+", RegexOption.IGNORE_CASE),
        Regex("api[_-]?key=\\S+", RegexOption.IGNORE_CASE)
    )

    fun executeCommand(command: String) {
        val maskedCommand = if (_state.value.secretMaskingEnabled) maskSecrets(command) else command
        val cmd = TerminalCommand(
            id = "cmd_${System.currentTimeMillis()}",
            command = maskedCommand,
            isRunning = true
        )
        _state.value = _state.value.copy(
            commands = _state.value.commands + cmd,
            isExecuting = true
        )
    }

    fun completeCommand(commandId: String, output: String, exitCode: Int) {
        val commands = _state.value.commands.map {
            if (it.id == commandId) it.copy(
                output = if (_state.value.secretMaskingEnabled) maskSecrets(output) else output,
                exitCode = exitCode,
                isRunning = false
            ) else it
        }
        _state.value = _state.value.copy(commands = commands, isExecuting = false)
    }

    fun setCurrentCommand(command: String) {
        _state.value = _state.value.copy(currentCommand = command)
    }

    fun toggleSecretMasking() {
        _state.value = _state.value.copy(secretMaskingEnabled = !_state.value.secretMaskingEnabled)
    }

    fun clearHistory() {
        _state.value = _state.value.copy(commands = emptyList())
    }

    private fun maskSecrets(text: String): String {
        var masked = text
        for (pattern in sensitivePatterns) {
            masked = pattern.replace(masked) { match ->
                val value = match.value
                val eqIndex = value.indexOf('=')
                if (eqIndex != -1) {
                    "${value.substring(0, eqIndex + 1)}***MASKED***"
                } else {
                    "***MASKED***"
                }
            }
        }
        return masked
    }
}
