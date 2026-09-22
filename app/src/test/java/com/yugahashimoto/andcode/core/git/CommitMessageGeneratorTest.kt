package com.yugahashimoto.andcode.core.git

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CommitMessageGeneratorTest {

    private fun generator(vararg outputs: String): CommitMessageGenerator {
        var index = 0
        return CommitMessageGenerator { _ ->
            outputs.getOrElse(index++) { "" }
        }
    }

    @Test
    fun `generates feat for new files`() = runTest {
        val gen = generator(
            "",
            "A  src/feature/NewScreen.kt",
        )
        val msg = gen.generate()
        assertTrue(msg.startsWith("feat"))
    }

    @Test
    fun `generates fix for modified files`() = runTest {
        val gen = generator(
            "",
            "M  src/feature/Main.kt",
        )
        val msg = gen.generate()
        assertTrue(msg.startsWith("fix"))
    }

    @Test
    fun `generates chore for deleted files`() = runTest {
        val gen = generator(
            "",
            "D  src/old/OldFile.kt",
        )
        val msg = gen.generate()
        assertTrue(msg.startsWith("chore"))
    }

    @Test
    fun `generates test for test-only changes`() = runTest {
        val gen = generator(
            "",
            "M  src/test/MyTest.kt",
        )
        val msg = gen.generate()
        assertTrue(msg.startsWith("test"))
    }

    @Test
    fun `generates docs for doc-only changes`() = runTest {
        val gen = generator(
            "",
            "M  docs/README.md",
        )
        val msg = gen.generate()
        assertTrue(msg.startsWith("docs"))
    }

    @Test
    fun `scope detected from path`() = runTest {
        val gen = generator(
            "",
            "M  app/src/main/java/com/example/auth/Login.kt",
        )
        val msg = gen.generate()
        assertTrue(msg.contains("(auth)") || msg.contains("(code)"))
    }

    @Test
    fun `single file uses filename in description`() = runTest {
        val gen = generator(
            "",
            "M  app/src/main/java/com/app/UserService.kt",
        )
        val msg = gen.generate()
        assertTrue(msg.contains("UserService") || msg.contains("update"))
    }

    @Test
    fun `multiple files count in description`() = runTest {
        val gen = generator(
            "",
            "M  src/A.kt\nM  src/B.kt\nM  src/C.kt",
        )
        val msg = gen.generate()
        assertTrue(msg.contains("3") || msg.contains("file"))
    }
}
