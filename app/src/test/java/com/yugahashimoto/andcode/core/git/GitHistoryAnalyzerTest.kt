package com.yugahashimoto.andcode.core.git

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GitHistoryAnalyzerTest {

    private fun analyzer(vararg outputs: String): GitHistoryAnalyzer {
        var index = 0
        return GitHistoryAnalyzer { _ ->
            outputs.getOrElse(index++) { "" }
        }
    }

    @Test
    fun `log parses commit entries`() = runTest {
        val analyzer = analyzer("abc1234|abc1234|John|2024-01-15|feat: add parser\ndef5678|def5678|Jane|2024-01-16|fix: bug")
        val entries = analyzer.log(10)
        assertEquals(2, entries.size)
        assertEquals("abc1234", entries[0].hash)
        assertEquals("John", entries[0].author)
        assertEquals("feat: add parser", entries[0].subject)
    }

    @Test
    fun `log handles empty output`() = runTest {
        val analyzer = analyzer("")
        val entries = analyzer.log()
        assertEquals(0, entries.size)
    }

    @Test
    fun `currentBranch returns trimmed branch name`() = runTest {
        val analyzer = analyzer("feature/v2-autonomous\n")
        val branch = analyzer.currentBranch()
        assertEquals("feature/v2-autonomous", branch)
    }

    @Test
    fun `status parses porcelain output`() = runTest {
        val analyzer = analyzer("M  src/Main.kt\nA  src/New.kt\n D src/Old.kt")
        val status = analyzer.status()
        assertEquals(3, status.size)
        assertEquals("M", status["src/Main.kt"])
        assertEquals("A", status["src/New.kt"])
        assertEquals("D", status["src/Old.kt"])
    }

    @Test
    fun `diff returns stat output`() = runTest {
        val analyzer = analyzer(" 1 file changed, 10 insertions(+), 5 deletions(-)")
        val result = analyzer.diff()
        assertTrue(result.contains("insertions"))
    }
}
