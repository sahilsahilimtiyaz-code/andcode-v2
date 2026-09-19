package com.yugahashimoto.andcode.core.context

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ContextBudgetAllocatorTest {

    @Test
    fun `allocate distributes budget by source weight`() {
        val allocator = ContextBudgetAllocator()
        val budget = allocator.allocate(ContextBudget(maxTokens = 8000))

        assertEquals(8000, budget.maxTokens)
        val total = budget.allocations.values.sum()
        assertTrue(total <= 8000)
    }

    @Test
    fun `each source gets at least minimum tokens`() {
        val allocator = ContextBudgetAllocator()
        val budget = allocator.allocate(ContextBudget(maxTokens = 8000))

        for ((source, tokens) in budget.allocations) {
            assertTrue("$source got $tokens, expected >= ${ContextBudgetAllocator.MIN_TOKENS_PER_SOURCE}",
                tokens >= ContextBudgetAllocator.MIN_TOKENS_PER_SOURCE)
        }
    }

    @Test
    fun `conversation gets largest allocation`() {
        val allocator = ContextBudgetAllocator()
        val budget = allocator.allocate(ContextBudget(maxTokens = 8000))

        val conversationAlloc = budget.allocationFor(ContextSource.CONVERSATION)
        val systemAlloc = budget.allocationFor(ContextSource.SYSTEM_PROMPT)
        assertTrue(conversationAlloc > systemAlloc)
    }

    @Test
    fun `rebalance adjusts based on usage`() {
        val allocator = ContextBudgetAllocator()
        val original = allocator.allocate(ContextBudget(maxTokens = 8000))

        val used = mapOf(
            ContextSource.SYSTEM_PROMPT to 500,
            ContextSource.CONVERSATION to 3000,
        )
        val rebalanced = allocator.rebalance(original, used)

        assertTrue(rebalanced.maxTokens == 8000)
        val totalRebalanced = rebalanced.allocations.values.sum()
        assertTrue(totalRebalanced <= 8000)
    }

    @Test
    fun `small budget still works`() {
        val allocator = ContextBudgetAllocator()
        val budget = allocator.allocate(ContextBudget(maxTokens = 1000))
        assertTrue(budget.maxTokens == 1000)
    }
}
