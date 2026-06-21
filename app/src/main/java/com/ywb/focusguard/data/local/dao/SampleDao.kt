package com.ywb.focusguard.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ywb.focusguard.data.local.entity.LightSampleEntity
import com.ywb.focusguard.data.local.entity.MotionEventEntity
import com.ywb.focusguard.data.local.entity.NoiseSampleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SampleDao {
    // 采样表通过 sessionId 关联一次专注记录：一条 FocusSession 可以对应多条噪声/光照/移动样本。
    @Query("SELECT * FROM noise_samples WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    fun observeNoiseSamples(sessionId: Long): Flow<List<NoiseSampleEntity>>

    @Query("SELECT * FROM light_samples WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    fun observeLightSamples(sessionId: Long): Flow<List<LightSampleEntity>>

    @Query("SELECT * FROM motion_events WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    fun observeMotionEvents(sessionId: Long): Flow<List<MotionEventEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNoiseSample(sample: NoiseSampleEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLightSample(sample: LightSampleEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMotionEvent(event: MotionEventEntity)
}
