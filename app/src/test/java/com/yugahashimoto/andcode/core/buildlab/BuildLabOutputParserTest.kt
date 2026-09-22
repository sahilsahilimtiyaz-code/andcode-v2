package com.yugahashimoto.andcode.core.buildlab

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BuildLabOutputParserTest {

    private fun engineWith(output: String, exitCode: Int = 0) = BuildLabEngine { _, _ ->
        exitCode to output
    }

    @Test
    fun `successful build is detected`() = runBlocking {
        val engine = engineWith("BUILD SUCCESSFUL in 12s\n42 actionable tasks: 10 executed")
        val result = engine.executeBuild(BuildRequest(projectId = "app"))
        assertEquals(BuildStatus.SUCCESS, result.status)
    }

    @Test
    fun `failed build with non-zero exit code`() = runBlocking {
        val engine = engineWith("BUILD FAILED in 5s", exitCode = 1)
        val result = engine.executeBuild(BuildRequest(projectId = "app"))
        assertEquals(BuildStatus.FAILED, result.status)
    }

    @Test
    fun `failed build with zero exit but no success string`() = runBlocking {
        val engine = engineWith("Some output without success marker", exitCode = 0)
        val result = engine.executeBuild(BuildRequest(projectId = "app"))
        assertEquals(BuildStatus.FAILED, result.status)
    }

    @Test
    fun `errors are parsed from FAILURE lines`() = runBlocking {
        val engine = engineWith("FAILURE: Build failed with an exception.\n* What went wrong: compilation error", exitCode = 1)
        val result = engine.executeBuild(BuildRequest(projectId = "app"))
        assertTrue(result.errors.isNotEmpty())
        assertTrue(result.errors.any { it.message.contains("Build failed") })
    }

    @Test
    fun `file errors with line numbers are parsed`() = runBlocking {
        val output = "app/src/main/Foo.kt:42:10: error: Unresolved reference 'bar'"
        val engine = engineWith(output, exitCode = 1)
        val result = engine.executeBuild(BuildRequest(projectId = "app"))
        val fileError = result.errors.find { it.file != null }
        assertEquals(true, fileError != null)
        assertEquals("app/src/main/Foo.kt", fileError!!.file)
        assertEquals(42, fileError.line)
        assertEquals(10, fileError.column)
        assertEquals(ErrorSeverity.ERROR, fileError.severity)
    }

    @Test
    fun `task lines are counted`() = runBlocking {
        val output = "> Task :app:compileDebugKotlin UP-TO-DATE\n> Task :app:assembleDebug\nBUILD SUCCESSFUL"
        val engine = engineWith(output)
        val result = engine.executeBuild(BuildRequest(projectId = "app"))
        assertTrue(result.cacheStats.tasksTotal >= 2)
    }

    @Test
    fun `up-to-date tasks are counted in cache stats`() = runBlocking {
        val output = "> Task :app:compileDebugKotlin UP-TO-DATE\n> Task :app:assembleDebug\nBUILD SUCCESSFUL"
        val engine = engineWith(output)
        val result = engine.executeBuild(BuildRequest(projectId = "app"))
        assertTrue(result.cacheStats.tasksUpToDate >= 1)
    }

    @Test
    fun `kotlin error classified as KOTLIN kind`() = runBlocking {
        val engine = engineWith("error: Unresolved reference 'foo'", exitCode = 1)
        val result = engine.executeBuild(BuildRequest(projectId = "app"))
        assertEquals(ErrorKind.KOTLIN, result.errors.first().kind)
    }

    @Test
    fun `dependency error classified as DEPENDENCY kind`() = runBlocking {
        val engine = engineWith("error: Could not resolve com.example:lib:1.0", exitCode = 1)
        val result = engine.executeBuild(BuildRequest(projectId = "app"))
        assertEquals(ErrorKind.DEPENDENCY, result.errors.first().kind)
    }

    @Test
    fun `AAPT2 error classified as RESOURCE kind`() = runBlocking {
        val engine = engineWith("error: AAPT2 daemon startup failed", exitCode = 1)
        val result = engine.executeBuild(BuildRequest(projectId = "app"))
        assertEquals(ErrorKind.RESOURCE, result.errors.first().kind)
    }
}
