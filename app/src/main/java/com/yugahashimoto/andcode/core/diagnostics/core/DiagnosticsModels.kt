package com.yugahashimoto.andcode.core.diagnostics.core

data class HealthCheck(
    val id: String,
    val name: String,
    val category: HealthCategory,
    val status: HealthStatus,
    val message: String = "",
    val details: Map<String, String> = emptyMap(),
    val durationMillis: Long = 0,
    val timestampMillis: Long = System.currentTimeMillis(),
    val suggestion: String? = null,
)

enum class HealthCategory {
    RUNTIME,
    NETWORK,
    STORAGE,
    PROCESS,
    CONFIGURATION,
    DEPENDENCY,
}

enum class HealthStatus {
    HEALTHY,
    DEGRADED,
    UNHEALTHY,
    UNKNOWN,
}

data class DiagnosticReport(
    val timestampMillis: Long = System.currentTimeMillis(),
    val checks: List<HealthCheck> = emptyList(),
    val overallStatus: HealthStatus = HealthStatus.UNKNOWN,
    val summary: String = "",
    val recommendations: List<Recommendation> = emptyList(),
) {
    val healthyCount: Int get() = checks.count { it.status == HealthStatus.HEALTHY }
    val degradedCount: Int get() = checks.count { it.status == HealthStatus.DEGRADED }
    val unhealthyCount: Int get() = checks.count { it.status == HealthStatus.UNHEALTHY }
}

data class Recommendation(
    val id: String,
    val priority: RecommendationPriority,
    val category: HealthCategory,
    val title: String,
    val description: String,
    val action: String? = null,
    val autoFixAvailable: Boolean = false,
)

enum class RecommendationPriority { LOW, MEDIUM, HIGH, CRITICAL }

data class SystemInfo(
    val osName: String = "",
    val osVersion: String = "",
    val arch: String = "",
    val javaVersion: String = "",
    val kotlinVersion: String = "",
    val gradleVersion: String = "",
    val androidSdkVersion: String = "",
    val deviceModel: String = "",
    val deviceManufacturer: String = "",
    val totalRamBytes: Long = 0,
    val availableRamBytes: Long = 0,
    val totalStorageBytes: Long = 0,
    val availableStorageBytes: Long = 0,
    val networkType: String = "",
    val isConnected: Boolean = false,
)

data class DiagnosticsState(
    val lastReport: DiagnosticReport? = null,
    val reportHistory: List<DiagnosticReport> = emptyList(),
    val systemInfo: SystemInfo = SystemInfo(),
    val isRunning: Boolean = false,
    val lastRunAt: Long? = null,
)
