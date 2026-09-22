package com.yugahashimoto.andcode.core.context

object RelevanceScorer {

    fun score(query: String, snippet: ContextSnippet): Double {
        if (query.isBlank()) return 0.0
        val queryTerms = tokenize(query)
        val contentTerms = tokenize(snippet.content)
        if (queryTerms.isEmpty() || contentTerms.isEmpty()) return 0.0

        val termFrequency = queryTerms.sumOf { term ->
            contentTerms.count { it == term }
        }.toDouble() / contentTerms.size

        val coverage = queryTerms.count { term ->
            contentTerms.any { it == term }
        }.toDouble() / queryTerms.size

        val sourceWeight = snippet.source.weight

        val lengthPenalty = when {
            snippet.content.length < 10 -> 0.5
            snippet.content.length > 2000 -> 0.7
            else -> 1.0
        }

        val pathBonus = if (snippet.filePath != null) 1.1 else 1.0

        return (termFrequency * 0.4 + coverage * 0.4 + sourceWeight * 0.2) *
            lengthPenalty * pathBonus
    }

    fun rankSnippets(query: String, snippets: List<ContextSnippet>): List<ContextSnippet> {
        return snippets
            .map { it.copy(relevanceScore = score(query, it)) }
            .sortedByDescending { it.relevanceScore }
    }

    fun filterByThreshold(query: String, snippets: List<ContextSnippet>, threshold: Double = 0.1): List<ContextSnippet> {
        return rankSnippets(query, snippets).filter { it.relevanceScore >= threshold }
    }

    private fun tokenize(text: String): List<String> {
        return text.lowercase()
            .split(WORD_BOUNDARY)
            .filter { it.length >= 2 && it !in STOP_WORDS }
    }

    private val WORD_BOUNDARY = Regex("""[\s_\-./\\(){}\[\]<>'"]+""")
    private val STOP_WORDS = setOf(
        "the", "a", "an", "is", "are", "was", "were", "be", "been", "being",
        "have", "has", "had", "do", "does", "did", "will", "would", "could",
        "should", "may", "might", "shall", "can", "need", "dare", "ought",
        "used", "to", "of", "in", "for", "on", "with", "at", "by", "from",
        "as", "into", "through", "during", "before", "after", "above", "below",
        "between", "out", "off", "over", "under", "again", "further", "then",
        "once", "here", "there", "when", "where", "why", "how", "all", "both",
        "each", "few", "more", "most", "other", "some", "such", "no", "nor",
        "not", "only", "own", "same", "so", "than", "too", "very", "just",
        "don", "now", "it", "its", "this", "that", "these", "those",
    )
}
