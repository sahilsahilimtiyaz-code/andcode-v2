package com.yugahashimoto.andcode.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "mission_states")
data class MissionStateEntity(
    @PrimaryKey val missionId: String,
    val currentStep: Int = 0,
    val totalSteps: Int = 12,
    val status: String = "PENDING",
    val checkpoint: String? = null,
    val errorHistory: String = "[]",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)

@Dao
interface MissionStateDao {
    @Query("SELECT * FROM mission_states WHERE missionId = :missionId")
    suspend fun getById(missionId: String): MissionStateEntity?

    @Query("SELECT * FROM mission_states ORDER BY updatedAt DESC")
    fun getAll(): Flow<List<MissionStateEntity>>

    @Query("SELECT * FROM mission_states WHERE status = 'RUNNING'")
    fun getActive(): Flow<List<MissionStateEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(mission: MissionStateEntity)

    @Query("DELETE FROM mission_states WHERE missionId = :missionId")
    suspend fun delete(missionId: String)

    @Query("UPDATE mission_states SET status = :status, updatedAt = :updatedAt WHERE missionId = :missionId")
    suspend fun updateStatus(missionId: String, status: String, updatedAt: Long = System.currentTimeMillis())
}
