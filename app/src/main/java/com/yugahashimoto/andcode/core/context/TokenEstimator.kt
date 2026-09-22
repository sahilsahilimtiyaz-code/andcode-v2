package com.yugahashimoto.andcode.core.context

object TokenEstimator {

    private const val CHARS_PER_TOKEN = 4.0
    private const val WORDS_PER_TOKEN = 0.75
    private const val NEWLINE_BONUS = 0.1

    fun estimate(text: String): Int {
        if (text.isEmpty()) return 0
        val charEstimate = (text.length / CHARS_PER_TOKEN).toInt()
        val wordEstimate = (text.split(WHITESPACE).count { it.isNotEmpty() } / WORDS_PER_TOKEN).toInt()
        val newlineDiscount = (text.count { it == '\n' } * NEWLINE_BONUS).toInt()
        return ((charEstimate + wordEstimate) / 2 - newlineDiscount).coerceAtLeast(1)
    }

    fun estimateBatch(texts: List<String>): Int = texts.sumOf { estimate(it) }

    fun fitsWithin(text: String, maxTokens: Int): Boolean = estimate(text) <= maxTokens

    fun truncateToTokens(text: String, maxTokens: Int): String {
        val estimatedChars = (maxTokens * CHARS_PER_TOKEN).toInt()
        if (text.length <= estimatedChars) return text
        val truncated = text.substring(0, estimatedChars)
        val lastNewline = truncated.lastIndexOf('\n')
        return if (lastNewline > estimatedChars / 2) {
            truncated.substring(0, lastNewline)
        } else {
            truncated
        }
    }

    private val WHITESPACE = Regex("""\s+""")
}
