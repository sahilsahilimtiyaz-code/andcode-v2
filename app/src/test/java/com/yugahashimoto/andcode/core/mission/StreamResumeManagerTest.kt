package com.yugahashimoto.andcode.core.mission

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class StreamResumeManagerTest {

    @Test
    fun `getState returns default for unknown session`() {
        val manager = StreamResumeManager()
        val state = manager.getState("session-1")
        assertEquals("session-1", state.sessionId)
        assertFalse(state.connected)
        assertNull(state.lastEventId)
    }

    @Test
    fun `disconnect cancels active stream`() {
        val manager = StreamResumeManager()
        manager.disconnect("session-1")
        val state = manager.getState("session-1")
        assertFalse(state.connected)
    }

    @Test
    fun `appendPartialOutput accumulates text`() {
        val manager = StreamResumeManager()
        manager.appendPartialOutput("s1", "Hello ")
        manager.appendPartialOutput("s1", "World")
        val state = manager.getState("s1")
        assertEquals("Hello World", state.partialOutput)
    }

    @Test
    fun `clearPartialOutput resets text`() {
        val manager = StreamResumeManager()
        manager.appendPartialOutput("s1", "data")
        manager.clearPartialOutput("s1")
        assertEquals("", manager.getState("s1").partialOutput)
    }

    @Test
    fun `destroy cancels all jobs`() {
        val manager = StreamResumeManager()
        manager.destroy()
        val state = manager.getState("any")
        assertFalse(state.connected)
    }

    @Test
    fun `default state has zero reconnect count`() {
        val manager = StreamResumeManager()
        assertEquals(0, manager.getState("s1").reconnectCount)
    }

    @Test
    fun `default state has empty partial output`() {
        val manager = StreamResumeManager()
        assertEquals("", manager.getState("s1").partialOutput)
    }

    @Test
    fun `multiple sessions tracked independently`() {
        val manager = StreamResumeManager()
        manager.appendPartialOutput("s1", "alpha")
        manager.appendPartialOutput("s2", "beta")
        assertEquals("alpha", manager.getState("s1").partialOutput)
        assertEquals("beta", manager.getState("s2").partialOutput)
    }
}
