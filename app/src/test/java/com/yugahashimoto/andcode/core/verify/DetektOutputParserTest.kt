package com.yugahashimoto.andcode.core.verify

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DetektOutputParserTest {

    @Test
    fun `parseText finds violations`() {
        val output = "/src/Main.kt:10:5: warning - Unused import [unused-imports]"
        val violations = DetektOutputParser.parseText(output)
        assertEquals(1, violations.size)
        assertEquals("/src/Main.kt", violations[0].file)
        assertEquals(10, violations[0].line)
        assertEquals("unused-imports", violations[0].rule)
    }

    @Test
    fun `parseText handles multiple violations`() {
        val output = "/src/A.kt:1:1: error - Bad code [style]\n/src/B.kt:2:2: warning - Deprecated [deprecation]"
        val violations = DetektOutputParser.parseText(output)
        assertEquals(2, violations.size)
    }

    @Test
    fun `parseText handles empty output`() {
        val violations = DetektOutputParser.parseText("")
        assertEquals(0, violations.size)
    }

    @Test
    fun `parseText distinguishes error and warning severity`() {
        val output = "/src/A.kt:1:1: error - Problem [rule]\n/src/B.kt:2:2: warning - Issue [rule2]"
        val violations = DetektOutputParser.parseText(output)
        assertEquals(VerifySeverity.ERROR, violations[0].severity)
        assertEquals(VerifySeverity.WARNING, violations[1].severity)
    }

    @Test
    fun `parseSarif extracts from JSON-like output`() {
        val output = """{"ruleId":"no-var","message":"Use val instead","locations":[{"physicalLocation":{"artifactLocation":{"uri":"Main.kt"},"region":{"startLine":5,"startColumn":1}}}]}"""
        val violations = DetektOutputParser.parseSarif(output)
        assertEquals(1, violations.size)
        assertEquals("no-var", violations[0].rule)
        assertEquals("Main.kt", violations[0].file)
        assertEquals(5, violations[0].line)
    }
}
