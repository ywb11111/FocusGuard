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
