package com.yugahashimoto.andcode.core.diagnostics.core

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DiagnosticsEngineTest {

    private lateinit var engine: DiagnosticsEngine

    @Before
    fun setUp() {
        engine = DiagnosticsEngine { cmd, dir, timeout ->
            when {
                cmd.contains("which opencode") -> 0 to "/usr/bin/opencode"
                cmd.contains("which claude") -> 1 to "none"
                cmd.contains("df -h") -> 0 to "/dev/sda1  50G  20G  28G  42% /"
                cmd.contains("curl") -> 0 to "200"
                cmd.contains("free -m") -> 0 to "Mem:   8192   4096   4096"
                cmd.contains("uname -s") -> 0 to "Linux"
                cmd.contains("uname -r") -> 0 to "6.1.0"
                cmd.contains("uname -m") -> 0 to "x86_64"
                cmd.contains("java -version") -> 0 to "openjdk version \"17.0.2\" 2022-10-18"
                cmd.contains("git --version") -> 0 to "git version 2.40.0"
                cmd.contains("which gradle") -> 0 to "/usr/bin/gradle"
                cmd.contains("df -B1") -> 0 to "53687091200  21474836480  30064771072  42% /"
                cmd.contains("grep MemTotal") -> 0 to "8388608"
                else -> 0 to ""
            }
        }
    }

    @Test
    fun `runDiagnostics returns report with checks`() = runBlocking {
        val report = engine.runDiagnostics()
        assertTrue(report.checks.isNotEmpty())
        assertNotNull(report.overallStatus)
        assertNotNull(report.summary)
    }

    @Test
    fun `runDiagnostics detects healthy system`() = runBlocking {
        val report = engine.runDiagnostics()
        assertEquals(HealthStatus.HEALTHY, report.overallStatus)
    }

    @Test
    fun `runDiagnostics detects unhealthy system`() = runBlocking {
        val unhealthyEngine = DiagnosticsEngine { cmd, dir, timeout ->
            when {
                cmd.contains("df -h") -> 0 to "/dev/sda1  50G  49G  1G  98% /"
                cmd.contains("free -m") -> 0 to "Mem:   8192   7900   292"
                cmd.contains("curl") -> 1 to "000"
                cmd.contains("git --version") -> 1 to ""
                else -> 0 to ""
            }
        }
        val report = unhealthyEngine.runDiagnostics()
        assertEquals(HealthStatus.UNHEALTHY, report.overallStatus)
        assertTrue(report.unhealthyCount > 0)
    }

    @Test
    fun `runDiagnostics detects degraded system`() = runBlocking {
        val degradedEngine = DiagnosticsEngine { cmd, dir, timeout ->
            when {
                cmd.contains("which opencode") -> 1 to "none"
                cmd.contains("df -h") -> 0 to "/dev/sda1  50G  46G  4G  92% /"
                cmd.contains("free -m") -> 0 to "Mem:   8192   7100   1092"
                cmd.contains("curl") -> 0 to "200"
                cmd.contains("git --version") -> 0 to "git version 2.40.0"
                else -> 0 to ""
            }
        }
        val report = degradedEngine.runDiagnostics()
        assertEquals(HealthStatus.DEGRADED, report.overallStatus)
        assertTrue(report.degradedCount > 0)
    }

    @Test
    fun `runDiagnostics caps history at MAX_REPORTS`() = runBlocking {
        repeat(DiagnosticsEngine.MAX_REPORTS + 5) {
            engine.runDiagnostics()
        }
        assertEquals(DiagnosticsEngine.MAX_REPORTS, engine.getReportHistory().size)
    }

    @Test
    fun `getLastReport returns most recent`() = runBlocking {
        engine.runDiagnostics()
        val report2 = engine.runDiagnostics()
        val last = engine.getLastReport()
        assertNotNull(last)
        assertEquals(report2.timestampMillis, last!!.timestampMillis)
    }

    @Test
    fun `recommendations generated for unhealthy checks`() = runBlocking {
        val unhealthyEngine = DiagnosticsEngine { cmd, dir, timeout ->
            when {
                cmd.contains("which opencode") -> 1 to "none"
                cmd.contains("df -h") -> 0 to "/dev/sda1  50G  49G  1G  98% /"
                else -> 0 to ""
            }
        }
        val report = unhealthyEngine.runDiagnostics()
        assertTrue(report.recommendations.isNotEmpty())
    }

    @Test
    fun `getSystemInfo returns system details`() = runBlocking {
        val info = engine.getSystemInfo()
        assertEquals("Linux", info.osName)
        assertEquals("6.1.0", info.osVersion)
        assertEquals("x86_64", info.arch)
        assertTrue(info.javaVersion.contains("openjdk"))
        assertTrue(info.totalStorageBytes > 0)
        assertTrue(info.totalRamBytes > 0)
    }

    @Test
    fun `healthCheck records duration`() = runBlocking {
        val report = engine.runDiagnostics()
        for (check in report.checks) {
            assertTrue(check.durationMillis >= 0)
        }
    }

    @Test
    fun `healthCheck has suggestions for unhealthy status`() = runBlocking {
        val unhealthyEngine = DiagnosticsEngine { cmd, dir, timeout ->
            when {
                cmd.contains("df -h") -> 0 to "/dev/sda1  50G  49G  1G  98% /"
                else -> 0 to ""
            }
        }
        val report = unhealthyEngine.runDiagnostics()
        val unhealthyChecks = report.checks.filter { it.status == HealthStatus.UNHEALTHY }
        assertTrue(unhealthyChecks.any { it.suggestion != null })
    }
}
