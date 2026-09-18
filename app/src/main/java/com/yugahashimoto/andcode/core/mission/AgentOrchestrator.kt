package com.yugahashimoto.andcode.core.mission

import com.yugahashimoto.andcode.core.reliability.ResilientExecutor
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

class AgentOrchestrator(
    private val resilientExecutor: ResilientExecutor = ResilientExecutor(),
    private val config: OrchestrationConfig = OrchestrationConfig(),
    private val executeTask: suspend (AgentTask) -> MissionStepResult = {
        MissionStepResult.Failed("No executor registered for role ${it.role}")
    },
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
) {
    private val mutableAssignments = MutableStateFlow<Map<String, AgentAssignment>>(emptyMap())
    val assignments: StateFlow<Map<String, AgentAssignment>> = mutableAssignments.asStateFlow()

    private val semaphore = Semaphore(config.maxConcurrency)

    suspend fun orchestrate(tasks: List<AgentTask>): Map<String, AgentAssignment> {
        val assignments = mutableMapOf<String, AgentAssignment>()
        val taskMap = tasks.associateBy { it.id }

        coroutineScope {
            val deferredResults = tasks.map { task ->
                async(Dispatchers.IO) {
                    executeWithDependencies(task, taskMap, assignments)
                }
            }
            deferredResults.awaitAll().forEach { (taskId, assignment) ->
                assignments[taskId] = assignment
            }
        }

        mutableAssignments.value = assignments
        return assignments
    }

    private suspend fun executeWithDependencies(
        task: AgentTask,
        taskMap: Map<String, AgentTask>,
        completedAssignments: MutableMap<String, AgentAssignment>,
    ): Pair<String, AgentAssignment> {
        for (depId in task.dependsOn) {
            val depAssignment = completedAssignments[depId]
            if (depAssignment != null && depAssignment.result is MissionStepResult.Failed) {
                return task.id to AgentAssignment(
                    task = task,
                    agentId = "blocked",
                    result = MissionStepResult.Failed("Dependency $depId failed"),
                    error = "Blocked by failed dependency",
                )
            }
        }

        if (config.failFast && completedAssignments.values.any { it.result is MissionStepResult.Failed }) {
            return task.id to AgentAssignment(
                task = task,
                agentId = "skipped",
                result = MissionStepResult.Skipped,
                error = "Skipped due to fail-fast",
            )
        }

        return semaphore.withPermit {
            val startTime = System.currentTimeMillis()
            try {
                val result = withTimeout(task.timeoutMillis) {
                    withContext(Dispatchers.IO) {
                        resilientExecutor.execute { executeTask(task) }.value.getOrThrow()
                    }
                }
                task.id to AgentAssignment(
                    task = task,
                    agentId = "${task.role.name.lowercase()}-${task.id.take(8)}",
                    result = result,
                    startedAtMillis = startTime,
                    finishedAtMillis = System.currentTimeMillis(),
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                task.id to AgentAssignment(
                    task = task,
                    agentId = "${task.role.name.lowercase()}-${task.id.take(8)}",
                    result = MissionStepResult.Failed("Agent error: ${e.message}"),
                    startedAtMillis = startTime,
                    finishedAtMillis = System.currentTimeMillis(),
                    error = e.message,
                )
            }
        }
    }

    fun getAssignmentsForRole(role: AgentRole): List<AgentAssignment> =
        mutableAssignments.value.values.filter { it.task.role == role }

    fun getFailedAssignments(): List<AgentAssignment> =
        mutableAssignments.value.values.filter { it.result is MissionStepResult.Failed }

    fun clear() {
        mutableAssignments.value = emptyMap()
    }
}
