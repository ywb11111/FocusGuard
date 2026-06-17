package com.ywb.focusguard.data.repository

import com.ywb.focusguard.domain.model.UserSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepositoryImpl @Inject constructor() : SettingsRepository {
    private val settingsState = MutableStateFlow(UserSettings())

    override val settings: Flow<UserSettings> = settingsState

    override suspend fun updateNoiseThreshold(value: Float) {
        settingsState.value = settingsState.value.copy(noiseThresholdDb = value)
    }

    override suspend fun updateLightRange(min: Float, max: Float) {
        settingsState.value = settingsState.value.copy(
            comfortableLightMinLux = min,
            comfortableLightMaxLux = max
        )
    }

    override suspend fun updateDefaultFocusMinutes(minutes: Int) {
        settingsState.value = settingsState.value.copy(defaultFocusMinutes = minutes)
    }
}
