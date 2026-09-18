package com.yugahashimoto.andcode.core.mission

import com.yugahashimoto.andcode.core.api.OpenCodeEvent
import com.yugahashimoto.andcode.core.reliability.RetryPolicy
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class StreamState(
    val sessionId: String,
    val lastEventId: String? = null,
    val partialOutput: String = "",
    val connected: Boolean = false,
    val reconnectCount: Int = 0,
    val lastEventMillis: Long = 0L,
    val error: String? = null,
)

class StreamResumeManager(
    private val maxReconnectAttempts: Int = 10,
    private val retryPolicy: RetryPolicy = RetryPolicy(
        maxAttempts = 10,
        baseDelayMillis = 1000L,
        maxDelayMillis = 30_000L,
        backoffMultiplier = 2.0,
        jitter = true,
    ),
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
) {
    private val mutableStates = MutableStateFlow<Map<String, StreamState>>(emptyMap())
    val streamStates: StateFlow<Map<String, StreamState>> = mutableStates.asStateFlow()

    private val activeJobs = mutableMapOf<String, Job>()

    fun getState(sessionId: String): StreamState =
        mutableStates.value[sessionId] ?: StreamState(sessionId)

    fun resumeStream(
        sessionId: String,
        eventSource: suspend (lastEventId: String?) -> Flow<OpenCodeEvent>,
        onEvent: (OpenCodeEvent) -> Unit,
        onComplete: () -> Unit = {},
        onError: (Throwable) -> Unit = {},
    ): Job {
        activeJobs[sessionId]?.cancel()
        updateState(sessionId) { it.copy(connected = true, error = null) }

        val job = scope.launch {
            var reconnectAttempt = 0
            var running = true

            while (running && reconnectAttempt < maxReconnectAttempts) {
                try {
                    val lastId = mutableStates.value[sessionId]?.lastEventId
                    val events = eventSource(lastId)

                    events.collect { event ->
                        reconnectAttempt = 0
                        val eventId = extractEventId(event)
                        updateState(sessionId) {
                            it.copy(
                                lastEventId = eventId ?: it.lastEventId,
                                connected = true,
                                lastEventMillis = System.currentTimeMillis(),
                                reconnectCount = 0,
                                error = null,
                            )
                        }
                        onEvent(event)
                    }

                    running = false
                    updateState(sessionId) { it.copy(connected = false) }
                    onComplete()
                } catch (e: CancellationException) {
                    updateState(sessionId) { it.copy(connected = false) }
                    throw e
                } catch (e: Throwable) {
                    reconnectAttempt++
                    val delayMs = retryPolicy.delayForAttempt(reconnectAttempt)
                    updateState(sessionId) {
                        it.copy(
                            connected = false,
                            reconnectCount = reconnectAttempt,
                            error = e.message,
                        )
                    }
                    onError(e)
                    if (reconnectAttempt < maxReconnectAttempts) {
                        delay(delayMs)
                    } else {
                        running = false
                    }
                }
            }
        }

        activeJobs[sessionId] = job
        return job
    }

    fun disconnect(sessionId: String) {
        activeJobs.remove(sessionId)?.cancel()
        updateState(sessionId) { it.copy(connected = false) }
    }

    fun appendPartialOutput(sessionId: String, delta: String) {
        updateState(sessionId) { it.copy(partialOutput = it.partialOutput + delta) }
    }

    fun clearPartialOutput(sessionId: String) {
        updateState(sessionId) { it.copy(partialOutput = "") }
    }

    fun destroy() {
        activeJobs.values.forEach { it.cancel() }
        activeJobs.clear()
        scope.cancel()
    }

    private fun updateState(sessionId: String, transform: (StreamState) -> StreamState) {
        val current = mutableStates.value[sessionId] ?: StreamState(sessionId)
        mutableStates.value = mutableStates.value + (sessionId to transform(current))
    }

    private fun extractEventId(event: OpenCodeEvent): String? = when (event) {
        is OpenCodeEvent.MessagePartDelta -> "${event.sessionId}:${event.partId}:${event.field}"
        is OpenCodeEvent.MessageUpdated -> "${event.info.sessionId}:${event.info.id}"
        is OpenCodeEvent.MessagePartUpdated -> "${event.part.sessionId}:${event.part.id}"
        is OpenCodeEvent.SessionIdle -> "idle:${event.sessionId}"
        is OpenCodeEvent.SessionError -> "error:${event.sessionId}"
        else -> null
    }
}
