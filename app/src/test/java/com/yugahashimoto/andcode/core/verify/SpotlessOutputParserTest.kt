package com.yugahashimoto.andcode.core.verify

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SpotlessOutputParserTest {

    @Test
    fun `parseCheckOutput finds files needing formatting`() {
        val output = "Would reformat: /src/Main.kt\nWould reformat: /src/Util.kt"
        val files = SpotlessOutputParser.parseCheckOutput(output)
        assertEquals(2, files.size)
    }

    @Test
    fun `parseCheckOutput returns empty for clean code`() {
        val files = SpotlessOutputParser.parseCheckOutput("Everything up-to-date")
        assertEquals(0, files.size)
    }

    @Test
    fun `isAlreadyFormatted returns true for clean output`() {
        assertTrue(SpotlessOutputParser.isAlreadyFormatted("Everything up-to-date"))
    }

    @Test
    fun `isAlreadyFormatted returns false for dirty output`() {
        assertFalse(SpotlessOutputParser.isAlreadyFormatted("src/Main.kt would change"))
    }

    @Test
    fun `parseApplyOutput counts fixed files`() {
        val output = "Fixed /src/Main.kt\nFixed /src/Util.kt"
        val count = SpotlessOutputParser.parseApplyOutput(output)
        assertEquals(2, count)
    }
}
