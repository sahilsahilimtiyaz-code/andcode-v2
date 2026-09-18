package com.yugahashimoto.andcode.core.api

import kotlinx.serialization.Serializable

@Serializable
data class ModelLimit(
    val context: Long = 0L,
    val input: Long = 0L,
    val output: Long = 0L,
)

@Serializable
data class OpenCodeModel(
    val id: String,
    val name: String = id,
    val limit: ModelLimit? = null,
)

@Serializable
data class OpenCodeProvider(
    val id: String,
    val name: String = id,
    val models: Map<String, OpenCodeModel> = emptyMap(),
) {
    val connected: Boolean get() = models.isNotEmpty()
}

@Serializable
data class ProviderCatalog(
    val all: List<OpenCodeProvider> = emptyList(),
    val connected: List<String> = emptyList(),
)
