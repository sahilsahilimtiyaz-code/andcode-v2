package com.yugahashimoto.andcode.feature.premium.engines

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ProviderHealth(
    val providerId: String,
    val name: String,
    val latencyMs: Long = 0,
    val isHealthy: Boolean = true,
    val lastChecked: Long = System.currentTimeMillis(),
    val errorRate: Float = 0f
)

data class PerfMonitorState(
    val cpuUsage: Float = 0f,
    val memoryUsage: Float = 0f,
    val storageUsage: Float = 0f,
    val temperature: Float = 25f,
    val fps: Int = 60,
    val batteryLevel: Int = 100,
    val providers: List<ProviderHealth> = emptyList(),
    val isMonitoring: Boolean = false
)

class PerfMonitorEngine {
    private val _state = MutableStateFlow(PerfMonitorState())
    val state: StateFlow<PerfMonitorState> = _state.asStateFlow()

    fun startMonitoring() {
        _state.value = _state.value.copy(isMonitoring = true)
    }

    fun stopMonitoring() {
        _state.value = _state.value.copy(isMonitoring = false)
    }

    fun updateMetrics(
        cpu: Float = _state.value.cpuUsage,
        memory: Float = _state.value.memoryUsage,
        storage: Float = _state.value.storageUsage,
        temp: Float = _state.value.temperature,
        fps: Int = _state.value.fps,
        battery: Int = _state.value.batteryLevel
    ) {
        _state.value = _state.value.copy(
            cpuUsage = cpu.coerceIn(0f, 1f),
            memoryUsage = memory.coerceIn(0f, 1f),
            storageUsage = storage.coerceIn(0f, 1f),
            temperature = temp,
            fps = fps,
            batteryLevel = battery
        )
    }

    fun updateProviderHealth(providers: List<ProviderHealth>) {
        _state.value = _state.value.copy(providers = providers)
    }

    fun getStatusSummary(): String {
        val state = _state.value
        return buildString {
            append("CPU: ${(state.cpuUsage * 100).toInt()}% | ")
            append("MEM: ${(state.memoryUsage * 100).toInt()}% | ")
            append("FPS: ${state.fps} | ")
            append("Temp: ${state.temperature.toInt()}°C")
        }
    }
}
