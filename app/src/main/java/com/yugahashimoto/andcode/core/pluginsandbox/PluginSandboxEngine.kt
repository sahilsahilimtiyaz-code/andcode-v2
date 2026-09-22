package com.yugahashimoto.andcode.core.pluginsandbox

class PluginSandboxEngine(
    private val commandRunner: suspend (List<String>, Map<String, String>, String?, Int) -> Pair<Int, String>,
) {
    private val plugins = mutableMapOf<String, Plugin>()
    private val healthMap = mutableMapOf<String, PluginHealth>()
    private val lastResults = mutableMapOf<String, PluginRunResult>()

    fun getPlugins(): List<Plugin> = plugins.values.toList()

    fun getPluginById(id: String): Plugin? = plugins[id]

    fun installPlugin(plugin: Plugin) {
        plugins[plugin.id] = plugin
    }

    fun uninstallPlugin(pluginId: String) {
        plugins.remove(pluginId)
        healthMap.remove(pluginId)
        lastResults.remove(pluginId)
    }

    fun setPluginStatus(pluginId: String, status: PluginStatus) {
        plugins[pluginId]?.let {
            plugins[pluginId] = it.copy(status = status)
        }
    }

    fun getHealth(pluginId: String): PluginHealth? = healthMap[pluginId]

    fun getLastResult(pluginId: String): PluginRunResult? = lastResults[pluginId]

    suspend fun runPlugin(request: PluginRunRequest): PluginRunResult {
        val plugin = plugins[request.pluginId]
            ?: return PluginRunResult(
                pluginId = request.pluginId,
                startedAt = System.currentTimeMillis(),
                finishedAt = System.currentTimeMillis(),
                status = PluginRunStatus.FAILED,
                error = "Plugin not found: ${request.pluginId}",
            )

        val startTime = System.currentTimeMillis()
        val config = plugin.config
        val effectiveTimeout = request.timeoutSeconds ?: config.timeoutSeconds
        val allEnv = config.envVars + request.envOverrides
        val command = buildList {
            add(config.command)
            addAll(config.args)
        }

        setPluginStatus(plugin.id, PluginStatus.RUNNING)

        return try {
            val (exitCode, output) = commandRunner(command, allEnv, config.workingDir, effectiveTimeout * 1000)
            val finishedAt = System.currentTimeMillis()
            val status = if (exitCode == 0) PluginRunStatus.SUCCESS else PluginRunStatus.FAILED
            val result = PluginRunResult(
                pluginId = plugin.id,
                startedAt = startTime,
                finishedAt = finishedAt,
                status = status,
                output = output,
                exitCode = exitCode,
            )
            lastResults[plugin.id] = result
            setPluginStatus(plugin.id, if (status == PluginRunStatus.SUCCESS) PluginStatus.STOPPED else PluginStatus.ERROR)
            plugins[plugin.id]?.let {
                plugins[plugin.id] = it.copy(
                    lastRunAt = startTime,
                    runCount = it.runCount + 1,
                )
            }
            result
        } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
            val result = PluginRunResult(
                pluginId = plugin.id,
                startedAt = startTime,
                finishedAt = System.currentTimeMillis(),
                status = PluginRunStatus.TIMEOUT,
                error = "Plugin timed out after ${effectiveTimeout}s",
            )
            lastResults[plugin.id] = result
            setPluginStatus(plugin.id, PluginStatus.ERROR)
            result
        } catch (e: Exception) {
            val result = PluginRunResult(
                pluginId = plugin.id,
                startedAt = startTime,
                finishedAt = System.currentTimeMillis(),
                status = PluginRunStatus.FAILED,
                error = e.message,
            )
            lastResults[plugin.id] = result
            setPluginStatus(plugin.id, PluginStatus.ERROR)
            result
        }
    }

    suspend fun checkHealth(pluginId: String): PluginHealth {
        val plugin = plugins[pluginId]
        if (plugin == null) {
            val health = PluginHealth(pluginId = pluginId, isHealthy = false, error = "Plugin not found")
            healthMap[pluginId] = health
            return health
        }

        val startCheck = System.currentTimeMillis()
        return try {
            val (exitCode, output) = commandRunner(
                listOf(plugin.config.command, "--version"),
                plugin.config.envVars,
                plugin.config.workingDir,
                10_000,
            )
            val latency = System.currentTimeMillis() - startCheck
            val health = PluginHealth(
                pluginId = pluginId,
                isHealthy = exitCode == 0,
                latencyMillis = latency,
                version = output.trim().take(100),
            )
            healthMap[pluginId] = health
            health
        } catch (e: Exception) {
            val health = PluginHealth(
                pluginId = pluginId,
                isHealthy = false,
                error = e.message,
            )
            healthMap[pluginId] = health
            health
        }
    }

    suspend fun checkAllHealth(): Map<String, PluginHealth> {
        for (plugin in plugins.values) {
            checkHealth(plugin.id)
        }
        return healthMap.toMap()
    }

    fun getPluginsByType(type: PluginType): List<Plugin> =
        plugins.values.filter { it.type == type }

    fun getRunningPlugins(): List<Plugin> =
        plugins.values.filter { it.status == PluginStatus.RUNNING }

    companion object {
        const val MAX_LOG_ENTRIES = 500
    }
}
