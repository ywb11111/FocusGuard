package com.ywb.focusguard.data.repository

import com.ywb.focusguard.domain.model.EnvironmentSnapshot
import com.ywb.focusguard.domain.model.FocusConfig
import com.ywb.focusguard.domain.model.FocusSession
import com.ywb.focusguard.domain.model.LightSample
import com.ywb.focusguard.domain.model.MotionSample
import com.ywb.focusguard.domain.model.NoiseSample
import com.ywb.focusguard.domain.model.SessionDetail
import com.ywb.focusguard.domain.model.TodaySummary
import com.ywb.focusguard.domain.model.UserSettings
import kotlinx.coroutines.flow.Flow

/**
 * 专注记录的数据边界。
 *
 * ViewModel 依赖接口而不是 Room DAO，数据源和表结构变化时 UI 不需要跟着修改。
 */
interface FocusRepository {
    /** 持续观察今天已完成会话的汇总数据。 */
    fun observeTodaySummary(): Flow<TodaySummary>

    /** 按开始时间倒序观察全部已完成专注会话。 */
    fun observeSessions(): Flow<List<FocusSession>>

    /** 按会话 id 观察详情；记录不存在时发射 null。 */
    fun observeSessionDetail(sessionId: Long): Flow<SessionDetail?>

    /** 创建一条进行中记录，并返回 Room 生成的真实会话 id。 */
    suspend fun startSession(config: FocusConfig): Long

    /** 用 ViewModel 计算出的有效时长结束会话，更新数据库并返回完整结果。 */
    suspend fun finishSession(sessionId: Long, durationMillis: Long): FocusSession

    /** 保存一条属于指定会话的噪声采样。 */
    suspend fun saveNoiseSample(sessionId: Long, sample: NoiseSample)

    /** 保存一条属于指定会话的光照采样。 */
    suspend fun saveLightSample(sessionId: Long, sample: LightSample)

    /** 保存一次属于指定会话的已确认移动事件。 */
    suspend fun saveMotionEvent(sessionId: Long, event: MotionSample)
}

/**
 * 环境实时数据边界，统一屏蔽 SensorManager 和未来的 AudioRecord 实现细节。
 * 光照与移动当前是真实数据，噪声暂时仍是演示数据。
 */
interface EnvironmentRepository {
    /** 观察实时噪声样本。 */
    fun observeNoise(): Flow<NoiseSample>

    /** 观察实时光照样本。 */
    fun observeLight(): Flow<LightSample>

    /** 观察实时移动样本；true 只在确认新移动事件时出现。 */
    fun observeMotion(): Flow<MotionSample>

    /** 将三类样本组合为 UI 使用的整体环境快照。 */
    fun observeEnvironmentSnapshot(): Flow<EnvironmentSnapshot>
}

/** 设置数据边界；当前实现保存在内存，后续替换为 DataStore。 */
interface SettingsRepository {
    /** 持续发射最新用户设置。 */
    val settings: Flow<UserSettings>

    /** 更新噪声提醒阈值，单位相对 dB。 */
    suspend fun updateNoiseThreshold(value: Float)

    /** 更新舒适光照范围，单位 lux。 */
    suspend fun updateLightRange(min: Float, max: Float)

    /** 更新默认专注时长，单位分钟。 */
    suspend fun updateDefaultFocusMinutes(minutes: Int)
}
