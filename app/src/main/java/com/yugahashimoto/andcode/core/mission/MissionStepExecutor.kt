package com.yugahashimoto.andcode.core.mission

interface MissionStepExecutor {
    val step: MissionStep
    suspend fun execute(context: MissionContext): MissionStepResult
}
