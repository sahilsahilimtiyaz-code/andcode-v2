package com.yugahashimoto.andcode.core.context

class ContextAssembler(
    private val tokenEstimator: TokenEstimator = TokenEstimator,
    private val relevanceScorer: RelevanceScorer = RelevanceScorer,
    private val budgetAllocator: ContextBudgetAllocator = ContextBudgetAllocator(),
) {
    fun assemble(request: ContextRequest): AssembledContext {
        val budget = budgetAllocator.allocate(request.budget)
        val snippets = mutableListOf<ContextSnippet>()
        val sourceTokens = mutableMapOf<ContextSource, Int>()

        if (request.systemPrompt.isNotBlank()) {
            val tokens = tokenEstimator.estimate(request.systemPrompt)
            snippets.add(
                ContextSnippet(
                    source = ContextSource.SYSTEM_PROMPT,
                    content = request.systemPrompt,
                    tokenEstimate = tokens,
                )
            )
            sourceTokens[ContextSource.SYSTEM_PROMPT] = tokens
        }

        val rankedConversation = request.conversationHistory.map { msg ->
            ContextSnippet(source = ContextSource.CONVERSATION, content = msg)
        }.let { relevanceScorer.rankSnippets(request.query, it) }

        var conversationBudget = budget.allocationFor(ContextSource.CONVERSATION)
        var conversationTokens = 0
        for (snippet in rankedConversation) {
            if (conversationTokens + snippet.tokenEstimate <= conversationBudget) {
                snippets.add(snippet)
                conversationTokens += snippet.tokenEstimate
            }
        }
        sourceTokens[ContextSource.CONVERSATION] = conversationTokens

        val rankedFiles = request.relevantFiles.map { (path, content) ->
            ContextSnippet(
                source = ContextSource.FILE_CONTENT,
                content = content,
                tokenEstimate = tokenEstimator.estimate(content),
                filePath = path,
            )
        }.let { relevanceScorer.rankSnippets(request.query, it) }

        var fileBudget = budget.allocationFor(ContextSource.FILE_CONTENT)
        var fileTokens = 0
        for (snippet in rankedFiles) {
            if (fileTokens + snippet.tokenEstimate <= fileBudget) {
                snippets.add(snippet)
                fileTokens += snippet.tokenEstimate
            }
        }
        sourceTokens[ContextSource.FILE_CONTENT] = fileTokens

        var toolBudget = budget.allocationFor(ContextSource.TOOL_OUTPUT)
        var toolTokens = 0
        for (output in request.toolOutputs) {
            val tokens = tokenEstimator.estimate(output)
            if (toolTokens + tokens <= toolBudget) {
                snippets.add(ContextSnippet(source = ContextSource.TOOL_OUTPUT, content = output, tokenEstimate = tokens))
                toolTokens += tokens
            }
        }
        sourceTokens[ContextSource.TOOL_OUTPUT] = toolTokens

        val totalTokens = snippets.sumOf { it.tokenEstimate }
        val droppedConversation = if (conversationTokens > 0) 0 else rankedConversation.size
        val droppedFiles = if (fileTokens > 0) 0 else rankedFiles.size

        return AssembledContext(
            snippets = snippets,
            totalTokens = totalTokens,
            budgetUsed = if (budget.maxTokens > 0) totalTokens.toDouble() / budget.maxTokens else 0.0,
            sourceTokenCounts = sourceTokens,
            droppedSnippets = (droppedConversation + droppedFiles).coerceAtLeast(0),
        )
    }
}
