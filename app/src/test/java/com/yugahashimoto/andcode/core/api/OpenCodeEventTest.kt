package com.yugahashimoto.andcode.core.api

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class OpenCodeEventTest {

    @Test
    fun `MessageInfo default role is assistant`() {
        val msg = MessageInfo(id = "1", sessionId = "s1")
        assertEquals("assistant", msg.role)
    }

    @Test
    fun `MessagePartInfo default type is text`() {
        val part = MessagePartInfo(id = "p1", sessionId = "s1", messageId = "m1")
        assertEquals("text", part.type)
    }

    @Test
    fun `ModelLimit defaults to zero`() {
        val limit = ModelLimit()
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
