package com.yugahashimoto.andcode.core.diagnostics.gradle

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BuildFixGeneratorTest {

    private val generator = BuildFixGenerator()

    @Test
    fun `generates fix for unresolved reference`() {
        val errors = listOf(
            BuildError(
                kind = BuildErrorKind.COMPILE,
                severity = BuildSeverity.ERROR,
                message = "Unresolved reference: UserService",
                filePath = "Main.kt",
                line = 10,
            )
        )
        val fixes = generator.generate(errors)
        assertEquals(1, fixes.size)
        assertTrue(fixes[0].description.contains("Unresolved reference"))
        assertTrue(fixes[0].suggestion.contains("import"))
    }

    @Test
    fun `generates fix for type mismatch`() {
        val errors = listOf(
            BuildError(kind = BuildErrorKind.COMPILE, severity = BuildSeverity.ERROR, message = "Type mismatch: inferred String but Int expected")
        )
        val fixes = generator.generate(errors)
        assertTrue(fixes[0].suggestion.contains("cast") || fixes[0].suggestion.contains("type"))
    }

    @Test
    fun `generates fix for dependency error`() {
        val errors = listOf(
            BuildError(kind = BuildErrorKind.DEPENDENCY, severity = BuildSeverity.ERROR, message = "Could not resolve com.example:lib:1.0")
        )
        val fixes = generator.generate(errors)
        assertTrue(fixes[0].suggestion.contains("repository") || fixes[0].suggestion.contains("refresh"))
    }

    @Test
    fun `generates fix for test failure`() {
        val errors = listOf(
            BuildError(kind = BuildErrorKind.TEST, severity = BuildSeverity.ERROR, message = "Test failed: myTest")
        )
        val fixes = generator.generate(errors)
        assertEquals(1, fixes.size)
        assertTrue(fixes[0].suggestion.contains("test"))
    }

    @Test
    fun `generates fix for timeout`() {
        val errors = listOf(
            BuildError(kind = BuildErrorKind.TIMEOUT, severity = BuildSeverity.ERROR, message = "Build timed out after 300000 ms")
        )
        val fixes = generator.generate(errors)
        assertTrue(fixes[0].suggestion.contains("timeout") || fixes[0].suggestion.contains("loop"))
    }

    @Test
    fun `confidence varies by error kind`() {
        val errors = listOf(
            BuildError(kind = BuildErrorKind.COMPILE, severity = BuildSeverity.ERROR, message = "Unresolved reference: foo"),
            BuildError(kind = BuildErrorKind.UNKNOWN, severity = BuildSeverity.ERROR, message = "Something weird happened"),
        )
        val fixes = generator.generate(errors)
        assertTrue(fixes[0].confidence > fixes[1].confidence)
    }
}
