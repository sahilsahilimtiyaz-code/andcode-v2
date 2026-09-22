package com.yugahashimoto.andcode.core.git

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ConflictDetectorTest {

    @Test
    fun `detects single conflict`() {
        val content = """
            line before
            <<<<<<< HEAD
            our changes
            =======
            their changes
            >>>>>>> branch
            line after
        """.trimIndent()
        val conflict = ConflictDetector.detect("Foo.kt", content)
        assertTrue(conflict.hasConflicts)
        assertEquals(1, conflict.conflictCount)
    }

    @Test
    fun `detects multiple conflicts`() {
        val content = """
            <<<<<<< HEAD
            ours1
            =======
            theirs1
            >>>>>>> branch
            middle
            <<<<<<< HEAD
            ours2
            =======
            theirs2
            >>>>>>> branch
        """.trimIndent()
        val conflict = ConflictDetector.detect("Foo.kt", content)
        assertEquals(2, conflict.conflictCount)
    }

    @Test
    fun `no conflict returns empty`() {
        val content = "just normal code\nno conflicts here"
        val conflict = ConflictDetector.detect("Foo.kt", content)
        assertFalse(conflict.hasConflicts)
        assertEquals(0, conflict.conflictCount)
    }

    @Test
    fun `hasConflictMarkers detects markers`() {
        assertTrue(ConflictDetector.hasConflictMarkers("<<<<<<<\n=======\n>>>>>>>"))
        assertFalse(ConflictDetector.hasConflictMarkers("no markers"))
    }

    @Test
    fun `detectInFiles finds conflicts across files`() {
        val files = mapOf(
            "A.kt" to "<<<<<<< HEAD\nours\n=======\ntheirs\n>>>>>>> branch",
            "B.kt" to "clean code",
            "C.kt" to "<<<<<<< HEAD\nconflict\n=======\nother\n>>>>>>> branch",
        )
        val conflicts = ConflictDetector.detectInFiles(files)
        assertEquals(2, conflicts.size)
    }

    @Test
    fun `preserves ours and theirs content`() {
        val content = "<<<<<<< HEAD\nour text\n=======\ntheir text\n>>>>>>> branch"
        val conflict = ConflictDetector.detect("F.kt", content)
        assertEquals("our text", conflict.regions[0].oursContent)
        assertEquals("their text", conflict.regions[0].theirsContent)
    }

    @Test
    fun `line numbers are correct`() {
        val content = "line1\nline2\n<<<<<<< HEAD\nours\n=======\ntheirs\n>>>>>>> branch\nline8"
        val conflict = ConflictDetector.detect("F.kt", content)
        assertEquals(3, conflict.regions[0].startLine)
        assertEquals(7, conflict.regions[0].endLine)
    }
}
