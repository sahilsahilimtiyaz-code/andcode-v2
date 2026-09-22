package com.yugahashimoto.andcode.core.buildlab

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class BuildLabEngineTest {

    private lateinit var engine: BuildLabEngine
    private val commandLog = mutableListOf<Pair<List<String>, Long>>()

    @Before
    fun setUp() {
        commandLog.clear()
        engine = BuildLabEngine { command, timeout ->
            commandLog.add(command to timeout)
            0 to "BUILD SUCCESSFUL\n42 actionable tasks: 10 executed, 32 up-to-date\n"
        }
    }

    @Test
    fun `executeBuild runs assembleDebug by default`() = runBlocking {
        val request = BuildRequest(projectId = "app")
        val result = engine.executeBuild(request)
        assertEquals(BuildStatus.SUCCESS, result.status)
        assertTrue(commandLog.isNotEmpty())
        val args = commandLog.first().first
        assertTrue(args.contains("./gradlew"))
        assertTrue(args.any { it.contains("assembleDebug", ignoreCase = true) })
    }

    @Test
    fun `executeBuild adds clean task when cleanBuild is true`() = runBlocking {
        val request = BuildRequest(projectId = "app", cleanBuild = true)
        engine.executeBuild(request)
        val args = commandLog.first().first
        assertTrue(args.contains("clean"))
    }

    @Test
    fun `executeBuild uses custom tasks when provided`() = runBlocking {
        val request = BuildRequest(
            projectId = "app",
            tasks = listOf("testDebugUnitTest", "lintDebug"),
        )
        engine.executeBuild(request)
        val args = commandLog.first().first
        assertTrue(args.contains("testDebugUnitTest"))
        assertTrue(args.contains("lintDebug"))
    }

    @Test
    fun `executeBuild appends extra args`() = runBlocking {
        val request = BuildRequest(
            projectId = "app",
            extraArgs = listOf("--info", "-PmyProp=value"),
        )
        engine.executeBuild(request)
        val args = commandLog.first().first
        assertTrue(args.contains("--info"))
        assertTrue(args.contains("-PmyProp=value"))
    }

    @Test
    fun `executeBuild uses profile gradle args`() = runBlocking {
        val profile = BuildProfile(
            id = "p1", name = "Fast", variant = "release",
            gradleArgs = listOf("--build-cache", "--parallel"),
        )
        engine.addProfile(profile)
        val request = BuildRequest(projectId = "app")
        engine.executeBuild(request, profile)
        val args = commandLog.first().first
        assertTrue(args.contains("--build-cache"))
        assertTrue(args.contains("--parallel"))
    }

    @Test
    fun `executeBuild adds jvm args when profile has them`() = runBlocking {
        val profile = BuildProfile(
            id = "p1", name = "BigHeap", variant = "debug",
            jvmArgs = listOf("-Xmx4g", "-XX:+UseG1GC"),
        )
        engine.addProfile(profile)
        val request = BuildRequest(projectId = "app")
        engine.executeBuild(request, profile)
        val args = commandLog.first().first
        assertTrue(args.any { it.contains("Xmx4g") })
    }

    @Test
    fun `executeBuild records in history`() = runBlocking {
        val request = BuildRequest(projectId = "app")
        val result = engine.executeBuild(request)
        val history = engine.getHistory()
        assertEquals(1, history.size)
        assertEquals(result.status, history.first().result.status)
    }

    @Test
    fun `executeBuild caps history at MAX_HISTORY`() = runBlocking {
        repeat(BuildLabEngine.MAX_HISTORY + 5) { i ->
            engine.executeBuild(BuildRequest(projectId = "app$i"))
        }
        assertEquals(BuildLabEngine.MAX_HISTORY, engine.getHistory().size)
    }

    @Test
    fun `cancelBuild returns false when no active build`() {
        assertFalse(engine.cancelBuild())
    }

    @Test
    fun `clearHistory empties history`() = runBlocking {
        engine.executeBuild(BuildRequest(projectId = "app"))
        assertEquals(1, engine.getHistory().size)
        engine.clearHistory()
        assertEquals(0, engine.getHistory().size)
    }

    @Test
    fun `addProfile stores and retrieves profiles`() {
        val profile = BuildProfile(id = "p1", name = "Release", variant = "release")
        engine.addProfile(profile)
        assertEquals(1, engine.getProfiles().size)
        assertEquals("Release", engine.getProfiles().first().name)
    }

    @Test
    fun `addProfile replaces existing with same id`() {
        engine.addProfile(BuildProfile(id = "p1", name = "V1", variant = "debug"))
        engine.addProfile(BuildProfile(id = "p1", name = "V2", variant = "release"))
        assertEquals(1, engine.getProfiles().size)
        assertEquals("V2", engine.getProfiles().first().name)
    }

    @Test
    fun `removeProfile removes by id`() {
        engine.addProfile(BuildProfile(id = "p1", name = "P1", variant = "debug"))
        engine.addProfile(BuildProfile(id = "p2", name = "P2", variant = "release"))
        engine.removeProfile("p1")
        assertEquals(1, engine.getProfiles().size)
        assertEquals("p2", engine.getProfiles().first().id)
    }

    @Test
    fun `getActiveBuild returns null when idle`() {
        assertNull(engine.getActiveBuild())
    }
}
