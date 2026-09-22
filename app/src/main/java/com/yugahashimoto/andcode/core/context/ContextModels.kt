package com.yugahashimoto.andcode.core.context

enum class ContextSource(val weight: Double) {
    SYSTEM_PROMPT(0.20),
    CONVERSATION(0.50),
    PROJECT_INDEX(0.15),
    FILE_CONTENT(0.10),
    TOOL_OUTPUT(0.05),
}

data class ContextSnippet(
    val source: ContextSource,
    val content: String,
    val tokenEstimate: Int = 0,
    val relevanceScore: Double = 0.0,
    val filePath: String? = null,
    val lineRange: IntRange? = null,
    val metadata: Map<String, String> = emptyMap(),
)

data class ContextBudget(
    val maxTokens: Int = 8000,
    val allocations: Map<ContextSource, Int> = emptyMap(),
) {
    fun allocationFor(source: ContextSource): Int =
        allocations[source] ?: (maxTokens * source.weight).toInt()
}

data class AssembledContext(
    val snippets: List<ContextSnippet> = emptyList(),
    val totalTokens: Int = 0,
    val budgetUsed: Double = 0.0,
    val sourceTokenCounts: Map<ContextSource, Int> = emptyMap(),
    val droppedSnippets: Int = 0,
)

data class ContextRequest(
    val query: String,
    val conversationHistory: List<String> = emptyList(),
    val relevantFiles: List<Pair<String, String>> = emptyList(),
    val systemPrompt: String = "",
    val toolOutputs: List<String> = emptyList(),
    val budget: ContextBudget = ContextBudget(),
)
