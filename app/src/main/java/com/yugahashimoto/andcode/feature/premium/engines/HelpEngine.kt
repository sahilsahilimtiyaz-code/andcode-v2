package com.yugahashimoto.andcode.feature.premium.engines

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class HelpTopic(
    val id: String,
    val title: String,
    val content: String,
    val category: String
)

data class HelpState(
    val topics: List<HelpTopic> = emptyList(),
    val searchQuery: String = "",
    val searchResults: List<HelpTopic> = emptyList(),
    val isOnboardingComplete: Boolean = false
)

class HelpEngine {
    private val _state = MutableStateFlow(HelpState())
    val state: StateFlow<HelpState> = _state.asStateFlow()

    init {
        _state.value = HelpState(
            topics = listOf(
                HelpTopic("getting-started", "Getting Started", "Learn the basics of AndCode", "Basics"),
                HelpTopic("agents", "AI Agents", "Understanding different AI agents", "Features"),
                HelpTopic("missions", "Missions", "How to use the mission system", "Features"),
                HelpTopic("builds", "Build System", "Building and compiling your projects", "Features"),
                HelpTopic("git", "Git Integration", "Using git features in AndCode", "Features"),
                HelpTopic("shortcuts", "Keyboard Shortcuts", "Quick actions and shortcuts", "Tips"),
                HelpTopic("troubleshooting", "Troubleshooting", "Common issues and solutions", "Support")
            )
        )
    }

    fun search(query: String) {
        val results = if (query.isBlank()) {
            emptyList()
        } else {
            _state.value.topics.filter {
                it.title.contains(query, ignoreCase = true) ||
                    it.content.contains(query, ignoreCase = true)
            }
        }
        _state.value = _state.value.copy(searchQuery = query, searchResults = results)
    }

    fun completeOnboarding() {
        _state.value = _state.value.copy(isOnboardingComplete = true)
    }

    fun getTopicsByCategory(category: String): List<HelpTopic> {
        return _state.value.topics.filter { it.category == category }
    }
}
