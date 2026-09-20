package com.yugahashimoto.andcode.feature.premium.engines

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class SymbolInfo(
    val name: String,
    val type: String,
    val file: String,
    val line: Int,
    val description: String = ""
)

data class ProjectIndexState(
    val symbols: List<SymbolInfo> = emptyList(),
    val searchQuery: String = "",
    val searchResults: List<SymbolInfo> = emptyList()
)

class ProjectIndexer {
    private val _state = MutableStateFlow(ProjectIndexState())
    val state: StateFlow<ProjectIndexState> = _state.asStateFlow()

    fun indexSymbols(symbols: List<SymbolInfo>) {
        _state.value = _state.value.copy(symbols = symbols)
    }

    fun search(query: String) {
        val results = if (query.isBlank()) {
            emptyList()
        } else {
            _state.value.symbols.filter {
                it.name.contains(query, ignoreCase = true) ||
                    it.type.contains(query, ignoreCase = true) ||
                    it.description.contains(query, ignoreCase = true)
            }
        }
        _state.value = _state.value.copy(searchQuery = query, searchResults = results)
    }

    fun clearSearch() {
        _state.value = _state.value.copy(searchQuery = "", searchResults = emptyList())
    }
}
