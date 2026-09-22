package com.yugahashimoto.andcode.core.perfmon

class PerfMonitorEngine(
    private val metricsCollector: suspend () -> SystemMetrics,
    private val config: ThresholdConfig = ThresholdConfig(),
) {
    private val history = mutableListOf<SystemMetrics>()
    private val alerts = mutableListOf<PerfAlert>()
    private var alertIdCounter = 0

    fun getHistory(): List<SystemMetrics> = history.toList()

    fun getActiveAlerts(): List<PerfAlert> = alerts.filter { !it.acknowledged }

    fun getAllAlerts(): List<PerfAlert> = alerts.toList()

    fun acknowledgeAlert(alertId: String) {
        val index = alerts.indexOfFirst { it.id == alertId }
        if (index >= 0) {
            alerts[index] = alerts[index].copy(acknowledged = true)
        }
    }

    fun clearAlerts() {
        alerts.clear()
    }

    fun clearHistory() {
        history.clear()
    }

    suspend fun collectMetrics(): PerfSnapshot {
        val metrics = metricsCollector()
        history.add(metrics)
        if (history.size > MAX_HISTORY) {
            history.removeAt(0)
        }

        val newAlerts = evaluateThresholds(metrics)
        alerts.addAll(newAlerts)

        val trend = computeTrend()

        val snapshot = PerfSnapshot(
            metrics = metrics,
            alerts = newAlerts,
            trend = trend,
        )
        return snapshot
    }

    fun computeTrend(): PerformanceTrend {
        if (history.size < 3) return PerformanceTrend.STABLE
        val recent = history.takeLast(3)
        val cpuTrend = recent.last().cpu.usagePercent - recent.first().cpu.usagePercent
        val memTrend = recent.last().memory.usagePercent - recent.first().memory.usagePercent
        val combined = cpuTrend + memTrend
        return when {
            combined > 5.0 -> PerformanceTrend.DEGRADING
            combined < -5.0 -> PerformanceTrend.IMPROVING
            else -> PerformanceTrend.STABLE
        }
    }

    private fun evaluateThresholds(metrics: SystemMetrics): List<PerfAlert> {
        val result = mutableListOf<PerfAlert>()

        if (metrics.cpu.usagePercent >= config.cpuCriticalPercent) {
            result.add(createAlert(AlertType.CPU_HIGH, AlertSeverity.CRITICAL,
                "CPU at ${metrics.cpu.usagePercent}%", "cpu_usage", metrics.cpu.usagePercent, config.cpuCriticalPercent))
        } else if (metrics.cpu.usagePercent >= config.cpuWarningPercent) {
            result.add(createAlert(AlertType.CPU_HIGH, AlertSeverity.WARNING,
                "CPU at ${metrics.cpu.usagePercent}%", "cpu_usage", metrics.cpu.usagePercent, config.cpuWarningPercent))
        }

        if (metrics.memory.usagePercent >= config.memoryCriticalPercent) {
            result.add(createAlert(AlertType.MEMORY_HIGH, AlertSeverity.CRITICAL,
                "Memory at ${metrics.memory.usagePercent}%", "memory_usage", metrics.memory.usagePercent, config.memoryCriticalPercent))
        } else if (metrics.memory.usagePercent >= config.memoryWarningPercent) {
            result.add(createAlert(AlertType.MEMORY_HIGH, AlertSeverity.WARNING,
                "Memory at ${metrics.memory.usagePercent}%", "memory_usage", metrics.memory.usagePercent, config.memoryWarningPercent))
        }

        if (metrics.disk.usagePercent >= config.diskCriticalPercent) {
            result.add(createAlert(AlertType.DISK_FULL, AlertSeverity.CRITICAL,
                "Disk at ${metrics.disk.usagePercent}%", "disk_usage", metrics.disk.usagePercent, config.diskCriticalPercent))
        } else if (metrics.disk.usagePercent >= config.diskWarningPercent) {
            result.add(createAlert(AlertType.DISK_FULL, AlertSeverity.WARNING,
                "Disk at ${metrics.disk.usagePercent}%", "disk_usage", metrics.disk.usagePercent, config.diskWarningPercent))
        }

        if (metrics.runtime.gcPauseMillis >= config.gcPauseWarningMillis) {
            result.add(createAlert(AlertType.GC_PRESSURE, AlertSeverity.WARNING,
                "GC pause ${metrics.runtime.gcPauseMillis}ms", "gc_pause", metrics.runtime.gcPauseMillis.toDouble(), config.gcPauseWarningMillis.toDouble()))
        }

        if (metrics.runtime.threadCount >= config.threadCountWarning) {
            result.add(createAlert(AlertType.THREAD_LEAK, AlertSeverity.WARNING,
                "${metrics.runtime.threadCount} threads", "thread_count", metrics.runtime.threadCount.toDouble(), config.threadCountWarning.toDouble()))
        }

        if (metrics.runtime.maxFileDescriptors > 0) {
            val fdPercent = metrics.runtime.openFileDescriptors.toDouble() / metrics.runtime.maxFileDescriptors * 100
            if (fdPercent >= config.fdWarningPercent) {
                result.add(createAlert(AlertType.FD_LEAK, AlertSeverity.WARNING,
                    "FD usage at ${fdPercent.toInt()}%", "fd_usage", fdPercent, config.fdWarningPercent))
            }
        }

        return result
    }

    private fun createAlert(
        type: AlertType,
        severity: AlertSeverity,
        message: String,
        metric: String,
        value: Double,
        threshold: Double,
    ): PerfAlert {
        alertIdCounter++
        return PerfAlert(
            id = "alert_$alertIdCounter",
            type = type,
            severity = severity,
            message = message,
            metric = metric,
            value = value,
            threshold = threshold,
        )
    }

    companion object {
        const val MAX_HISTORY = 100
    }
}
