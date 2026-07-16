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
 * 查询返回 [Flow]，Room 在表内容变化时会自动发射新列表；写入方法使用 suspend，
 * 由 Repository 在协程中调用，避免数据库操作阻塞主线程。
 */
@Dao
interface SampleDao {
    // 采样表通过 sessionId 关联一次专注记录：一条 FocusSession 可以对应多条噪声/光照/移动样本。
    @Query("SELECT * FROM noise_samples WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    fun observeNoiseSamples(sessionId: Long): Flow<List<NoiseSampleEntity>>

    @Query("SELECT * FROM light_samples WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    fun observeLightSamples(sessionId: Long): Flow<List<LightSampleEntity>>

    // 移动表只保存防抖后确认的事件，因此结果数量可以直接用于统计移动次数。
    @Query("SELECT * FROM motion_events WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    fun observeMotionEvents(sessionId: Long): Flow<List<MotionEventEntity>>

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
