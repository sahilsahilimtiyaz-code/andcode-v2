package com.yugahashimoto.andcode.core.diagnostics.gradle

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GradleOutputParserTest {

    @Test
    fun `parses compile error with file line column`() {
        val output = "e: /src/Main.kt:10:5 error: Unresolved reference: foo"
        val errors = GradleOutputParser.parse(output)
        assertEquals(1, errors.size)
        val error = errors[0]
        assertEquals(BuildErrorKind.COMPILE, error.kind)
        assertEquals(BuildSeverity.ERROR, error.severity)
        assertEquals("/src/Main.kt", error.filePath)
        assertEquals(10, error.line)
        assertEquals(5, error.column)
        assertTrue(error.message.contains("Unresolved reference"))
    }

    @Test
    fun `parses warning`() {
        val output = "w: /src/Util.kt:3:1 warning: Deprecated function"
        val errors = GradleOutputParser.parse(output)
        assertEquals(1, errors.size)
        assertEquals(BuildSeverity.WARNING, errors[0].severity)
    }

    @Test
    fun `parses dependency resolution failure`() {
        val output = "Could not resolve com.example:lib:1.0.\nRequired by: project :app"
        val errors = GradleOutputParser.parse(output)
        assertTrue(errors.any { it.kind == BuildErrorKind.DEPENDENCY })
    }

    @Test
    fun `parses test failure`() {
        val output = "FAILED: com.example.MyTest.testMethod"
        val errors = GradleOutputParser.parse(output)
        assertTrue(errors.any { it.kind == BuildErrorKind.TEST })
    }

    @Test
    fun `parses build failure message`() {
        val output = "FAILURE: Build failed with an exception.\n\n* What went wrong:\nExecution failed for task ':app:compileDebugKotlin'."
        val errors = GradleOutputParser.parse(output)
        assertTrue(errors.isNotEmpty())
        assertTrue(errors.any { it.severity == BuildSeverity.ERROR })
    }

    @Test
    fun `empty output produces no errors`() {
        val errors = GradleOutputParser.parse("")
        assertEquals(0, errors.size)
    }

    @Test
    fun `parses multiple errors`() {
        val output = "e: /src/A.kt:1:1 error: Type mismatch\n\ne: /src/B.kt:2:3 error: Unresolved reference"
        val errors = GradleOutputParser.parse(output)
        assertTrue(errors.size >= 2)
    }

    @Test
    fun `parses timeout`() {
        val output = "Build timed out after 300000 ms"
        val errors = GradleOutputParser.parse(output)
        assertTrue(errors.any { it.kind == BuildErrorKind.TIMEOUT })
    }
}
