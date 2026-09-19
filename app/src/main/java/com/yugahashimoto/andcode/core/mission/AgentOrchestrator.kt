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
        val taskMap = tasks.associateBy { it.id }
        val completedAssignments = mutableMapOf<String, AgentAssignment>()

        val executionOrder = topologicalSort(tasks)

        for (wave in executionOrder) {
            coroutineScope {
                val deferredResults = wave.map { task ->
                    async(Dispatchers.IO) {
                        executeSingleTask(task, completedAssignments)
                    }
                }
                deferredResults.awaitAll().forEach { (taskId, assignment) ->
                    completedAssignments[taskId] = assignment
                }
            }

            if (config.failFast && completedAssignments.values.any { it.result is MissionStepResult.Failed }) {
                break
            }
        }

        mutableAssignments.value = completedAssignments
        return completedAssignments
    }

    private fun topologicalSort(tasks: List<AgentTask>): List<List<AgentTask>> {
        val taskMap = tasks.associateBy { it.id }
        val completed = mutableSetOf<String>()
        val waves = mutableListOf<List<AgentTask>>()
        var remaining = tasks.toList()

        while (remaining.isNotEmpty()) {
            val ready = remaining.filter { task ->
                task.dependsOn.all { it in completed }
            }
            if (ready.isEmpty()) {
                waves.add(remaining)
                break
            }
            waves.add(ready)
            completed.addAll(ready.map { it.id })
            remaining = remaining.filter { it.id !in completed }
        }

        return waves
    }

    private suspend fun executeSingleTask(
        task: AgentTask,
        completedAssignments: Map<String, AgentAssignment>,
    ): Pair<String, AgentAssignment> {
        for (depId in task.dependsOn) {
            val depAssignment = completedAssignments[depId]
            if (depAssignment == null) {
                return task.id to AgentAssignment(
                    task = task,
                    agentId = "blocked",
                    result = MissionStepResult.Failed("Dependency $depId not completed"),
                    error = "Missing dependency",
                )
            }
            if (depAssignment.result is MissionStepResult.Failed) {
                return task.id to AgentAssignment(
                    task = task,
                    agentId = "blocked",
                    result = MissionStepResult.Failed("Dependency $depId failed"),
                    error = "Blocked by failed dependency",
                )
            }
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
