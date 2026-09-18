package com.yugahashimoto.andcode.core.reliability

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class MissionStateEntityTest {

    @Test
    fun `default values are correct`() {
        val entity = MissionStateEntity(missionId = "test-1")
        assertEquals("test-1", entity.missionId)
        assertEquals(0, entity.currentStep)
        assertEquals(12, entity.totalSteps)
        assertEquals("PENDING", entity.status)
        assertNull(entity.checkpoint)
        assertEquals("[]", entity.errorHistory)
        assertNotNull(entity.createdAt)
        assertNotNull(entity.updatedAt)
    }

    @Test
    fun `entity preserves all fields`() {
        val now = System.currentTimeMillis()
        val entity = MissionStateEntity(
            missionId = "mission-42",
            currentStep = 5,
            totalSteps = 12,
            status = "RUNNING",
            checkpoint = "3",
            errorHistory = """["error1","error2"]""",
            createdAt = now,
            updatedAt = now,
        )
        assertEquals("mission-42", entity.missionId)
        assertEquals(5, entity.currentStep)
        assertEquals(12, entity.totalSteps)
        assertEquals("RUNNING", entity.status)
        assertEquals("3", entity.checkpoint)
        assertEquals("""["error1","error2"]""", entity.errorHistory)
        assertEquals(now, entity.createdAt)
        assertEquals(now, entity.updatedAt)
    }

    @Test
    fun `copy creates independent instance`() {
        val original = MissionStateEntity(missionId = "m1", currentStep = 3)
        val copy = original.copy(currentStep = 7, status = "RUNNING")
        assertEquals(3, original.currentStep)
        assertEquals("PENDING", original.status)
        assertEquals(7, copy.currentStep)
        assertEquals("RUNNING", copy.status)
    }

    @Test
    fun `step progression through states`() {
        var entity = MissionStateEntity(missionId = "m1")
        assertEquals(0, entity.currentStep)
        assertEquals("PENDING", entity.status)

        entity = entity.copy(status = "RUNNING", currentStep = 1)
        assertEquals(1, entity.currentStep)
        assertEquals("RUNNING", entity.status)

        entity = entity.copy(checkpoint = "1")
        assertEquals("1", entity.checkpoint)

        entity = entity.copy(currentStep = 6, checkpoint = "6")
        assertEquals(6, entity.currentStep)
        assertEquals("6", entity.checkpoint)

        entity = entity.copy(status = "COMPLETED", currentStep = 12)
        assertEquals("COMPLETED", entity.status)
        assertEquals(12, entity.currentStep)
    }

    @Test
    fun `error history is a JSON array string`() {
        val errors = """["timeout","crash","oom"]"""
        val entity = MissionStateEntity(missionId = "m1", errorHistory = errors)
        assertEquals(errors, entity.errorHistory)
    }
}
