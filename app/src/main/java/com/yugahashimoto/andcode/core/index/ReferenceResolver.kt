package com.yugahashimoto.andcode.core.index

class ReferenceResolver(private val index: ProjectIndex) {

    fun findReferences(symbolName: String): List<SymbolEntry> {
        val files = index.invertedIndex[symbolName] ?: return emptyList()
        return files.flatMap { filePath ->
            index.files[filePath]?.symbols?.filter { it.name == symbolName }.orEmpty()
        }
    }

    fun findFilesContaining(word: String): List<String> {
        return index.invertedIndex[word]?.toList().orEmpty()
    }

    fun findDefinition(symbolName: String): SymbolEntry? {
        for (fileIndex in index.files.values) {
            val match = fileIndex.symbols.firstOrNull {
                it.name == symbolName && it.kind != SymbolKind.IMPORT
            }
            if (match != null) return match
        }
        return null
    }

    fun resolveImports(filePath: String, imports: List<String>): Map<String, SymbolEntry?> {
        return imports.associateWith { import ->
            val shortName = import.substringAfterLast('.')
            findDefinition(shortName)
        }
    }

    fun findUsages(filePath: String, symbolName: String): List<SymbolEntry> {
        val fileIndex = index.files[filePath] ?: return emptyList()
        val lineNumbers = fileIndex.references[symbolName] ?: return emptyList()
        return lineNumbers.mapNotNull { line ->
            fileIndex.symbols.firstOrNull { it.name == symbolName && it.line == line }
        }
    }

    fun search(query: SearchQuery): SearchResult {
        val startMillis = System.currentTimeMillis()
        val results = when (query.scope) {
            SearchScope.SYMBOLS -> searchSymbols(query)
            SearchScope.REFERENCES -> searchReferences(query)
            SearchScope.FILES -> searchFiles(query)
            SearchScope.ALL -> searchSymbols(query) + searchReferences(query)
        }
        val tookMillis = System.currentTimeMillis() - startMillis
        return SearchResult(
            query = query.text,
            matches = results.take(query.maxResults),
            tookMillis = tookMillis,
        )
    }

    private fun searchSymbols(query: SearchQuery): List<SymbolEntry> {
        val matches = mutableListOf<SymbolEntry>()
        for (fileIndex in index.files.values) {
            for (symbol in fileIndex.symbols) {
                if (symbol.name.contains(query.text, ignoreCase = true)) {
                    if (query.kindFilter == null || symbol.kind in query.kindFilter) {
                        matches.add(symbol)
                    }
                }
            }
        }
        return matches.sortedBy { it.name.length }
    }

    private fun searchReferences(query: SearchQuery): List<SymbolEntry> {
        val fileNames = index.invertedIndex[query.text] ?: return emptyList()
        return fileNames.flatMap { filePath ->
            index.files[filePath]?.symbols?.filter {
                it.name.contains(query.text, ignoreCase = true)
            }.orEmpty()
        }
    }

    private fun searchFiles(query: SearchQuery): List<SymbolEntry> {
        return index.files.keys
            .filter { it.contains(query.text, ignoreCase = true) }
            .map { path ->
                SymbolEntry(
                    name = path.substringAfterLast('/'),
                    kind = SymbolKind.CLASS,
                    filePath = path,
                    line = 0,
                )
            }
    }
}
