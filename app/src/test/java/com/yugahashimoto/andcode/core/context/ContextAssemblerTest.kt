package com.yugahashimoto.andcode.core.context

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ContextAssemblerTest {

    private val assembler = ContextAssembler()

    @Test
    fun `assembles context from system prompt`() {
        val result = assembler.assemble(
            ContextRequest(
                query = "test",
                systemPrompt = "You are a coding assistant.",
            )
        )
        assertTrue(result.snippets.any { it.source == ContextSource.SYSTEM_PROMPT })
        assertTrue(result.totalTokens > 0)
    }

    @Test
    fun `assembles context from conversation history`() {
        val result = assembler.assemble(
            ContextRequest(
                query = "user service",
                conversationHistory = listOf(
                    "How do I create a user service?",
                    "Use dependency injection.",
                ),
            )
        )
        assertTrue(result.snippets.any { it.source == ContextSource.CONVERSATION })
    }

    @Test
    fun `assembles context from file content`() {
        val result = assembler.assemble(
            ContextRequest(
                query = "UserService",
                relevantFiles = listOf(
                    "UserService.kt" to "class UserService { fun getUser() {} }",
                ),
            )
        )
        assertTrue(result.snippets.any { it.source == ContextSource.FILE_CONTENT })
    }

    @Test
    fun `respects token budget`() {
        val result = assembler.assemble(
            ContextRequest(
                query = "test",
                systemPrompt = "x ".repeat(5000),
                conversationHistory = listOf("y ".repeat(5000)),
                budget = ContextBudget(maxTokens = 500),
            )
        )
        assertTrue(result.totalTokens <= 600)
    }

    @Test
    fun `tracks source token counts`() {
        val result = assembler.assemble(
            ContextRequest(
                query = "test",
                systemPrompt = "System prompt here",
                conversationHistory = listOf("Hello", "World"),
            )
        )
        assertTrue(result.sourceTokenCounts.containsKey(ContextSource.SYSTEM_PROMPT))
        assertTrue(result.sourceTokenCounts.containsKey(ContextSource.CONVERSATION))
    }

    @Test
    fun `budgetUsed is within range`() {
        val result = assembler.assemble(
            ContextRequest(
                query = "test",
                systemPrompt = "short prompt",
                budget = ContextBudget(maxTokens = 8000),
            )
        )
        assertTrue(result.budgetUsed in 0.0..1.0)
    }
}
