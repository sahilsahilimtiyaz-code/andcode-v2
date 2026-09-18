package com.yugahashimoto.andcode.core.reliability

import com.yugahashimoto.andcode.data.local.MissionStateDao
import com.yugahashimoto.andcode.data.local.MissionStateEntity
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RecoveryManagerTest {

    private class FakeMissionStateDao : MissionStateDao {
        private val store = mutableMapOf<String, MissionStateEntity>()

        override suspend fun getById(missionId: String): MissionStateEntity? = store[missionId]
        override fun getAll() = flowOf(store.values.toList())
        override fun getActive() = flowOf(store.values.filter { it.status == "RUNNING" })
        override suspend fun upsert(mission: MissionStateEntity) { store[mission.missionId] = mission }
        override suspend fun delete(missionId: String) { store.remove(missionId) }
        override suspend fun updateStatus(missionId: String, status: String, updatedAt: Long) {
            store[missionId]?.let { store[missionId] = it.copy(status = status, updatedAt = updatedAt) }
        }
    }

    private fun createManager(dao: FakeMissionStateDao = FakeMissionStateDao()): Pair<RecoveryManager, FakeMissionStateDao> {
        val supervisor = ProcessSupervisor(
            startProcess = { 999L },
            stopProcess = { },
            checkHealth = { HealthCheckResult(healthy = true) },
        )
        val manager = RecoveryManager(
            missionStateDao = dao,
            processSupervisor = supervisor,
        )
        return manager to dao
    }

    @Test
    fun `attemptRecovery returns Failed for missing mission`() = runTest {
        val (manager, _) = createManager()
        val result = manager.attemptRecovery("nonexistent")
        assertTrue(result is RecoveryResult.Failed)
        assertEquals("nonexistent", (result as RecoveryResult.Failed).missionId)
    }

    @Test
    fun `attemptRecovery returns NotNeeded for completed mission`() = runTest {
        val dao = FakeMissionStateDao()
        dao.upsert(MissionStateEntity(missionId = "m1", status = "COMPLETED"))
        val (manager, _) = createManager(dao)
        val result = manager.attemptRecovery("m1")
        assertTrue(result is RecoveryResult.NotNeeded)
    }

    @Test
    fun `attemptRecovery returns NotNeeded for cancelled mission`() = runTest {
        val dao = FakeMissionStateDao()
        dao.upsert(MissionStateEntity(missionId = "m1", status = "CANCELLED"))
        val (manager, _) = createManager(dao)
        val result = manager.attemptRecovery("m1")
        assertTrue(result is RecoveryResult.NotNeeded)
    }

    @Test
    fun `attemptRecovery rolls back failed mission with checkpoint`() = runTest {
        val dao = FakeMissionStateDao()
        dao.upsert(MissionStateEntity(missionId = "m1", status = "FAILED", currentStep = 8, checkpoint = "5"))
        val (manager, _) = createManager(dao)
        val result = manager.attemptRecovery("m1")
        assertTrue(result is RecoveryResult.Recovered)
        assertEquals(5, (result as RecoveryResult.Recovered).restoredStep)
        val updated = dao.getById("m1")
        assertEquals("PENDING", updated?.status)
        assertEquals(5, updated?.currentStep)
    }

    @Test
    fun `attemptRecovery fails for failed mission without checkpoint`() = runTest {
        val dao = FakeMissionStateDao()
        dao.upsert(MissionStateEntity(missionId = "m1", status = "FAILED", checkpoint = null))
        val (manager, _) = createManager(dao)
        val result = manager.attemptRecovery("m1")
        assertTrue(result is RecoveryResult.Failed)
    }

    @Test
    fun `attemptRecovery resets running mission to pending`() = runTest {
        val dao = FakeMissionStateDao()
        dao.upsert(MissionStateEntity(missionId = "m1", status = "RUNNING", currentStep = 3))
        val (manager, _) = createManager(dao)
        val result = manager.attemptRecovery("m1")
        assertTrue(result is RecoveryResult.Recovered)
        assertEquals(0, (result as RecoveryResult.Recovered).restoredStep)
        val updated = dao.getById("m1")
        assertEquals("PENDING", updated?.status)
    }

    @Test
    fun `persistCheckpoint stores checkpoint value`() = runTest {
        val dao = FakeMissionStateDao()
        val (manager, _) = createManager(dao)
        manager.persistCheckpoint("m1", 5)
        val state = dao.getById("m1")
        assertNotNull(state)
        assertEquals("5", state?.checkpoint)
        assertEquals(5, state?.currentStep)
    }

    @Test
    fun `persistCheckpoint updates existing mission`() = runTest {
        val dao = FakeMissionStateDao()
        dao.upsert(MissionStateEntity(missionId = "m1", currentStep = 3))
        val (manager, _) = createManager(dao)
        manager.persistCheckpoint("m1", 7)
        val state = dao.getById("m1")
        assertEquals("7", state?.checkpoint)
        assertEquals(3, state?.currentStep)
    }

    @Test
    fun `markRunning sets status to RUNNING`() = runTest {
        val dao = FakeMissionStateDao()
        val (manager, _) = createManager(dao)
        manager.markRunning("m1", totalSteps = 12)
        val state = dao.getById("m1")
        assertEquals("RUNNING", state?.status)
        assertEquals(12, state?.totalSteps)
    }

    @Test
    fun `markComplete sets status to COMPLETED`() = runTest {
        val dao = FakeMissionStateDao()
        dao.upsert(MissionStateEntity(missionId = "m1", status = "RUNNING", totalSteps = 12))
        val (manager, _) = createManager(dao)
        manager.markComplete("m1")
        val state = dao.getById("m1")
        assertEquals("COMPLETED", state?.status)
        assertEquals(12, state?.currentStep)
    }

    @Test
    fun `markFailed sets status to FAILED with error`() = runTest {
        val dao = FakeMissionStateDao()
        dao.upsert(MissionStateEntity(missionId = "m1", status = "RUNNING"))
        val (manager, _) = createManager(dao)
        manager.markFailed("m1", "OOM killed")
        val state = dao.getById("m1")
        assertEquals("FAILED", state?.status)
        assertTrue(state?.errorHistory?.contains("OOM killed") == true)
    }

    @Test
    fun `cancel sets status to CANCELLED`() = runTest {
        val dao = FakeMissionStateDao()
        dao.upsert(MissionStateEntity(missionId = "m1", status = "RUNNING"))
        val (manager, _) = createManager(dao)
        manager.cancel("m1")
        val state = dao.getById("m1")
        assertEquals("CANCELLED", state?.status)
    }

    @Test
    fun `cancel does nothing for completed mission`() = runTest {
        val dao = FakeMissionStateDao()
        dao.upsert(MissionStateEntity(missionId = "m1", status = "COMPLETED"))
        val (manager, _) = createManager(dao)
        manager.cancel("m1")
        val state = dao.getById("m1")
        assertEquals("COMPLETED", state?.status)
    }

    @Test
    fun `getMissionState returns null for missing`() = runTest {
        val (manager, _) = createManager()
        assertNull(manager.getMissionState("nonexistent"))
    }

    @Test
    fun `getMissionState returns existing state`() = runTest {
        val dao = FakeMissionStateDao()
        dao.upsert(MissionStateEntity(missionId = "m1", status = "RUNNING", currentStep = 4))
        val (manager, _) = createManager(dao)
        val state = manager.getMissionState("m1")
        assertNotNull(state)
        assertEquals("RUNNING", state?.status)
        assertEquals(4, state?.currentStep)
    }
}
