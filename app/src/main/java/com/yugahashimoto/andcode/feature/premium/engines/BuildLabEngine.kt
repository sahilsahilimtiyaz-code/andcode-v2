package com.yugahashimoto.andcode.feature.premium.engines

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class BuildConfig(
    val id: String,
    val name: String,
    val type: String,
    val isRunning: Boolean = false,
    val success: Boolean? = null,
    val progress: Float = 0f,
    val output: List<String> = emptyList(),
    val duration: Long = 0
)

data class BuildLabState(
    val builds: List<BuildConfig> = emptyList(),
    val activeBuildId: String? = null,
    val lastBuildResult: BuildConfig? = null
)

class BuildLabEngine {
    private val _state = MutableStateFlow(BuildLabState())
    val state: StateFlow<BuildLabState> = _state.asStateFlow()

    init {
        _state.value = BuildLabState(
            builds = listOf(
                BuildConfig("debug", "Debug Build", "debug"),
                BuildConfig("release", "Release Build", "release"),
                BuildConfig("profile", "Profile Build", "profile")
            )
        )
    }

    fun startBuild(buildId: String) {
        val builds = _state.value.builds.map {
            if (it.id == buildId) it.copy(isRunning = true, progress = 0f) else it
        }
        _state.value = _state.value.copy(builds = builds, activeBuildId = buildId)
    }

    fun updateProgress(buildId: String, progress: Float, output: String = "") {
        val builds = _state.value.builds.map {
            if (it.id == buildId) {
                val newOutput = if (output.isNotBlank()) it.output + output else it.output
                it.copy(progress = progress, output = newOutput)
            } else it
        }
        _state.value = _state.value.copy(builds = builds)
    }

    fun completeBuild(buildId: String, success: Boolean, duration: Long = 0) {
        val builds = _state.value.builds.map {
            if (it.id == buildId) it.copy(
                isRunning = false,
                success = success,
                progress = if (success) 1f else it.progress,
                duration = duration
            ) else it
        }
        val completed = builds.find { it.id == buildId }
        _state.value = _state.value.copy(
            builds = builds,
            activeBuildId = null,
            lastBuildResult = completed
        )
    }

    fun cleanBuild() {
        _state.value = _state.value.copy(
            builds = _state.value.builds.map { it.copy(output = emptyList(), duration = 0) }
        )
    }

    fun buildApk(variant: String = "release") {
        startBuild(variant)
    }
}
