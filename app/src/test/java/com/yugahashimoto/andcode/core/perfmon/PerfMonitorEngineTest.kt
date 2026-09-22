package com.yugahashimoto.andcode.core.perfmon

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class PerfMonitorEngineTest {

    private lateinit var engine: PerfMonitorEngine

    private fun metricsWith(
        cpuPercent: Double = 50.0,
        memPercent: Double = 60.0,
        diskPercent: Double = 40.0,
        gcPause: Long = 10,
        threadCount: Int = 50,
        fdCount: Int = 100,
        maxFd: Int = 1024,
    ) = SystemMetrics(
        cpu = CpuMetrics(usagePercent = cpuPercent),
        memory = MemoryMetrics(usagePercent = memPercent),
        disk = DiskMetrics(usagePercent = diskPercent),
        runtime = RuntimeMetrics(
            gcPauseMillis = gcPause,
            threadCount = threadCount,
            openFileDescriptors = fdCount,
            maxFileDescriptors = maxFd,
        ),
    )

    @Before
    fun setUp() {
        engine = PerfMonitorEngine(
            metricsCollector = { metricsWith() },
            config = ThresholdConfig(),
        )
    }

    @Test
    fun `collectMetrics returns snapshot`() = runBlocking {
        val snapshot = engine.collectMetrics()
        assertNotNull(snapshot.metrics)
        assertEquals(50.0, snapshot.metrics.cpu.usagePercent, 0.01)
    }

    @Test
    fun `collectMetrics adds to history`() = runBlocking {
        engine.collectMetrics()
        engine.collectMetrics()
        assertEquals(2, engine.getHistory().size)
    }

    @Test
    fun `collectMetrics caps history at MAX_HISTORY`() = runBlocking {
        repeat(PerfMonitorEngine.MAX_HISTORY + 5) {
            engine.collectMetrics()
        }
        assertEquals(PerfMonitorEngine.MAX_HISTORY, engine.getHistory().size)
    }

    @Test
    fun `cpu alert generated above warning threshold`() = runBlocking {
        val monitor = PerfMonitorEngine(
            metricsCollector = { metricsWith(cpuPercent = 85.0) },
            config = ThresholdConfig(cpuWarningPercent = 80.0, cpuCriticalPercent = 95.0),
        )
        val snapshot = monitor.collectMetrics()
        assertEquals(1, snapshot.alerts.size)
        assertEquals(AlertType.CPU_HIGH, snapshot.alerts.first().type)
        assertEquals(AlertSeverity.WARNING, snapshot.alerts.first().severity)
    }

    @Test
    fun `cpu alert generated above critical threshold`() = runBlocking {
        val monitor = PerfMonitorEngine(
            metricsCollector = { metricsWith(cpuPercent = 96.0) },
            config = ThresholdConfig(cpuWarningPercent = 80.0, cpuCriticalPercent = 95.0),
        )
        val snapshot = monitor.collectMetrics()
        assertEquals(AlertSeverity.CRITICAL, snapshot.alerts.first().severity)
    }

    @Test
    fun `memory alert generated above warning threshold`() = runBlocking {
        val monitor = PerfMonitorEngine(
            metricsCollector = { metricsWith(memPercent = 86.0) },
            config = ThresholdConfig(memoryWarningPercent = 85.0),
        )
        val snapshot = monitor.collectMetrics()
        assertTrue(snapshot.alerts.any { it.type == AlertType.MEMORY_HIGH })
    }

    @Test
    fun `disk alert generated above warning threshold`() = runBlocking {
        val monitor = PerfMonitorEngine(
            metricsCollector = { metricsWith(diskPercent = 91.0) },
            config = ThresholdConfig(diskWarningPercent = 90.0),
        )
        val snapshot = monitor.collectMetrics()
        assertTrue(snapshot.alerts.any { it.type == AlertType.DISK_FULL })
    }

    @Test
    fun `gc alert generated when pause exceeds threshold`() = runBlocking {
        val monitor = PerfMonitorEngine(
            metricsCollector = { metricsWith(gcPause = 150) },
            config = ThresholdConfig(gcPauseWarningMillis = 100),
        )
        val snapshot = monitor.collectMetrics()
        assertTrue(snapshot.alerts.any { it.type == AlertType.GC_PRESSURE })
    }

    @Test
    fun `thread alert generated when count exceeds threshold`() = runBlocking {
        val monitor = PerfMonitorEngine(
            metricsCollector = { metricsWith(threadCount = 250) },
            config = ThresholdConfig(threadCountWarning = 200),
        )
        val snapshot = monitor.collectMetrics()
        assertTrue(snapshot.alerts.any { it.type == AlertType.THREAD_LEAK })
    }

    @Test
    fun `fd alert generated when usage exceeds threshold`() = runBlocking {
        val monitor = PerfMonitorEngine(
            metricsCollector = { metricsWith(fdCount = 900, maxFd = 1024) },
            config = ThresholdConfig(fdWarningPercent = 80.0),
        )
        val snapshot = monitor.collectMetrics()
        assertTrue(snapshot.alerts.any { it.type == AlertType.FD_LEAK })
    }

    @Test
    fun `no alerts when all metrics normal`() = runBlocking {
        val snapshot = engine.collectMetrics()
        assertTrue(snapshot.alerts.isEmpty())
    }

    @Test
    fun `acknowledgeAlert marks alert as acknowledged`() = runBlocking {
        val snapshot = engine.collectMetrics()
        assertTrue(snapshot.alerts.isEmpty())
        val monitor = PerfMonitorEngine(
            metricsCollector = { metricsWith(cpuPercent = 90.0) },
            config = ThresholdConfig(cpuWarningPercent = 80.0),
        )
        val snap2 = monitor.collectMetrics()
        val alertId = snap2.alerts.first().id
        monitor.acknowledgeAlert(alertId)
        assertTrue(monitor.getActiveAlerts().isEmpty())
        assertEquals(1, monitor.getAllAlerts().size)
    }

    @Test
    fun `clearAlerts empties alert list`() = runBlocking {
        val monitor = PerfMonitorEngine(
            metricsCollector = { metricsWith(cpuPercent = 90.0) },
            config = ThresholdConfig(cpuWarningPercent = 80.0),
        )
        monitor.collectMetrics()
        assertTrue(monitor.getActiveAlerts().isNotEmpty())
        monitor.clearAlerts()
        assertTrue(monitor.getActiveAlerts().isEmpty())
    }

    @Test
    fun `computeTrend returns STABLE with less than 3 samples`() = runBlocking {
        engine.collectMetrics()
        assertEquals(PerformanceTrend.STABLE, engine.computeTrend())
    }

    @Test
    fun `computeTrend detects DEGRADING trend`() = runBlocking {
        val degrading = PerfMonitorEngine(
            metricsCollector = {
                metricsWith(cpuPercent = 20.0 + (engine.getHistory().size * 15.0))
            },
        )
        degrading.collectMetrics()
        degrading.collectMetrics()
        degrading.collectMetrics()
        assertEquals(PerformanceTrend.DEGRADING, degrading.computeTrend())
    }

    @Test
    fun `clearHistory empties history`() = runBlocking {
        engine.collectMetrics()
        engine.collectMetrics()
        engine.clearHistory()
        assertTrue(engine.getHistory().isEmpty())
    }
}
