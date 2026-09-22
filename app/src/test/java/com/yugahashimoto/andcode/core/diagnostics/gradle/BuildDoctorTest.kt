package com.yugahashimoto.andcode.core.diagnostics.gradle

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BuildDoctorTest {

    private val doctor = BuildDoctor()

    @Test
    fun `diagnose successful build`() {
        val report = doctor.diagnose("BUILD SUCCESSFUL in 10s")
        assertTrue(report.isSuccessful)
        assertEquals(0, report.totalErrors)
    }

    @Test
    fun `diagnose build with errors`() {
        val output = "e: /src/Main.kt:10:5 error: Unresolved reference: foo"
        val report = doctor.diagnose(output)
        assertFalse(report.isSuccessful)
        assertEquals(1, report.totalErrors)
        assertTrue(report.fixes.isNotEmpty())
    }

    @Test
    fun `diagnose provides summary`() {
        val output = "e: /src/A.kt:1:1 error: Type mismatch\nw: /src/B.kt:2:2 warning: Unused import"
        val report = doctor.diagnose(output)
        assertTrue(report.summary.contains("1 error"))
        assertTrue(report.summary.contains("1 warning"))
    }

    @Test
    fun `topFixes returns limited results`() {
        val output = "e: /src/A.kt:1:1 error: Type mismatch\ne: /src/B.kt:2:2 error: Unresolved reference\ne: /src/C.kt:3:3 error: Missing return"
        val report = doctor.diagnose(output)
        val topFixes = doctor.topFixes(report, count = 2)
        assertEquals(2, topFixes.size)
    }

    @Test
    fun `prioritize sorts by confidence`() {
        val fixes = listOf(
            BuildFix(0, "Low", "Suggestion", 0.2),
            BuildFix(1, "High", "Suggestion", 0.9),
            BuildFix(2, "Medium", "Suggestion", 0.5),
        )
        val prioritized = doctor.prioritize(fixes)
        assertEquals(0.9, prioritized[0].confidence, 0.001)
        assertEquals(0.5, prioritized[1].confidence, 0.001)
        assertEquals(0.2, prioritized[2].confidence, 0.001)
    }

    @Test
    fun `diagnose tracks duration`() {
        val report = doctor.diagnose("BUILD SUCCESSFUL", durationMillis = 5000)
        assertEquals(5000L, report.durationMillis)
    }
}
