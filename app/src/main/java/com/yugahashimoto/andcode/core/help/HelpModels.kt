package com.yugahashimoto.andcode.core.help

data class HelpArticle(
    val id: String,
    val title: String,
    val category: HelpCategory,
    val content: String,
    val tags: List<String> = emptyList(),
    val relatedArticleIds: List<String> = emptyList(),
    val lastUpdatedMillis: Long = System.currentTimeMillis(),
    val viewCount: Int = 0,
)

enum class HelpCategory {
    GETTING_STARTED,
    WORKSPACE,
    TERMINAL,
    BUILD_SYSTEM,
    SETTINGS,
    TROUBLESHOOTING,
    AI_AGENTS,
    KEYBOARD_SHORTCUTS,
}

data class OnboardingStep(
    val id: String,
    val title: String,
    val description: String,
    val type: StepType,
    val isCompleted: Boolean = false,
    val isSkippable: Boolean = true,
    val actionLabel: String? = null,
    val deepLink: String? = null,
    val order: Int = 0,
)

enum class StepType { INFO, ACTION, CHECKLIST, VIDEO }

data class OnboardingProgress(
    val completedSteps: Set<String> = emptySet(),
    val skippedSteps: Set<String> = emptySet(),
    val startedAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
    val currentStepId: String? = null,
) {
    val isComplete: Boolean get() = completedAt != null
    val progressPercent: Double get() {
        val total = completedSteps.size + skippedSteps.size
        return if (total > 0) completedSteps.size.toDouble() / maxOf(total, 1) * 100 else 0.0
    }
}

data class SearchResult(
    val article: HelpArticle,
    val relevanceScore: Double,
    val matchedSnippet: String,
)

data class HelpState(
    val articles: List<HelpArticle> = emptyList(),
    val onboardingSteps: List<OnboardingStep> = emptyList(),
    val onboardingProgress: OnboardingProgress = OnboardingProgress(),
    val searchResults: List<SearchResult> = emptyList(),
    val lastViewedArticleId: String? = null,
    val isOnboardingDismissed: Boolean = false,
)
