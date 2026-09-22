package com.yugahashimoto.andcode.feature.premium.engines

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class DiagnosticEntry(
    val id: String,
    val timestamp: Long,
    val level: String,
    val category: String,
    val message: String,
    val details: String = ""
)

data class Anomaly(
    val id: String,
    val type: String,
    val description: String,
    val severity: String,
    val timestamp: Long
)

data class DiagnosticsState(
    val entries: List<DiagnosticEntry> = emptyList(),
    val anomalies: List<Anomaly> = emptyList(),
    val isCapturing: Boolean = false
)

class DiagnosticsEngine {
    private val _state = MutableStateFlow(DiagnosticsState())
    val state: StateFlow<DiagnosticsState> = _state.asStateFlow()

    fun startCapturing() {
        _state.value = _state.value.copy(isCapturing = true)
    }

    fun stopCapturing() {
        _state.value = _state.value.copy(isCapturing = false)
    }

    fun addEntry(level: String, category: String, message: String, details: String = "") {
        val entry = DiagnosticEntry(
            id = "log_${System.currentTimeMillis()}",
            timestamp = System.currentTimeMillis(),
            level = level,
            category = category,
            message = message,
            details = details
        )
        _state.value = _state.value.copy(entries = _state.value.entries + entry)
    }

    fun addAnomaly(type: String, description: String, severity: String) {
        val anomaly = Anomaly(
            id = "anomaly_${System.currentTimeMillis()}",
            type = type,
            description = description,
            severity = severity,
            timestamp = System.currentTimeMillis()
        )
        _state.value = _state.value.copy(anomalies = _state.value.anomalies + anomaly)
    }

    fun clearLogs() {
        _state.value = _state.value.copy(entries = emptyList())
    }

    fun clearAnomalies() {
        _state.value = _state.value.copy(anomalies = emptyList())
    }

    fun getEntriesByLevel(level: String): List<DiagnosticEntry> {
        return _state.value.entries.filter { it.level == level }
    }
}
