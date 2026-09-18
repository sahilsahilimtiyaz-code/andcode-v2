package com.yugahashimoto.andcode.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [MissionStateEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class MissionStateDatabase : RoomDatabase() {
    abstract fun missionStateDao(): MissionStateDao
}
