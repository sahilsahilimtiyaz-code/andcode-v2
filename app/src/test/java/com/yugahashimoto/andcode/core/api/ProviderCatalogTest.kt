package com.yugahashimoto.andcode.core.api

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProviderCatalogTest {

    @Test
    fun `provider reports connected when it has models`() {
        val provider = OpenCodeProvider(
            id = "anthropic",
            name = "Anthropic",
            models = mapOf("claude-sonnet" to OpenCodeModel(id = "claude-sonnet")),
        )
        assertTrue(provider.connected)
    }

    @Test
    fun `provider reports not connected when no models`() {
        val provider = OpenCodeProvider(id = "openai", name = "OpenAI")
        assertFalse(provider.connected)
    }

    @Test
    fun `catalog connected list filters providers`() {
        val catalog = ProviderCatalog(
            all = listOf(
                OpenCodeProvider(id = "anthropic", models = mapOf("m" to OpenCodeModel("m"))),
                OpenCodeProvider(id = "openai"),
            ),
            connected = listOf("anthropic"),
        )
        assertEquals(1, catalog.connected.size)
        assertEquals("anthropic", catalog.connected.first())
    }
}
