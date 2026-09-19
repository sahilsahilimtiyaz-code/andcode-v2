package com.yugahashimoto.andcode.core.api

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class OpenCodeEventParser(
    private val json: Json =
        Json {
            ignoreUnknownKeys = true
            isLenient = true
        },
) {
    fun parse(raw: String): OpenCodeEvent {
        val envelope =
            runCatching { json.parseToJsonElement(raw).jsonObject }.getOrNull()
                ?: return OpenCodeEvent.Unknown("invalid", raw)
        // `/global/event` wraps each instance event as {directory, project, workspace, payload};
        // the per-instance `/event` stream emits the event object directly.
        val root = envelope["payload"] as? JsonObject ?: envelope
        val type = root["type"]?.jsonPrimitive?.content ?: return OpenCodeEvent.Unknown("missing-type", raw)
        val properties = root["properties"] as? JsonObject ?: JsonObject(emptyMap())

        return runCatching {
            when (type) {
                "server.connected" -> OpenCodeEvent.ServerConnected
                "message.updated" -> {
                    val infoElement = properties["info"] ?: return@runCatching OpenCodeEvent.Unknown(type, raw)
                    val info = json.decodeFromJsonElement(OpenCodeMessageInfo.serializer(), infoElement.jsonObject)
                    OpenCodeEvent.MessageUpdated(info)
                }
                "message.part.updated" -> {
                    val partElement = properties["part"] ?: return@runCatching OpenCodeEvent.Unknown(type, raw)
                    val part = json.decodeFromJsonElement(OpenCodePart.serializer(), partElement.jsonObject)
                    OpenCodeEvent.MessagePartUpdated(part)
                }
                "message.part.delta" -> {
                    val sessionId = properties["sessionID"]?.jsonPrimitive?.content ?: return@runCatching OpenCodeEvent.Unknown(type, raw)
                    val messageId = properties["messageID"]?.jsonPrimitive?.content ?: return@runCatching OpenCodeEvent.Unknown(type, raw)
                    val partId = properties["partID"]?.jsonPrimitive?.content ?: return@runCatching OpenCodeEvent.Unknown(type, raw)
                    val field = properties["field"]?.jsonPrimitive?.content ?: return@runCatching OpenCodeEvent.Unknown(type, raw)
                    val delta = properties["delta"]?.jsonPrimitive?.content ?: return@runCatching OpenCodeEvent.Unknown(type, raw)
                    OpenCodeEvent.MessagePartDelta(sessionId, messageId, partId, field, delta)
                }
                "permission.asked" -> {
                    val id = properties["id"]?.jsonPrimitive?.content ?: return@runCatching OpenCodeEvent.Unknown(type, raw)
                    val sessionId = properties["sessionID"]?.jsonPrimitive?.content ?: return@runCatching OpenCodeEvent.Unknown(type, raw)
                    val permission = properties["permission"]?.jsonPrimitive?.content ?: return@runCatching OpenCodeEvent.Unknown(type, raw)
                    OpenCodeEvent.PermissionAsked(
                        PermissionRequest(
                            id = id,
                            sessionId = sessionId,
                            permission = permission,
                            patterns =
                                (properties["patterns"] as? JsonArray)
                                    ?.mapNotNull { element -> (element as? JsonPrimitive)?.content }
                                    .orEmpty(),
                            metadata =
                                (properties["metadata"] as? JsonObject)
                                    ?.entries
                                    ?.associate { (key, value) -> key to value }
                                    .orEmpty(),
                        ),
                    )
                }
                "question.asked" -> {
                    val questions =
                        (properties["questions"] as? JsonArray)
                            ?.mapNotNull { element -> parseQuestionPrompt(element) }
                            .orEmpty()
                    if (questions.isEmpty()) return@runCatching OpenCodeEvent.Unknown(type, raw)

                    val id = properties["id"]?.jsonPrimitive?.content ?: return@runCatching OpenCodeEvent.Unknown(type, raw)
                    val sessionId = properties["sessionID"]?.jsonPrimitive?.content ?: return@runCatching OpenCodeEvent.Unknown(type, raw)
                    OpenCodeEvent.QuestionAsked(
                        QuestionRequest(
                            id = id,
                            sessionId = sessionId,
                            questions = questions,
                            directory = (envelope["directory"] as? JsonPrimitive)?.content,
                        ),
                    )
                }
                "permission.replied" -> {
                    val sessionId = properties["sessionID"]?.jsonPrimitive?.content ?: return@runCatching OpenCodeEvent.Unknown(type, raw)
                    val requestId = properties["requestID"]?.jsonPrimitive?.content ?: return@runCatching OpenCodeEvent.Unknown(type, raw)
                    OpenCodeEvent.PermissionReplied(sessionId = sessionId, requestId = requestId)
                }
                "session.idle" -> {
                    val sessionId = properties["sessionID"]?.jsonPrimitive?.content ?: return@runCatching OpenCodeEvent.Unknown(type, raw)
                    OpenCodeEvent.SessionIdle(sessionId)
                }
                "session.created" -> {
                    val infoElement = properties["info"] ?: return@runCatching OpenCodeEvent.Unknown(type, raw)
                    val session = json.decodeFromJsonElement(OpenCodeSession.serializer(), infoElement.jsonObject)
                    OpenCodeEvent.SessionCreated(session)
                }
                "session.updated" -> {
                    val infoElement = properties["info"] ?: return@runCatching OpenCodeEvent.Unknown(type, raw)
                    val session = json.decodeFromJsonElement(OpenCodeSession.serializer(), infoElement.jsonObject)
                    OpenCodeEvent.SessionUpdated(session)
                }
                "session.status" -> {
                    val sessionId = properties["sessionID"]?.jsonPrimitive?.content ?: return@runCatching OpenCodeEvent.Unknown(type, raw)
                    val status = properties["status"]?.jsonObject?.get("type")?.jsonPrimitive?.content ?: return@runCatching OpenCodeEvent.Unknown(type, raw)
                    OpenCodeEvent.SessionStatusChanged(sessionId = sessionId, status = status)
                }
                "session.error" -> {
                    val errorObject = properties["error"] as? JsonObject
                    OpenCodeEvent.SessionError(
                        sessionId = (properties["sessionID"] as? JsonPrimitive)?.content,
                        message = describeError(properties["error"]),
                        name = (errorObject?.get("name") as? JsonPrimitive)?.content,
                    )
                }
                else -> OpenCodeEvent.Unknown(type, raw)
            }
        }.getOrElse { OpenCodeEvent.Unknown(type, raw) }
    }

    /**
     * OpenCode reports session failures as a named error object, e.g.
     * `{"name":"ProviderAuthError","data":{"message":"..."}}`. Prefer the human-readable message
     * over dumping the raw JSON into the chat.
     */
    private fun describeError(element: kotlinx.serialization.json.JsonElement?): String? {
        if (element == null) return null
        (element as? JsonPrimitive)?.let { return it.content }
        val error = element as? JsonObject ?: return element.toString()
        val message =
            ((error["data"] as? JsonObject)?.get("message") as? JsonPrimitive)
                ?.content
                ?.takeIf { it.isNotBlank() }
        val name = (error["name"] as? JsonPrimitive)?.content?.takeIf { it.isNotBlank() }
        return when {
            message != null && name != null -> "$name: $message"
            message != null -> message
            name != null -> name
            else -> error.toString()
        }
    }

    private fun parseQuestionPrompt(element: kotlinx.serialization.json.JsonElement): QuestionPrompt? =
        when {
            element is JsonPrimitive && element.isString -> QuestionPrompt(question = element.content)
            element is JsonObject -> {
                val prompt = element
                val question = (prompt["question"] as? JsonPrimitive)?.content
                question?.let {
                    QuestionPrompt(
                        question = it,
                        header = (prompt["header"] as? JsonPrimitive)?.content,
                        options =
                            (prompt["options"] as? JsonArray)
                                ?.mapNotNull { option -> parseQuestionOption(option) }
                                .orEmpty(),
                        placeholder = (prompt["placeholder"] as? JsonPrimitive)?.content,
                        // Both flags belong to the individual prompt, not to the request.
                        multiple = (prompt["multiple"] as? JsonPrimitive)?.booleanOrNull ?: false,
                        custom = (prompt["custom"] as? JsonPrimitive)?.booleanOrNull ?: true,
                    )
                }
            }
            else -> null
        }

    private fun parseQuestionOption(element: kotlinx.serialization.json.JsonElement): QuestionOption? =
        when {
            element is JsonPrimitive && element.isString -> QuestionOption(label = element.content)
            element is JsonObject -> {
                val option = element
                val label = (option["label"] as? JsonPrimitive)?.content
                label?.let {
                    QuestionOption(
                        label = it,
                        description = (option["description"] as? JsonPrimitive)?.content,
                    )
                }
            }
            else -> null
        }
}
