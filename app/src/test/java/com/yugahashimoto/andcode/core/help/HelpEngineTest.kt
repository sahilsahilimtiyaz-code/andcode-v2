package com.yugahashimoto.andcode.core.help

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class HelpEngineTest {

    private lateinit var engine: HelpEngine

    @Before
    fun setUp() {
        engine = HelpEngine()
    }

    @Test
    fun `getArticles returns default articles`() {
        val articles = engine.getArticles()
        assertTrue(articles.isNotEmpty())
        assertTrue(articles.any { it.id == "getting_started" })
        assertTrue(articles.any { it.id == "workspace_basics" })
        assertTrue(articles.any { it.id == "terminal_basics" })
    }

    @Test
    fun `getArticleById returns correct article`() {
        val article = engine.getArticleById("getting_started")
        assertNotNull(article)
        assertEquals("Getting Started with AndCode", article!!.title)
        assertEquals(HelpCategory.GETTING_STARTED, article.category)
    }

    @Test
    fun `getArticleById returns null for unknown`() {
        assertNull(engine.getArticleById("nonexistent"))
    }

    @Test
    fun `getArticlesByCategory filters correctly`() {
        val articles = engine.getArticlesByCategory(HelpCategory.TROUBLESHOOTING)
        assertTrue(articles.all { it.category == HelpCategory.TROUBLESHOOTING })
        assertTrue(articles.isNotEmpty())
    }

    @Test
    fun `searchArticles finds by title`() {
        val results = engine.searchArticles("terminal")
        assertTrue(results.isNotEmpty())
        assertTrue(results.any { it.article.id == "terminal_basics" })
    }

    @Test
    fun `searchArticles finds by content`() {
        val results = engine.searchArticles("Gradle")
        assertTrue(results.isNotEmpty())
    }

    @Test
    fun `searchArticles finds by tags`() {
        val results = engine.searchArticles("opencode")
        assertTrue(results.isNotEmpty())
    }

    @Test
    fun `searchArticles returns empty for no match`() {
        val results = engine.searchArticles("xyznonexistent")
        assertTrue(results.isEmpty())
    }

    @Test
    fun `searchArticles ranks title matches highest`() {
        val results = engine.searchArticles("workspace")
        val workspaceArticle = results.find { it.article.id == "workspace_basics" }
        assertNotNull(workspaceArticle)
        assertEquals(1.0, workspaceArticle!!.relevanceScore, 0.01)
    }

    @Test
    fun `searchArticles provides snippet`() {
        val results = engine.searchArticles("Gradle")
        assertTrue(results.any { it.matchedSnippet.isNotBlank() })
    }

    @Test
    fun `recordArticleView increments viewCount`() {
        engine.recordArticleView("getting_started")
        engine.recordArticleView("getting_started")
        val article = engine.getArticleById("getting_started")!!
        assertEquals(2, article.viewCount)
    }

    @Test
    fun `getMostViewedArticles returns top N`() {
        engine.recordArticleView("getting_started")
        engine.recordArticleView("getting_started")
        engine.recordArticleView("workspace_basics")
        val top = engine.getMostViewedArticles(1)
        assertEquals(1, top.size)
        assertEquals("getting_started", top.first().id)
    }

    @Test
    fun `getRelatedArticles returns linked articles`() {
        val related = engine.getRelatedArticles("getting_started")
        assertTrue(related.isNotEmpty())
        assertTrue(related.any { it.id == "workspace_basics" })
    }

    @Test
    fun `getRelatedArticles returns empty for unknown id`() {
        assertTrue(engine.getRelatedArticles("nonexistent").isEmpty())
    }

    @Test
    fun `getOnboardingSteps returns default steps`() {
        val steps = engine.getOnboardingSteps()
        assertEquals(5, steps.size)
        assertEquals("welcome", steps.first().id)
    }

    @Test
    fun `startOnboarding resets progress`() {
        engine.completeStep("welcome")
        engine.startOnboarding()
        val progress = engine.getOnboardingProgress()
        assertTrue(progress.completedSteps.isEmpty())
        assertNotNull(progress.startedAt)
    }

    @Test
    fun `completeStep marks step as completed`() {
        engine.startOnboarding()
        engine.completeStep("welcome")
        val progress = engine.getOnboardingProgress()
        assertTrue("welcome" in progress.completedSteps)
    }

    @Test
    fun `skipStep marks step as skipped`() {
        engine.startOnboarding()
        engine.skipStep("welcome")
        val progress = engine.getOnboardingProgress()
        assertTrue("welcome" in progress.skippedSteps)
    }

    @Test
    fun `completeStep updates currentStepId`() {
        engine.startOnboarding()
        engine.completeStep("welcome")
        val progress = engine.getOnboardingProgress()
        assertEquals("choose_runtime", progress.currentStepId)
    }

    @Test
    fun `dismissOnboarding sets completedAt`() {
        engine.startOnboarding()
        engine.dismissOnboarding()
        val progress = engine.getOnboardingProgress()
        assertNotNull(progress.completedAt)
        assertTrue(progress.isComplete)
    }

    @Test
    fun `resetOnboarding clears all progress`() {
        engine.startOnboarding()
        engine.completeStep("welcome")
        engine.skipStep("choose_runtime")
        engine.resetOnboarding()
        val progress = engine.getOnboardingProgress()
        assertTrue(progress.completedSteps.isEmpty())
        assertTrue(progress.skippedSteps.isEmpty())
        assertNull(progress.completedAt)
    }

    @Test
    fun `progressPercent calculates correctly`() {
        engine.startOnboarding()
        engine.completeStep("welcome")
        engine.completeStep("choose_runtime")
        engine.skipStep("create_workspace")
        val progress = engine.getOnboardingProgress()
        assertEquals(2.0 / 3.0 * 100, progress.progressPercent, 1.0)
    }
}
