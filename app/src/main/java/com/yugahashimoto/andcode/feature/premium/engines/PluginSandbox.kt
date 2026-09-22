package com.yugahashimoto.andcode.feature.premium.engines

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class Plugin(
    val id: String,
    val name: String,
    val description: String,
    val version: String,
    val isEnabled: Boolean = true,
    val author: String = ""
)

data class PluginSandboxState(
    val plugins: List<Plugin> = emptyList(),
    val selectedPluginId: String? = null
)

class PluginSandbox {
    private val _state = MutableStateFlow(PluginSandboxState())
    val state: StateFlow<PluginSandboxState> = _state.asStateFlow()

    init {
        _state.value = PluginSandboxState(
            plugins = listOf(
                Plugin("mcp-servers", "MCP Servers", "Model Context Protocol integrations", "1.0.0", true, "AndCode"),
                Plugin("git-enhanced", "Git Enhanced", "Advanced git operations", "2.1.0", true, "AndCode"),
                Plugin("code-analysis", "Code Analysis", "Static code analysis tools", "1.5.0", false, "Community"),
                Plugin("ai-assist", "AI Assistant", "Enhanced AI capabilities", "1.2.0", true, "AndCode")
            )
        )
    }

    fun togglePlugin(pluginId: String) {
        val plugins = _state.value.plugins.map {
            if (it.id == pluginId) it.copy(isEnabled = !it.isEnabled) else it
        }
        _state.value = _state.value.copy(plugins = plugins)
    }

    fun selectPlugin(pluginId: String) {
        _state.value = _state.value.copy(selectedPluginId = pluginId)
    }

    fun addPlugin(plugin: Plugin) {
        _state.value = _state.value.copy(plugins = _state.value.plugins + plugin)
    }

    fun removePlugin(pluginId: String) {
        _state.value = _state.value.copy(
            plugins = _state.value.plugins.filter { it.id != pluginId }
        )
    }
}
