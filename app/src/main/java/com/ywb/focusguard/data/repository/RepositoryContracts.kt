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

// Repository 是 UI/ViewModel 和具体数据来源之间的边界。
// ViewModel 依赖接口而不是 Room/SensorManager/AudioRecord，后续替换实现时 UI 不需要跟着大改。
interface FocusRepository {
    fun observeTodaySummary(): Flow<TodaySummary>
    fun observeSessions(): Flow<List<FocusSession>>
    fun observeSessionDetail(sessionId: Long): Flow<SessionDetail?>
    suspend fun startSession(config: FocusConfig): Long
    suspend fun finishSession(sessionId: Long): FocusSession
    suspend fun saveNoiseSample(sessionId: Long, sample: NoiseSample)
    suspend fun saveLightSample(sessionId: Long, sample: LightSample)
    suspend fun saveMotionEvent(sessionId: Long, event: MotionSample)
}

// 环境数据未来会来自麦克风、光照传感器、加速度计；当前先用接口把调用方式固定下来。
interface EnvironmentRepository {
    fun observeNoise(): Flow<NoiseSample>
    fun observeLight(): Flow<LightSample>
    fun observeMotion(): Flow<MotionSample>
    fun observeEnvironmentSnapshot(): Flow<EnvironmentSnapshot>
}

// 设置数据未来会落到 DataStore；现在先保留接口，避免 UI 直接依赖存储细节。
interface SettingsRepository {
    val settings: Flow<UserSettings>
    suspend fun updateNoiseThreshold(value: Float)
    suspend fun updateLightRange(min: Float, max: Float)
    suspend fun updateDefaultFocusMinutes(minutes: Int)
}
