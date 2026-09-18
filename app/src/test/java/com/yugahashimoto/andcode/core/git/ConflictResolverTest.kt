package com.yugahashimoto.andcode.core.git

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ConflictResolverTest {

    private val conflictContent = """
        before
        <<<<<<< HEAD
        ours content
        =======
        theirs content
        >>>>>>> branch
        after
    """.trimIndent()

    private val conflict = MergeConflict(
        filePath = "Foo.kt",
        regions = listOf(
            ConflictRegion(
                startLine = 2,
                endLine = 7,
                oursContent = "ours content",
                theirsContent = "theirs content",
            )
        ),
        rawContent = conflictContent,
    )

    private val resolver = ConflictResolver()

    @Test
    fun `TAKE_OURS keeps our content`() {
        val result = resolver.resolve(conflict, ResolutionStrategy.TAKE_OURS)
        assertTrue(result.resolvedContent.contains("ours content"))
        assertFalse(result.resolvedContent.contains("theirs content"))
        assertFalse(result.resolvedContent.contains("<<<<<<<"))
    }

    @Test
    fun `TAKE_THEIRS keeps their content`() {
        val result = resolver.resolve(conflict, ResolutionStrategy.TAKE_THEIRS)
        assertTrue(result.resolvedContent.contains("theirs content"))
        assertFalse(result.resolvedContent.contains("ours content"))
        assertFalse(result.resolvedContent.contains(">>>>>>>"))
    }

    @Test
    fun `UNION keeps both contents`() {
        val result = resolver.resolve(conflict, ResolutionStrategy.UNION)
        assertTrue(result.resolvedContent.contains("ours content"))
        assertTrue(result.resolvedContent.contains("theirs content"))
        assertFalse(result.resolvedContent.contains("======="))
    }

    @Test
    fun `preserves surrounding lines`() {
        val result = resolver.resolve(conflict, ResolutionStrategy.TAKE_OURS)
        assertTrue(result.resolvedContent.contains("before"))
        assertTrue(result.resolvedContent.contains("after"))
    }

    @Test
    fun `resolveAll handles multiple conflicts`() {
        val conflicts = listOf(
            conflict,
            MergeConflict(
                filePath = "Bar.kt",
                regions = listOf(ConflictRegion(1, 5, "a", "b")),
                rawContent = "<<<<<<< HEAD\na\n=======\nb\n>>>>>>> branch",
            ),
        )
        val results = resolver.resolveAll(conflicts, ResolutionStrategy.TAKE_OURS)
        assertEquals(2, results.size)
        assertTrue(results.all { it.strategy == ResolutionStrategy.TAKE_OURS })
    }

    @Test
    fun `resolution records metadata`() {
        val result = resolver.resolve(conflict, ResolutionStrategy.TAKE_OURS)
        assertEquals("Foo.kt", result.filePath)
        assertEquals(ResolutionStrategy.TAKE_OURS, result.strategy)
        assertTrue(result.timestampMillis > 0)
    }
}
