package com.yugahashimoto.andcode.core.index

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ReferenceResolverTest {

    private lateinit var index: ProjectIndex
    private lateinit var resolver: ReferenceResolver

    @Before
    fun setup() {
        index = ProjectIndex(
            rootPath = "/test",
            files = mapOf(
                "Main.kt" to FileIndex(
                    path = "Main.kt",
                    lastModifiedMillis = 0L,
                    symbols = listOf(
                        SymbolEntry("UserService", SymbolKind.CLASS, "Main.kt", 1),
                        SymbolEntry("getUser", SymbolKind.FUNCTION, "Main.kt", 5),
                    ),
                    references = mapOf("UserService" to listOf(1, 10, 15), "getUser" to listOf(5)),
                ),
                "Test.kt" to FileIndex(
                    path = "Test.kt",
                    lastModifiedMillis = 0L,
                    symbols = listOf(
                        SymbolEntry("UserService", SymbolKind.CLASS, "Test.kt", 1),
                        SymbolEntry("OtherClass", SymbolKind.CLASS, "Test.kt", 5),
                    ),
                    references = mapOf("UserService" to listOf(1)),
                ),
            ),
            invertedIndex = mapOf(
                "UserService" to setOf("Main.kt", "Test.kt"),
                "getUser" to setOf("Main.kt"),
                "OtherClass" to setOf("Test.kt"),
            ),
        )
        resolver = ReferenceResolver(index)
    }

    @Test
    fun `findReferences returns all occurrences across files`() {
        val refs = resolver.findReferences("UserService")
        assertEquals(3, refs.size)
    }

    @Test
    fun `findFilesContaining returns files with word`() {
        val files = resolver.findFilesContaining("UserService")
        assertEquals(2, files.size)
        assertTrue(files.contains("Main.kt"))
        assertTrue(files.contains("Test.kt"))
    }

    @Test
    fun `findDefinition returns first non-import symbol`() {
        val def = resolver.findDefinition("UserService")
        assertNotNull(def)
        assertEquals("Main.kt", def!!.filePath)
    }

    @Test
    fun `findDefinition returns null for unknown symbol`() {
        assertNull(resolver.findDefinition("NonExistent"))
    }

    @Test
    fun `search finds matching symbols`() {
        val result = resolver.search(SearchQuery("User", SearchScope.SYMBOLS))
        assertTrue(result.matches.isNotEmpty())
        assertTrue(result.matches.all { it.name.contains("User") })
    }

    @Test
    fun `search respects kind filter`() {
        val result = resolver.search(
            SearchQuery("User", SearchScope.SYMBOLS, kindFilter = setOf(SymbolKind.FUNCTION))
        )
        assertTrue(result.matches.isEmpty())
    }

    @Test
    fun `search respects maxResults`() {
        val result = resolver.search(SearchQuery("User", SearchScope.ALL, maxResults = 1))
        assertTrue(result.matches.size <= 1)
    }

    @Test
    fun `search files scope finds file paths`() {
        val result = resolver.search(SearchQuery("Main", SearchScope.FILES))
        assertTrue(result.fileMatches.isNotEmpty())
    }
}
