package com.yugahashimoto.andcode.core.mission

import com.yugahashimoto.andcode.core.api.OpenCodeProvider
import com.yugahashimoto.andcode.core.api.OpenCodeModel
import com.yugahashimoto.andcode.core.api.ModelLimit
import com.yugahashimoto.andcode.core.api.ProviderCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ModelRouterTest {

    private fun testCatalog(): ProviderCatalog = ProviderCatalog(
        all = listOf(
            OpenCodeProvider(
                id = "anthropic",
                name = "Anthropic",
                models = mapOf(
                    "claude-sonnet" to OpenCodeModel(
                        id = "claude-sonnet",
                        name = "Claude Sonnet",
                        limit = ModelLimit(context = 200_000, input = 180_000, output = 20_000),
                    ),
                    "claude-haiku" to OpenCodeModel(
                        id = "claude-haiku",
                        name = "Claude Haiku",
                        limit = ModelLimit(context = 100_000),
                    ),
                ),
            ),
            OpenCodeProvider(
                id = "openai",
                name = "OpenAI",
                models = mapOf(
                    "gpt-4o" to OpenCodeModel(
                        id = "gpt-4o",
                        name = "GPT-4o",
                        limit = ModelLimit(context = 128_000),
                    ),
                ),
            ),
        ),
        connected = listOf("anthropic", "openai"),
    )

    @Test
    fun `selectModel returns preferred provider when healthy`() {
        val router = ModelRouter()
        val catalog = testCatalog()
        val decision = router.selectModel(
            catalog = catalog,
            preferredProviderId = "anthropic",
            preferredModelId = "claude-sonnet",
        )
        assertEquals("anthropic", decision.providerId)
        assertEquals("claude-sonnet", decision.modelId)
        assertEquals(RoutingReason.HEALTHY, decision.reason)
    }

    @Test
    fun `selectModel falls back when preferred not connected`() {
        val router = ModelRouter()
        val catalog = testCatalog()
        val decision = router.selectModel(
            catalog = catalog,
            preferredProviderId = "google",
            preferredModelId = "gemini",
        )
        assertTrue(decision.providerId in listOf("anthropic", "openai"))
        assertEquals(RoutingReason.FALLBACK, decision.reason)
    }

    @Test
    fun `selectModel respects context requirement`() {
        val router = ModelRouter()
        val catalog = testCatalog()
        val decision = router.selectModel(
            catalog = catalog,
            requiredContextTokens = 150_000,
        )
        assertEquals("anthropic", decision.providerId)
        assertEquals("claude-sonnet", decision.modelId)
    }

    @Test
    fun `recordSuccess updates health`() {
        val router = ModelRouter()
        router.recordSuccess("anthropic")
        val health = router.getHealth("anthropic")
        assertEquals(1, health.successCount)
        assertEquals(0, health.errorCount)
    }

    @Test
    fun `recordFailure updates health and circuit breaker`() {
        val router = ModelRouter()
        repeat(5) { router.recordFailure("anthropic") }
        val health = router.getHealth("anthropic")
        assertEquals(5, health.errorCount)
        assertEquals(com.yugahashimoto.andcode.core.reliability.CircuitState.OPEN, health.circuitState)
    }

    @Test
    fun `selectModel skips unhealthy providers`() {
        val router = ModelRouter()
        repeat(5) { router.recordFailure("anthropic") }
        val catalog = testCatalog()
        val decision = router.selectModel(catalog = catalog)
        assertEquals("openai", decision.providerId)
    }

    @Test
    fun `resetCircuitBreaker clears state`() {
        val router = ModelRouter()
        repeat(5) { router.recordFailure("anthropic") }
        router.resetCircuitBreaker("anthropic")
        val health = router.getHealth("anthropic")
        assertEquals(com.yugahashimoto.andcode.core.reliability.CircuitState.CLOSED, health.circuitState)
    }
}
