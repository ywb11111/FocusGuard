package com.ywb.focusguard.data.repository

import com.ywb.focusguard.domain.model.UserSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 设置 Repository 的内存实现，用于先跑通 UI 数据流。
 * App 进程结束后设置会丢失，后续阶段会把 [settingsState] 替换为 DataStore。
 */
@Singleton
class SettingsRepositoryImpl @Inject constructor() : SettingsRepository {
    /** 当前设置的可变状态源，只允许 Repository 内部修改。 */
    private val settingsState = MutableStateFlow(UserSettings())

    /** 对外暴露只读 Flow，避免 UI 绕过更新方法直接修改设置。 */
    override val settings: Flow<UserSettings> = settingsState

    /** 复制当前设置，仅替换噪声阈值。 */
    override suspend fun updateNoiseThreshold(value: Float) {
        settingsState.value = settingsState.value.copy(noiseThresholdDb = value)
    }

    /** 同时更新光照上下限，调用方应保证 min 不大于 max。 */
    override suspend fun updateLightRange(min: Float, max: Float) {
        settingsState.value = settingsState.value.copy(
            comfortableLightMinLux = min,
            comfortableLightMaxLux = max
        )
    }

    /** 更新默认专注分钟数。 */
    override suspend fun updateDefaultFocusMinutes(minutes: Int) {
        settingsState.value = settingsState.value.copy(defaultFocusMinutes = minutes)
    }
}
