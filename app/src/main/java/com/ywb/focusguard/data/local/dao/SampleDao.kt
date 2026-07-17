package com.ywb.focusguard.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ywb.focusguard.data.local.entity.LightSampleEntity
import com.ywb.focusguard.data.local.entity.MotionEventEntity
import com.ywb.focusguard.data.local.entity.NoiseSampleEntity
import kotlinx.coroutines.flow.Flow

/**
 * 三类环境采样表的数据库访问入口。
 *
 * 查询返回 [Flow]，Room 在表内容变化时会自动发射新列表；
 * 同步查询使用 suspend，由 Repository 在协程中调用。
 */
@Dao
interface SampleDao {
    // ==================== Flow 查询（用于实时观察） ====================

    /** 观察指定会话的噪声采样列表。 */
    @Query("SELECT * FROM noise_samples WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    fun observeNoiseSamples(sessionId: Long): Flow<List<NoiseSampleEntity>>

    /** 观察指定会话的光照采样列表。 */
    @Query("SELECT * FROM light_samples WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    fun observeLightSamples(sessionId: Long): Flow<List<LightSampleEntity>>

    /** 观察指定会话的移动事件列表。 */
    @Query("SELECT * FROM motion_events WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    fun observeMotionEvents(sessionId: Long): Flow<List<MotionEventEntity>>

    // ==================== 同步查询（用于结束时聚合统计） ====================

    /** 一次性获取指定会话的光照采样列表。 */
    @Query("SELECT * FROM light_samples WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    suspend fun getLightSamplesOnce(sessionId: Long): List<LightSampleEntity>

    /** 一次性获取指定会话的移动事件列表。 */
    @Query("SELECT * FROM motion_events WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    suspend fun getMotionEventsOnce(sessionId: Long): List<MotionEventEntity>

    /** 一次性获取指定会话的噪声采样列表。 */
    @Query("SELECT * FROM noise_samples WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    suspend fun getNoiseSamplesOnce(sessionId: Long): List<NoiseSampleEntity>

    // ==================== 写入接口 ====================

    /** 保存一条噪声采样。 */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNoiseSample(sample: NoiseSampleEntity)

    /** 保存一条光照采样。 */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLightSample(sample: LightSampleEntity)

    /** 保存一次已确认的移动事件。 */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMotionEvent(event: MotionEventEntity)
}
