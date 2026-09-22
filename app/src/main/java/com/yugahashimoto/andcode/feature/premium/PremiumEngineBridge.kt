package com.yugahashimoto.andcode.feature.premium

import com.yugahashimoto.andcode.core.context.ContextAssembler
import com.yugahashimoto.andcode.core.context.TokenEstimator
import com.yugahashimoto.andcode.core.git.CommitMessageGenerator
import com.yugahashimoto.andcode.core.git.GitHistoryAnalyzer
import com.yugahashimoto.andcode.core.mission.CheckpointManager
import com.yugahashimoto.andcode.core.mission.StreamResumeManager
import com.yugahashimoto.andcode.core.reliability.ProcessSupervisor
import com.yugahashimoto.andcode.core.reliability.RecoveryManager
import com.yugahashimoto.andcode.core.verify.VerifyPipeline
import com.yugahashimoto.andcode.feature.premium.engines.MissionEngine
import com.yugahashimoto.andcode.runtime.OpenCodeBackend
import java.io.File

object PremiumEngineBridge {
    private var _streamResumeManager: StreamResumeManager? = null
    private var _processSupervisor: ProcessSupervisor? = null
    private var _recoveryManager: RecoveryManager? = null
    private var _verifyPipeline: VerifyPipeline? = null
    private var _gitHistoryAnalyzer: GitHistoryAnalyzer? = null
    private var _commitMessageGenerator: CommitMessageGenerator? = null
    private var _buildDoctor: com.yugahashimoto.andcode.core.diagnostics.gradle.BuildDoctor? = null
    private var _checkpointManager: CheckpointManager? = null

    fun bind(
        streamResumeManager: StreamResumeManager,
        processSupervisor: ProcessSupervisor,
        recoveryManager: RecoveryManager,
    ) {
        _streamResumeManager = streamResumeManager
        _processSupervisor = processSupervisor
        _recoveryManager = recoveryManager
    }

    fun bindVerifyPipeline(pipeline: VerifyPipeline) { _verifyPipeline = pipeline }
    fun bindGitTools(analyzer: GitHistoryAnalyzer, generator: CommitMessageGenerator) {
        _gitHistoryAnalyzer = analyzer
        _commitMessageGenerator = generator
    }
    fun bindBuildDoctor(doctor: com.yugahashimoto.andcode.core.diagnostics.gradle.BuildDoctor) {
        _buildDoctor = doctor
    }
    fun bindCheckpointManager(manager: CheckpointManager) { _checkpointManager = manager }

    fun streamResumeManager(): StreamResumeManager =
        _streamResumeManager ?: StreamResumeManager()

    fun processSupervisor(): ProcessSupervisor? = _processSupervisor
    fun recoveryManager(): RecoveryManager? = _recoveryManager
    fun verifyPipeline(): VerifyPipeline? = _verifyPipeline
    fun gitHistoryAnalyzer(): GitHistoryAnalyzer? = _gitHistoryAnalyzer
    fun commitMessageGenerator(): CommitMessageGenerator? = _commitMessageGenerator
    fun buildDoctor(): com.yugahashimoto.andcode.core.diagnostics.gradle.BuildDoctor? = _buildDoctor
    fun checkpointManager(): CheckpointManager? = _checkpointManager

    fun createCheckpointManager(workspaceDir: File): CheckpointManager {
        return _checkpointManager ?: CheckpointManager(
            runtimeDirectory = workspaceDir,
            commandRunner = createSimpleCommandRunner(),
        )
    }

    fun createMissionEngine(backend: OpenCodeBackend? = null): MissionEngine {
        val orchestrator = com.yugahashimoto.andcode.core.mission.AgentOrchestrator()
        return MissionEngine(backend = backend, orchestrator = orchestrator)
    }

    fun estimateTokens(text: String): Int = TokenEstimator.estimate(text)
    fun assembleContext(request: com.yugahashimoto.andcode.core.context.ContextRequest) =
        ContextAssembler().assemble(request)

    private fun createSimpleCommandRunner(): com.yugahashimoto.andcode.runtime.local.LocalRuntimeCommandRunner {
        val runtimeDir = File(System.getProperty("java.io.tmpdir"), "andcode-runtime")
        runtimeDir.mkdirs()
        return com.yugahashimoto.andcode.runtime.local.LocalRuntimeCommandRunner(
            runtimeDirectory = runtimeDir,
            installedRuntimeProvider = { null },
        )
    }
}
