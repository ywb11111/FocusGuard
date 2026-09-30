package com.ywb.focusguard.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.ywb.focusguard.data.local.dao.FocusSessionDao
import com.ywb.focusguard.data.local.dao.SampleDao
import com.ywb.focusguard.data.local.dao.SleepDao
import com.ywb.focusguard.data.local.entity.FocusSessionEntity
import com.ywb.focusguard.data.local.entity.LightSampleEntity
import com.ywb.focusguard.data.local.entity.MotionEventEntity
import com.ywb.focusguard.data.local.entity.NoiseSampleEntity
import com.ywb.focusguard.data.local.entity.SleepEpochEntity
import com.ywb.focusguard.data.local.entity.SleepSessionEntity

// Room 数据库入口：集中声明有哪些表，以及能拿到哪些 DAO。
// Entity 负责“怎么存”，DAO 负责“怎么查/写”，Repository 负责“怎么给业务层使用”。
// version 2：新增睡眠监测两张表，升级路径见 DatabaseMigrations.kt 的 MIGRATION_1_2。
@Database(
    entities = [
        FocusSessionEntity::class,
        NoiseSampleEntity::class,
        LightSampleEntity::class,
        MotionEventEntity::class,
        SleepSessionEntity::class,
        SleepEpochEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class FocusGuardDatabase : RoomDatabase() {
    /** 提供专注会话表的 DAO，由 Hilt 交给 FocusRepository 使用。 */
    abstract fun focusSessionDao(): FocusSessionDao

    /** 提供噪声、光照和移动采样表的统一 DAO。 */
    abstract fun sampleDao(): SampleDao

    /** 提供睡眠记录和分钟数据的 DAO，由 Hilt 交给 SleepRepository 使用。 */
    abstract fun sleepDao(): SleepDao
}
