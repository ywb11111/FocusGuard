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

interface EnvironmentRepository {
    fun observeNoise(): Flow<NoiseSample>
    fun observeLight(): Flow<LightSample>
    fun observeMotion(): Flow<MotionSample>
    fun observeEnvironmentSnapshot(): Flow<EnvironmentSnapshot>
}

interface SettingsRepository {
    val settings: Flow<UserSettings>
    suspend fun updateNoiseThreshold(value: Float)
    suspend fun updateLightRange(min: Float, max: Float)
    suspend fun updateDefaultFocusMinutes(minutes: Int)
}
