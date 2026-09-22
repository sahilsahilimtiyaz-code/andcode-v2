package com.yugahashimoto.andcode.core.help

class HelpEngine {
    private val articles = mutableMapOf<String, HelpArticle>()
    private val steps = mutableListOf<OnboardingStep>()
    private var progress = OnboardingProgress()

    init {
        articles.putAll(defaultArticles())
        steps.addAll(defaultOnboardingSteps())
    }

    fun getArticles(): List<HelpArticle> = articles.values.toList()

    fun getArticleById(id: String): HelpArticle? = articles[id]

    fun getArticlesByCategory(category: HelpCategory): List<HelpArticle> =
        articles.values.filter { it.category == category }

    fun searchArticles(query: String): List<SearchResult> {
        val lowerQuery = query.lowercase()
        return articles.values.mapNotNull { article ->
            val titleMatch = article.title.lowercase().contains(lowerQuery)
            val contentMatch = article.content.lowercase().contains(lowerQuery)
            val tagMatch = article.tags.any { it.lowercase().contains(lowerQuery) }
            if (titleMatch || contentMatch || tagMatch) {
                val score = when {
                    titleMatch -> 1.0
                    tagMatch -> 0.7
                    contentMatch -> 0.5
                    else -> 0.0
                }
                val snippet = extractSnippet(article.content, lowerQuery)
                SearchResult(article, score, snippet)
            } else null
        }.sortedByDescending { it.relevanceScore }
    }

    fun recordArticleView(articleId: String) {
        articles[articleId]?.let {
            articles[articleId] = it.copy(viewCount = it.viewCount + 1)
        }
    }

    fun getOnboardingSteps(): List<OnboardingStep> = steps.toList()

    fun getOnboardingProgress(): OnboardingProgress = progress

    fun completeStep(stepId: String) {
        progress = progress.copy(
            completedSteps = progress.completedSteps + stepId,
            skippedSteps = progress.skippedSteps - stepId,
        )
        updateCurrentStep()
        checkCompletion()
    }

    fun skipStep(stepId: String) {
        progress = progress.copy(
            skippedSteps = progress.skippedSteps + stepId,
            completedSteps = progress.completedSteps - stepId,
        )
        updateCurrentStep()
        checkCompletion()
    }

    fun startOnboarding() {
        progress = OnboardingProgress(startedAt = System.currentTimeMillis())
        updateCurrentStep()
    }

    fun dismissOnboarding() {
        progress = progress.copy(completedAt = System.currentTimeMillis())
    }

    fun resetOnboarding() {
        progress = OnboardingProgress()
        steps.forEachIndexed { i, step ->
            steps[i] = step.copy(isCompleted = false)
        }
    }

    fun getMostViewedArticles(limit: Int = 5): List<HelpArticle> =
        articles.values.sortedByDescending { it.viewCount }.take(limit)

    fun getRelatedArticles(articleId: String): List<HelpArticle> {
        val article = articles[articleId] ?: return emptyList()
        return article.relatedArticleIds.mapNotNull { articles[it] }
    }

    private fun updateCurrentStep() {
        val incomplete = steps.filter {
            it.id !in progress.completedSteps && it.id !in progress.skippedSteps
        }
        progress = progress.copy(currentStepId = incomplete.firstOrNull()?.id)
    }

    private fun checkCompletion() {
        val allHandled = steps.all {
            it.id in progress.completedSteps || it.id in progress.skippedSteps
        }
        if (allHandled && progress.completedAt == null) {
            progress = progress.copy(completedAt = System.currentTimeMillis())
        }
    }

    private fun extractSnippet(content: String, query: String): String {
        val idx = content.lowercase().indexOf(query)
        if (idx < 0) return content.take(150)
        val start = maxOf(0, idx - 40)
        val end = minOf(content.length, idx + query.length + 60)
        val prefix = if (start > 0) "..." else ""
        val suffix = if (end < content.length) "..." else ""
        return prefix + content.substring(start, end) + suffix
    }

    companion object {
        fun defaultArticles() = mapOf(
            "getting_started" to HelpArticle(
                id = "getting_started",
                title = "Getting Started with AndCode",
                category = HelpCategory.GETTING_STARTED,
                content = "AndCode lets you run AI coding agents directly on your Android device. " +
                    "Start by choosing a runtime (OpenCode, Claude Code, or Antigravity) during onboarding. " +
                    "Each agent has different capabilities and pricing models.",
                tags = listOf("intro", "setup", "first-time"),
                relatedArticleIds = listOf("workspace_basics", "agent_selection"),
            ),
            "workspace_basics" to HelpArticle(
                id = "workspace_basics",
                title = "Working with Workspaces",
                category = HelpCategory.WORKSPACE,
                content = "A workspace is a project directory that your AI agent can access. " +
                    "You can create workspaces from local folders, clone git repositories, " +
                    "or import from external storage. Each workspace maintains its own git state " +
                    "and file history.",
                tags = listOf("workspace", "project", "files"),
                relatedArticleIds = listOf("getting_started", "terminal_basics"),
            ),
            "terminal_basics" to HelpArticle(
                id = "terminal_basics",
                title = "Using the Terminal",
                category = HelpCategory.TERMINAL,
                content = "The built-in terminal lets you run shell commands inside the PRoot Linux environment. " +
                    "Commands run in a sandboxed Alpine Linux system with common tools like git, curl, and ripgrep. " +
                    "You can also use the automation features to chain commands together.",
                tags = listOf("terminal", "shell", "commands"),
                relatedArticleIds = listOf("workspace_basics", "automation_scripts"),
            ),
            "build_system" to HelpArticle(
                id = "build_system",
                title = "Build System Integration",
                category = HelpCategory.BUILD_SYSTEM,
                content = "AndCode integrates with Gradle for Android builds. " +
                    "The Build Lab provides build profiles, history tracking, and error classification. " +
                    "Build errors are automatically categorized by type (compilation, dependency, resource) " +
                    "with fix suggestions.",
                tags = listOf("gradle", "build", "compile", "error"),
                relatedArticleIds = listOf("troubleshooting_build"),
            ),
            "agent_selection" to HelpArticle(
                id = "agent_selection",
                title = "Choosing an AI Agent",
                category = HelpCategory.AI_AGENTS,
                content = "AndCode supports three AI coding agents:\n" +
                    "- OpenCode: Full-featured, supports MCP servers\n" +
                    "- Claude Code: Anthropic's agent with permission controls\n" +
                    "- Antigravity: Google's agent with Debian rootfs support\n" +
                    "Each agent can be configured independently with different API keys and models.",
                tags = listOf("agent", "opencode", "claude", "antigravity"),
                relatedArticleIds = listOf("getting_started", "mcp_setup"),
            ),
            "troubleshooting_build" to HelpArticle(
                id = "troubleshooting_build",
                title = "Troubleshooting Build Errors",
                category = HelpCategory.TROUBLESHOOTING,
                content = "Common build error causes:\n" +
                    "1. Missing SDK components - ensure Android SDK is properly configured\n" +
                    "2. Dependency conflicts - check for duplicate class errors\n" +
                    "3. Memory issues - increase JVM heap with -Xmx flag\n" +
                    "4. Network issues - verify Maven repository access\n" +
                    "Use the Diagnostics tool to check system health.",
                tags = listOf("troubleshoot", "error", "fix", "build"),
                relatedArticleIds = listOf("build_system"),
            ),
            "performance_monitoring" to HelpArticle(
                id = "performance_monitoring",
                title = "Performance Monitoring",
                category = HelpCategory.TROUBLESHOOTING,
                content = "The Performance Monitor tracks CPU, memory, disk usage, and runtime health. " +
                    "Alerts are generated when metrics exceed configurable thresholds. " +
                    "Use the trend analysis to detect degrading performance over time.",
                tags = listOf("performance", "monitor", "cpu", "memory"),
            ),
        )

        fun defaultOnboardingSteps() = listOf(
            OnboardingStep(
                id = "welcome",
                title = "Welcome to AndCode",
                description = "Your AI coding assistant for Android. Let's get you set up.",
                type = StepType.INFO,
                order = 0,
                isSkippable = false,
            ),
            OnboardingStep(
                id = "choose_runtime",
                title = "Choose a Runtime",
                description = "Select which AI agent to use. You can change this later in Settings.",
                type = StepType.ACTION,
                order = 1,
                actionLabel = "Configure Runtime",
                deepLink = "workspace/local-runtime-management",
            ),
            OnboardingStep(
                id = "create_workspace",
                title = "Create Your First Workspace",
                description = "A workspace is where your code lives. Create one to get started.",
                type = StepType.ACTION,
                order = 2,
                actionLabel = "Create Workspace",
                deepLink = "workspace/workspaces",
            ),
            OnboardingStep(
                id = "try_terminal",
                title = "Try the Terminal",
                description = "Run shell commands in the embedded terminal. Try 'git status' or 'ls'.",
                type = StepType.ACTION,
                order = 3,
                actionLabel = "Open Terminal",
                deepLink = "workspace/terminal",
            ),
            OnboardingStep(
                id = "explore_settings",
                title = "Explore Settings",
                description = "Customize AndCode to your preferences. Set up API keys, themes, and more.",
                type = StepType.INFO,
                order = 4,
                isSkippable = true,
            ),
        )
    }
}
