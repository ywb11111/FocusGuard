package com.ywb.focusguard.data.repository

import com.ywb.focusguard.domain.model.EnvironmentSnapshot
import com.ywb.focusguard.domain.model.FocusConfig
import com.ywb.focusguard.domain.model.FocusSession
import com.ywb.focusguard.domain.model.LightSample
import com.ywb.focusguard.domain.model.MotionSample
import com.ywb.focusguard.domain.model.NoiseSample
import com.ywb.focusguard.domain.model.SessionDetail
import com.ywb.focusguard.domain.model.SleepDetail
import com.ywb.focusguard.domain.model.SleepEpoch
import com.ywb.focusguard.domain.model.SleepPlacement
import com.ywb.focusguard.domain.model.SleepSession
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

    /** 按日期范围观察已完成会话（用于周报/月报）。 */
    fun observeSessionsInRange(startMillis: Long, endMillis: Long): Flow<List<FocusSession>>

    /** 一次性获取日期范围内的会话，用于计算周/月统计。 */
    suspend fun getSessionsInRange(startMillis: Long, endMillis: Long): List<FocusSession>
}

/**
 * 环境实时数据边界，统一屏蔽 SensorManager 和 AudioRecord 实现细节。
 * 光照、噪声和移动均为真实传感器数据（AudioRecord + SensorManager）。
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

    /** 更新专注期间是否启用前台后台监测。 */
    suspend fun updateBackgroundMonitoringEnabled(enabled: Boolean)

    /** 更新是否允许生成每日总结。 */
    suspend fun updateDailyReportEnabled(enabled: Boolean)

    /** 标记首次引导已经完成。 */
    suspend fun completeOnboarding()
}

/**
 * 睡眠记录的数据边界。
 *
 * 写入方是 SleepMonitorService（开始、每分钟写 epoch、结束），读取方是睡眠相关 ViewModel。
 * 两边都只依赖这个接口，不直接接触 Room 和 SleepAnalyzer。
 */
interface SleepRepository {
    /** 观察进行中的睡眠记录；没有时发射 null。 */
    fun observeActiveSession(): Flow<SleepSession?>

    /** 按开始时间倒序观察已结束的睡眠记录。 */
    fun observeSessions(): Flow<List<SleepSession>>

    /** 观察一晚的完整详情（主记录 + 分钟数据 + 实时分析）；不存在时发射 null。 */
    fun observeSleepDetail(sessionId: Long): Flow<SleepDetail?>

    /** 一次性读取进行中的记录，Service 重启恢复时使用。 */
    suspend fun getActiveSession(): SleepSession?

    /** 创建一条进行中的睡眠记录，返回 Room 生成的 id。 */
    suspend fun startSession(placement: SleepPlacement, audioEnabled: Boolean): Long

    /** 保存一分钟聚合数据。 */
    suspend fun saveEpoch(sessionId: Long, epoch: SleepEpoch)

    /**
     * 结束一晚记录：读取全部分钟数据交给 SleepAnalyzer，回写统计和评分。
     *
     * @param endTime 结束时间；记录被系统中断时应传最后一分钟数据的结束时间，而不是"现在"。
     * @param note 系统说明，例如"记录被系统中断"。
     */
    suspend fun finishSession(sessionId: Long, endTime: Long, note: String? = null): SleepSession?

    /**
     * 结束一晚被系统中断的记录（进程被杀、Service 没能恢复）。
     * 结束时间取最后一分钟数据的结束时间，避免把中断后没有数据的几个小时算进报告。
     */
    suspend fun finishInterruptedSession(sessionId: Long): SleepSession?
}
