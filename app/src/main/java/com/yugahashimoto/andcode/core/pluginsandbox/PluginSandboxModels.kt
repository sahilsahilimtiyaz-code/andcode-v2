package com.yugahashimoto.andcode.core.pluginsandbox

data class Plugin(
    val id: String,
    val name: String,
    val version: String,
    val description: String = "",
    val author: String = "",
    val type: PluginType,
    val config: PluginConfig = PluginConfig(),
    val status: PluginStatus = PluginStatus.INSTALLED,
    val installedAt: Long = System.currentTimeMillis(),
    val lastRunAt: Long? = null,
    val runCount: Int = 0,
)

enum class PluginType { MCP_SERVER, CLI_TOOL, GRADLE_PLUGIN, CUSTOM }
enum class PluginStatus { INSTALLED, RUNNING, STOPPED, ERROR, DISABLED }

data class PluginConfig(
    val command: String = "",
    val args: List<String> = emptyList(),
    val envVars: Map<String, String> = emptyMap(),
    val workingDir: String? = null,
    val timeoutSeconds: Int = 60,
    val maxRetries: Int = 0,
    val autoRestart: Boolean = false,
    val port: Int? = null,
    val transport: TransportType = TransportType.STDIO,
)

enum class TransportType { STDIO, SSE, HTTP }

data class PluginRunRequest(
    val pluginId: String,
    val input: String = "",
    val envOverrides: Map<String, String> = emptyMap(),
    val timeoutSeconds: Int? = null,
)

data class PluginRunResult(
    val pluginId: String,
    val startedAt: Long,
    val finishedAt: Long,
    val status: PluginRunStatus,
    val output: String = "",
    val error: String? = null,
    val exitCode: Int? = null,
    val logs: List<PluginLogEntry> = emptyMap(),
) {
    val durationMillis: Long get() = finishedAt - startedAt
}

enum class PluginRunStatus { SUCCESS, FAILED, TIMEOUT, CANCELLED }

data class PluginLogEntry(
    val timestampMillis: Long = System.currentTimeMillis(),
    val level: LogLevel,
    val message: String,
)

enum class LogLevel { DEBUG, INFO, WARN, ERROR }

data class PluginHealth(
    val pluginId: String,
    val isHealthy: Boolean,
    val lastCheckAt: Long = System.currentTimeMillis(),
    val latencyMillis: Long = 0,
    val error: String? = null,
    val version: String = "",
    val capabilities: List<String> = emptyList(),
)

data class SandboxState(
    val plugins: List<Plugin> = emptyList(),
    val runningPlugins: Set<String> = emptySet(),
    val healthMap: Map<String, PluginHealth> = emptyMap(),
    val lastRunResults: Map<String, PluginRunResult> = emptyMap(),
)
