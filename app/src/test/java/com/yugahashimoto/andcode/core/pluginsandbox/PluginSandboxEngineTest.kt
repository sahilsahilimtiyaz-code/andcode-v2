package com.yugahashimoto.andcode.core.pluginsandbox

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class PluginSandboxEngineTest {

    private lateinit var engine: PluginSandboxEngine

    @Before
    fun setUp() {
        engine = PluginSandboxEngine { _, _, _, _ ->
            0 to "ok"
        }
    }

    private fun testPlugin(
        id: String = "p1",
        name: String = "Test Plugin",
        type: PluginType = PluginType.MCP_SERVER,
        command: String = "echo",
        port: Int? = null,
    ) = Plugin(
        id = id,
        name = name,
        version = "1.0.0",
        type = type,
        config = PluginConfig(command = command, port = port),
    )

    @Test
    fun `getPlugins returns empty initially`() {
        assertTrue(engine.getPlugins().isEmpty())
    }

    @Test
    fun `installPlugin stores and retrieves`() {
        engine.installPlugin(testPlugin())
        assertEquals(1, engine.getPlugins().size)
        assertEquals("Test Plugin", engine.getPlugins().first().name)
    }

    @Test
    fun `installPlugin replaces existing with same id`() {
        engine.installPlugin(testPlugin(id = "p1", name = "V1"))
        engine.installPlugin(testPlugin(id = "p1", name = "V2"))
        assertEquals(1, engine.getPlugins().size)
        assertEquals("V2", engine.getPlugins().first().name)
    }

    @Test
    fun `uninstallPlugin removes plugin`() {
        engine.installPlugin(testPlugin(id = "p1"))
        engine.installPlugin(testPlugin(id = "p2"))
        engine.uninstallPlugin("p1")
        assertEquals(1, engine.getPlugins().size)
        assertEquals("p2", engine.getPlugins().first().id)
    }

    @Test
    fun `getPluginById returns correct plugin`() {
        engine.installPlugin(testPlugin(id = "p1", name = "Alpha"))
        engine.installPlugin(testPlugin(id = "p2", name = "Beta"))
        assertEquals("Alpha", engine.getPluginById("p1")!!.name)
        assertEquals("Beta", engine.getPluginById("p2")!!.name)
    }

    @Test
    fun `getPluginById returns null for unknown`() {
        assertNull(engine.getPluginById("nonexistent"))
    }

    @Test
    fun `setPluginStatus updates status`() {
        engine.installPlugin(testPlugin())
        engine.setPluginStatus("p1", PluginStatus.RUNNING)
        assertEquals(PluginStatus.RUNNING, engine.getPluginById("p1")!!.status)
    }

    @Test
    fun `runPlugin succeeds on clean command`() = runBlocking {
        engine.installPlugin(testPlugin())
        val result = engine.runPlugin(PluginRunRequest(pluginId = "p1"))
        assertEquals(PluginRunStatus.SUCCESS, result.status)
        assertEquals("ok", result.output)
    }

    @Test
    fun `runPlugin fails for unknown plugin`() = runBlocking {
        val result = engine.runPlugin(PluginRunRequest(pluginId = "nonexistent"))
        assertEquals(PluginRunStatus.FAILED, result.status)
        assertTrue(result.error!!.contains("not found"))
    }

    @Test
    fun `runPlugin fails on non-zero exit`() = runBlocking {
        val failingEngine = PluginSandboxEngine { _, _, _, _ -> 1 to "error" }
        failingEngine.installPlugin(testPlugin())
        val result = failingEngine.runPlugin(PluginRunRequest(pluginId = "p1"))
        assertEquals(PluginRunStatus.FAILED, result.status)
        assertEquals(1, result.exitCode)
    }

    @Test
    fun `runPlugin records last result`() = runBlocking {
        engine.installPlugin(testPlugin())
        engine.runPlugin(PluginRunRequest(pluginId = "p1"))
        assertNotNull(engine.getLastResult("p1"))
        assertEquals(PluginRunStatus.SUCCESS, engine.getLastResult("p1")!!.status)
    }

    @Test
    fun `runPlugin increments runCount`() = runBlocking {
        engine.installPlugin(testPlugin())
        engine.runPlugin(PluginRunRequest(pluginId = "p1"))
        engine.runPlugin(PluginRunRequest(pluginId = "p1"))
        assertEquals(2, engine.getPluginById("p1")!!.runCount)
    }

    @Test
    fun `runPlugin sets status to RUNNING then STOPPED`() = runBlocking {
        engine.installPlugin(testPlugin())
        engine.runPlugin(PluginRunRequest(pluginId = "p1"))
        assertEquals(PluginStatus.STOPPED, engine.getPluginById("p1")!!.status)
    }

    @Test
    fun `runPlugin sets status to ERROR on failure`() = runBlocking {
        val failingEngine = PluginSandboxEngine { _, _, _, _ -> 1 to "error" }
        failingEngine.installPlugin(testPlugin())
        failingEngine.runPlugin(PluginRunRequest(pluginId = "p1"))
        assertEquals(PluginStatus.ERROR, failingEngine.getPluginById("p1")!!.status)
    }

    @Test
    fun `runPlugin uses env overrides`() = runBlocking {
        val envLog = mutableListOf<Map<String, String>>()
        val engineWithLog = PluginSandboxEngine { _, env, _, _ ->
            envLog.add(env)
            0 to "ok"
        }
        engineWithLog.installPlugin(testPlugin())
        engineWithLog.runPlugin(PluginRunRequest(
            pluginId = "p1",
            envOverrides = mapOf("MY_VAR" to "test_value"),
        ))
        assertTrue(envLog.first().containsKey("MY_VAR"))
        assertEquals("test_value", envLog.first()["MY_VAR"])
    }

    @Test
    fun `checkHealth returns healthy for working plugin`() = runBlocking {
        engine.installPlugin(testPlugin(command = "echo"))
        val health = engine.checkHealth("p1")
        assertTrue(health.isHealthy)
        assertNotNull(health.version)
    }

    @Test
    fun `checkHealth returns unhealthy for unknown plugin`() = runBlocking {
        val health = engine.checkHealth("nonexistent")
        assertFalse(health.isHealthy)
        assertTrue(health.error!!.contains("not found"))
    }

    @Test
    fun `checkHealth returns unhealthy on command failure`() = runBlocking {
        val failingEngine = PluginSandboxEngine { _, _, _, _ -> 1 to "error" }
        failingEngine.installPlugin(testPlugin())
        val health = failingEngine.checkHealth("p1")
        assertFalse(health.isHealthy)
    }

    @Test
    fun `checkAllHealth checks all plugins`() = runBlocking {
        engine.installPlugin(testPlugin(id = "p1"))
        engine.installPlugin(testPlugin(id = "p2"))
        val healthMap = engine.checkAllHealth()
        assertEquals(2, healthMap.size)
        assertTrue(healthMap.containsKey("p1"))
        assertTrue(healthMap.containsKey("p2"))
    }

    @Test
    fun `getPluginsByType filters correctly`() {
        engine.installPlugin(testPlugin(id = "p1", type = PluginType.MCP_SERVER))
        engine.installPlugin(testPlugin(id = "p2", type = PluginType.CLI_TOOL))
        engine.installPlugin(testPlugin(id = "p3", type = PluginType.MCP_SERVER))
        assertEquals(2, engine.getPluginsByType(PluginType.MCP_SERVER).size)
        assertEquals(1, engine.getPluginsByType(PluginType.CLI_TOOL).size)
    }

    @Test
    fun `getRunningPlugins returns only running`() {
        engine.installPlugin(testPlugin(id = "p1"))
        engine.installPlugin(testPlugin(id = "p2"))
        engine.setPluginStatus("p1", PluginStatus.RUNNING)
        val running = engine.getRunningPlugins()
        assertEquals(1, running.size)
        assertEquals("p1", running.first().id)
    }
}
