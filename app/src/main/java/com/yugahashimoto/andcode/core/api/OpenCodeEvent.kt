package com.yugahashimoto.andcode.core.api

import kotlinx.serialization.Serializable

@Serializable
sealed class OpenCodeEvent {
    @Serializable
    data class SessionIdle(val sessionId: String) : OpenCodeEvent()

    @Serializable
    data class SessionError(val sessionId: String, val error: String) : OpenCodeEvent()

    @Serializable
    data class MessageUpdated(val info: MessageInfo) : OpenCodeEvent()

    @Serializable
    data class MessagePartUpdated(val part: MessagePartInfo) : OpenCodeEvent()

    @Serializable
    data class MessagePartDelta(
        val sessionId: String,
        val partId: String,
        val field: String,
        val delta: String,
    ) : OpenCodeEvent()

    @Serializable
    data class ProviderListUpdated(val providers: List<String>) : OpenCodeEvent()

    @Serializable
    data class PermissionRequest(
        val sessionId: String,
        val toolName: String,
        val input: Map<String, String> = emptyMap(),
    ) : OpenCodeEvent()

    @Serializable
    data class PermissionResponse(val sessionId: String, val granted: Boolean) : OpenCodeEvent()
}

@Serializable
data class MessageInfo(
    val id: String,
    val sessionId: String,
    val role: String = "assistant",
    val parts: List<MessagePartInfo> = emptyList(),
)

@Serializable
data class MessagePartInfo(
    val id: String,
    val sessionId: String,
    val messageId: String,
    val type: String = "text",
    val text: String = "",
)
