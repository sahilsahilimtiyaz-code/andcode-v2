package com.yugahashimoto.andcode.core.index

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ProjectIndexerTest {

    @Test
    fun `index contains source files`() {
        val dir = createTempDir("index-test")
        File(dir, "Main.kt").writeText("class Main { fun run() {} }")
        File(dir, "data.txt").writeText("not indexed")

        val indexer = ProjectIndexer()
        val index = runBlocking { indexer.buildIndex(dir) }

        assertTrue(index.files.containsKey("Main.kt"))
        assertEquals(1, index.fileCount)
        dir.deleteRecursively()
    }

    @Test
    fun `index extracts symbols from kotlin files`() {
        val dir = createTempDir("index-test")
        File(dir, "App.kt").writeText("""
            package com.app
            class App {
                fun start() {}
                val name = "test"
            }
        """.trimIndent())

        val indexer = ProjectIndexer()
        val index = runBlocking { indexer.buildIndex(dir) }

        val appFile = index.files["App.kt"]
        assertNotNull(appFile)
        assertTrue(appFile!!.symbols.any { it.name == "App" && it.kind == SymbolKind.CLASS })
        assertTrue(appFile.symbols.any { it.name == "start" && it.kind == SymbolKind.FUNCTION })
        dir.deleteRecursively()
    }

    @Test
    fun `index builds inverted index`() {
        val dir = createTempDir("index-test")
        File(dir, "A.kt").writeText("class Foo")
        File(dir, "B.kt").writeText("val x = Foo()")

        val indexer = ProjectIndexer()
        val index = runBlocking { indexer.buildIndex(dir) }

        assertTrue(index.invertedIndex.containsKey("Foo"))
        assertEquals(2, index.invertedIndex["Foo"]?.size)
        dir.deleteRecursively()
    }

    @Test
    fun `reindexFile updates single file`() {
        val dir = createTempDir("index-test")
        File(dir, "Edit.kt").writeText("class Old")
        val indexer = ProjectIndexer()
        runBlocking { indexer.buildIndex(dir) }

        File(dir, "Edit.kt").writeText("class New")
        runBlocking { indexer.reindexFile(dir, "Edit.kt") }

        val updated = indexer.index.files["Edit.kt"]
        assertNotNull(updated)
        assertTrue(updated!!.symbols.any { it.name == "New" })
        dir.deleteRecursively()
    }

    @Test
    fun `skips binary and unsupported files`() {
        val dir = createTempDir("index-test")
        File(dir, "image.png").writeBytes(byteArrayOf(0x89.toByte(), 0x50))
        File(dir, "App.kt").writeText("class App")

        val indexer = ProjectIndexer()
        val index = runBlocking { indexer.buildIndex(dir) }

        assertEquals(1, index.fileCount)
        assertTrue(index.files.containsKey("App.kt"))
        dir.deleteRecursively()
    }

    private fun <T> runBlocking(block: suspend () -> T): T {
        return kotlinx.coroutines.runBlocking { block() }
    }
}
