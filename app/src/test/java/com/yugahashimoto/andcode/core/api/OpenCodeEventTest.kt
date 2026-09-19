package com.yugahashimoto.andcode.core.api

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class OpenCodeEventTest {

    @Test
    fun `OpenCodeMessageInfo default role is assistant`() {
        val msg = OpenCodeMessageInfo(id = "1", sessionId = "s1", role = "assistant")
        assertEquals("assistant", msg.role)
    }

    @Test
    fun `OpenCodePart default type is text`() {
        val part = OpenCodePart(id = "p1", sessionId = "s1", messageId = "m1", type = "text")
        assertEquals("text", part.type)
    }

    @Test
    fun `OpenCodeModelLimit defaults to zero`() {
        val limit = OpenCodeModelLimit()
        assertEquals(0L, limit.context)
        assertEquals(0L, limit.input)
        assertEquals(0L, limit.output)
    }

    @Test
    fun `OpenCodeModel defaults to id as name`() {
        val model = OpenCodeModel(id = "gpt-4o")
        assertEquals("gpt-4o", model.name)
    }
}
