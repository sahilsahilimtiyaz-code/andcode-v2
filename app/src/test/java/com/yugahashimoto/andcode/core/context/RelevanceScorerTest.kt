package com.yugahashimoto.andcode.core.context

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RelevanceScorerTest {

    @Test
    fun `higher score for more relevant content`() {
        val relevant = ContextSnippet(
            source = ContextSource.CONVERSATION,
            content = "UserService fetches user data from the database",
        )
        val irrelevant = ContextSnippet(
            source = ContextSource.CONVERSATION,
            content = "The weather is nice today",
        )
        val score = RelevanceScorer.score("user service", relevant)
        val badScore = RelevanceScorer.score("user service", irrelevant)
        assertTrue(score > badScore)
    }

    @Test
    fun `rankSnippets orders by relevance`() {
        val snippets = listOf(
            ContextSnippet(source = ContextSource.CONVERSATION, content = "unrelated text about weather"),
            ContextSnippet(source = ContextSource.CONVERSATION, content = "UserRepository handles user CRUD operations"),
            ContextSnippet(source = ContextSource.CONVERSATION, content = "The user interface is well designed"),
        )
        val ranked = RelevanceScorer.rankSnippets("UserRepository", snippets)
        assertEquals("UserRepository handles user CRUD operations", ranked.first().content)
    }

    @Test
    fun `filterByThreshold removes low-relevance snippets`() {
        val snippets = listOf(
            ContextSnippet(source = ContextSource.CONVERSATION, content = "UserRepository fetches users"),
            ContextSnippet(source = ContextSource.CONVERSATION, content = "completely unrelated text about cars"),
        )
        val filtered = RelevanceScorer.filterByThreshold("UserRepository", snippets, threshold = 0.1)
        assertTrue(filtered.all { it.content.contains("User") })
    }

    @Test
    fun `empty query returns zero scores`() {
        val snippet = ContextSnippet(source = ContextSource.CONVERSATION, content = "test")
        assertEquals(0.0, RelevanceScorer.score("", snippet), 0.001)
    }

    @Test
    fun `source weight affects score`() {
        val content = "UserRepository handles users"
        val systemSnippet = ContextSnippet(source = ContextSource.SYSTEM_PROMPT, content = content)
        val convoSnippet = ContextSnippet(source = ContextSource.CONVERSATION, content = content)
        val systemScore = RelevanceScorer.score("UserRepository", systemSnippet)
        val convoScore = RelevanceScorer.score("UserRepository", convoSnippet)
        assertTrue(systemScore != convoScore)
    }

    @Test
    fun `file path bonus applied`() {
        val withPath = ContextSnippet(source = ContextSource.FILE_CONTENT, content = "test", filePath = "Foo.kt")
        val withoutPath = ContextSnippet(source = ContextSource.FILE_CONTENT, content = "test")
        val scoreWithPath = RelevanceScorer.score("test", withPath)
        val scoreWithout = RelevanceScorer.score("test", withoutPath)
        assertTrue(scoreWithPath > scoreWithout)
    }
}
