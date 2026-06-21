package com.ywb.focusguard.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.ywb.focusguard.data.local.dao.FocusSessionDao
import com.ywb.focusguard.data.local.dao.SampleDao
import com.ywb.focusguard.data.local.entity.FocusSessionEntity
import com.ywb.focusguard.data.local.entity.LightSampleEntity
import com.ywb.focusguard.data.local.entity.MotionEventEntity
import com.ywb.focusguard.data.local.entity.NoiseSampleEntity

// Room 数据库入口：集中声明有哪些表，以及能拿到哪些 DAO。
// Entity 负责“怎么存”，DAO 负责“怎么查/写”，Repository 负责“怎么给业务层使用”。
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
