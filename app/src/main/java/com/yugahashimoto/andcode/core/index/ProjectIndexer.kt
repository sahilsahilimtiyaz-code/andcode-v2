package com.yugahashimoto.andcode.core.index

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class ProjectIndexer(
    private val store: ProjectIndexStore? = null,
) {
    private var currentIndex = ProjectIndex(rootPath = "")

    val index: ProjectIndex get() = currentIndex

    suspend fun buildIndex(rootDir: File): ProjectIndex = withContext(Dispatchers.IO) {
        val existing = store?.load(rootDir.absolutePath) ?: ProjectIndex(rootPath = rootDir.absolutePath)
        val filesToIndex = mutableMapOf<String, FileIndex>()
        val invertedIndex = mutableMapOf<String, MutableSet<String>>()

        val sourceFiles = rootDir.walkTopDown()
            .filter { it.isFile && it.extension in SUPPORTED_EXTENSIONS }
            .toList()

        for (file in sourceFiles) {
            val relativePath = file.relativeTo(rootDir).path
            val lastModified = file.lastModified()
            val cached = existing.files[relativePath]

            if (cached != null && cached.lastModifiedMillis >= lastModified) {
                filesToIndex[relativePath] = cached
                for ((word, lines) in cached.references) {
                    invertedIndex.getOrPut(word) { mutableSetOf() }.add(relativePath)
                }
                continue
            }

            try {
                val content = file.readText()
                val symbols = SymbolExtractor.extract(relativePath, content)
                val references = SymbolExtractor.extractReferences(relativePath, content)
                val fileIndex = FileIndex(
                    path = relativePath,
                    lastModifiedMillis = lastModified,
                    symbols = symbols,
                    references = references,
                )
                filesToIndex[relativePath] = fileIndex
                for ((word, _) in references) {
                    invertedIndex.getOrPut(word) { mutableSetOf() }.add(relativePath)
                }
            } catch (_: Exception) {
                // Skip unreadable files
            }
        }

        currentIndex = ProjectIndex(
            rootPath = rootDir.absolutePath,
            files = filesToIndex,
            invertedIndex = invertedIndex,
            lastFullIndexMillis = System.currentTimeMillis(),
        )

        store?.save(currentIndex)
        currentIndex
    }

    suspend fun reindexFile(rootDir: File, relativePath: String): FileIndex? = withContext(Dispatchers.IO) {
        val file = File(rootDir, relativePath)
        if (!file.isFile) {
            currentIndex = currentIndex.copy(
                files = currentIndex.files - relativePath,
            )
            return@withContext null
        }

        try {
            val content = file.readText()
            val symbols = SymbolExtractor.extract(relativePath, content)
            val references = SymbolExtractor.extractReferences(relativePath, content)
            val fileIndex = FileIndex(
                path = relativePath,
                lastModifiedMillis = file.lastModified(),
                symbols = symbols,
                references = references,
            )
            currentIndex = currentIndex.copy(
                files = currentIndex.files + (relativePath to fileIndex),
            )
            fileIndex
        } catch (_: Exception) {
            null
        }
    }

    companion object {
        val SUPPORTED_EXTENSIONS = setOf(
            "kt", "java", "xml", "gradle", "kts", "json", "yaml", "yml",
            "toml", "properties", "md", "txt", "py", "js", "ts",
        )
    }
}
