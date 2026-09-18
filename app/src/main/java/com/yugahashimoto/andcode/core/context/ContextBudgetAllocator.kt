package com.yugahashimoto.andcode.core.context

class ContextBudgetAllocator(
    private val defaultMaxTokens: Int = 8000,
) {
    fun allocate(budget: ContextBudget): ContextBudget {
        val max = if (budget.maxTokens > 0) budget.maxTokens else defaultMaxTokens
        val totalWeight = ContextSource.entries.sumOf { it.weight }

        val allocations = ContextSource.entries.associateWith { source ->
            val proportional = (max * source.weight / totalWeight).toInt()
            proportional.coerceIn(MIN_TOKENS_PER_SOURCE, max)
        }

        val allocated = allocations.values.sum()
        if (allocated > max) {
            val overflow = allocated - max
            val largestSource = allocations.maxByOrNull { it.value }?.key ?: ContextSource.CONVERSATION
            return ContextBudget(
                maxTokens = max,
                allocations = allocations + (largestSource to (allocations[largestSource]!! - overflow)),
            )
        }

        return ContextBudget(maxTokens = max, allocations = allocations)
    }

    fun rebalance(
        original: ContextBudget,
        used: Map<ContextSource, Int>,
    ): ContextBudget {
        val max = original.maxTokens
        val newAllocations = mutableMapOf<ContextSource, Int>()

        for (source in ContextSource.entries) {
            val budgetForSource = original.allocationFor(source)
            val usedBySource = used[source] ?: 0
            val remaining = budgetForSource - usedBySource
            newAllocations[source] = remaining.coerceAtLeast(0)
        }

        val totalRemaining = newAllocations.values.sum()
        val deficit = max - totalRemaining
        if (deficit > 0) {
            val unconstrained = ContextSource.entries
                .filter { used[it] == null || used[it]!! < original.allocationFor(it) }
            if (unconstrained.isNotEmpty()) {
                val extraPerSource = deficit / unconstrained.size
                for (source in unconstrained) {
                    newAllocations[source] = (newAllocations[source] ?: 0) + extraPerSource
                }
            }
        }

        return ContextBudget(maxTokens = max, allocations = newAllocations)
    }

    companion object {
        const val MIN_TOKENS_PER_SOURCE = 100
    }
}
