package com.yugahashimoto.andcode.core.index

import kotlinx.serialization.Serializable

enum class SymbolKind {
    CLASS,
    INTERFACE,
    FUNCTION,
    PROPERTY,
    ENUM_ENTRY,
    ANNOTATION,
    IMPORT,
    PACKAGE,
}

@Serializable
data class SymbolEntry(
    val name: String,
    val kind: SymbolKind,
    val filePath: String,
    val line: Int,
    val column: Int = 0,
    val snippet: String = "",
)

@Serializable
data class FileIndex(
    val path: String,
    val lastModifiedMillis: Long,
    val symbols: List<SymbolEntry> = emptyList(),
    val references: Map<String, List<Int>> = emptyMap(),
)

@Serializable
data class ProjectIndex(
    val rootPath: String,
    val files: Map<String, FileIndex> = emptyMap(),
    val invertedIndex: Map<String, Set<String>> = emptyMap(),
    val lastFullIndexMillis: Long = 0L,
) {
    val symbolCount: Int get() = files.values.sumOf { it.symbols.size }
    val fileCount: Int get() = files.size
}

enum class SearchScope { ALL, SYMBOLS, REFERENCES, FILES }

data class SearchQuery(
    val text: String,
    val scope: SearchScope = SearchScope.ALL,
    val kindFilter: Set<SymbolKind>? = null,
    val maxResults: Int = 50,
)

data class SearchResult(
    val query: String,
    val matches: List<SymbolEntry> = emptyList(),
    val fileMatches: List<String> = emptyList(),
    val tookMillis: Long = 0L,
)
