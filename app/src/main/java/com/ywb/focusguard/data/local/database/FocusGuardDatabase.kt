package com.ywb.focusguard.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.ywb.focusguard.data.local.dao.FocusSessionDao
import com.ywb.focusguard.data.local.dao.SampleDao
import com.ywb.focusguard.data.local.entity.FocusSessionEntity
import com.ywb.focusguard.data.local.entity.LightSampleEntity
import com.ywb.focusguard.data.local.entity.MotionEventEntity
import com.ywb.focusguard.data.local.entity.NoiseSampleEntity

@Database(
    entities = [
        FocusSessionEntity::class,
        NoiseSampleEntity::class,
        LightSampleEntity::class,
        MotionEventEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class FocusGuardDatabase : RoomDatabase() {
    abstract fun focusSessionDao(): FocusSessionDao
    abstract fun sampleDao(): SampleDao
}
