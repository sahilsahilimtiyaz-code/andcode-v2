package com.yugahashimoto.andcode.core.perfmon

data class SystemMetrics(
    val timestampMillis: Long = System.currentTimeMillis(),
    val cpu: CpuMetrics = CpuMetrics(),
    val memory: MemoryMetrics = MemoryMetrics(),
    val disk: DiskMetrics = DiskMetrics(),
    val runtime: RuntimeMetrics = RuntimeMetrics(),
)

data class CpuMetrics(
    val usagePercent: Double = 0.0,
    val coreCount: Int = 0,
    val loadAverage1m: Double = 0.0,
    val loadAverage5m: Double = 0.0,
    val loadAverage15m: Double = 0.0,
)

data class MemoryMetrics(
    val totalBytes: Long = 0,
    val usedBytes: Long = 0,
    val freeBytes: Long = 0,
    val usagePercent: Double = 0.0,
    val swapTotalBytes: Long = 0,
    val swapUsedBytes: Long = 0,
    val jvmHeapUsedBytes: Long = 0,
    val jvmHeapMaxBytes: Long = 0,
    val jvmNonHeapUsedBytes: Long = 0,
)

data class DiskMetrics(
    val totalBytes: Long = 0,
    val usedBytes: Long = 0,
    val freeBytes: Long = 0,
    val usagePercent: Double = 0.0,
    val readBytesTotal: Long = 0,
    val writeBytesTotal: Long = 0,
)

data class RuntimeMetrics(
    val pid: Int = 0,
    val uptimeMillis: Long = 0,
    val threadCount: Int = 0,
    val openFileDescriptors: Int = 0,
    val maxFileDescriptors: Int = 0,
    val gcCount: Long = 0,
    val gcPauseMillis: Long = 0,
    val restartCount: Int = 0,
    val lastExitCode: Int = 0,
)

data class PerfAlert(
    val id: String,
    val type: AlertType,
    val severity: AlertSeverity,
    val message: String,
    val metric: String,
    val value: Double,
    val threshold: Double,
    val timestampMillis: Long = System.currentTimeMillis(),
    val acknowledged: Boolean = false,
)

enum class AlertType {
    CPU_HIGH,
    MEMORY_HIGH,
    DISK_FULL,
    GC_PRESSURE,
    THREAD_LEAK,
    FD_LEAK,
    PROCESS_CRASH,
}

enum class AlertSeverity { WARNING, CRITICAL }

data class PerfSnapshot(
    val metrics: SystemMetrics,
    val alerts: List<PerfAlert> = emptyList(),
    val trend: PerformanceTrend = PerformanceTrend.STABLE,
)

enum class PerformanceTrend { IMPROVING, STABLE, DEGRADING }

data class ThresholdConfig(
    val cpuWarningPercent: Double = 80.0,
    val cpuCriticalPercent: Double = 95.0,
    val memoryWarningPercent: Double = 85.0,
    val memoryCriticalPercent: Double = 95.0,
    val diskWarningPercent: Double = 90.0,
    val diskCriticalPercent: Double = 98.0,
    val gcPauseWarningMillis: Long = 100,
    val threadCountWarning: Int = 200,
    val fdWarningPercent: Double = 80.0,
)

data class PerfMonitorState(
    val currentSnapshot: PerfSnapshot? = null,
    val history: List<SystemMetrics> = emptyList(),
    val activeAlerts: List<PerfAlert> = emptyList(),
    val isMonitoring: Boolean = false,
    val config: ThresholdConfig = ThresholdConfig(),
)
