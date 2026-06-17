package com.ywb.focusguard.ui.state

import com.ywb.focusguard.domain.model.ActiveSession
import com.ywb.focusguard.domain.model.EnvironmentSnapshot
import com.ywb.focusguard.domain.model.FocusConfig
import com.ywb.focusguard.domain.model.FocusSession
import com.ywb.focusguard.domain.model.LightLevel
import com.ywb.focusguard.domain.model.NoiseSample
import com.ywb.focusguard.domain.model.PermissionState
import com.ywb.focusguard.domain.model.TodaySummary
import com.ywb.focusguard.domain.model.UserSettings

data class TodayUiState(
    val isLoading: Boolean = true,
    val environment: EnvironmentSnapshot? = null,
    val todaySummary: TodaySummary? = null,
    val latestSession: FocusSession? = null,
    val permissionState: PermissionState = PermissionState(),
    val activeSession: ActiveSession? = null,
    val errorMessage: String? = null
)

sealed interface SessionUiState {
    data object Idle : SessionUiState

    data class Ready(
        val config: FocusConfig,
        val environment: EnvironmentSnapshot?
    ) : SessionUiState

    data class Running(
        val sessionId: Long,
        val elapsedMillis: Long,
        val remainingMillis: Long?,
        val noiseSamples: List<NoiseSample>,
        val lightLevel: LightLevel,
        val movementCount: Int
    ) : SessionUiState

    data class Finished(
        val session: FocusSession
    ) : SessionUiState
}

data class ReportsUiState(
    val sessions: List<FocusSession> = emptyList(),
    val summary: TodaySummary? = null
)

data class SettingsUiState(
    val settings: UserSettings = UserSettings(),
    val permissionState: PermissionState = PermissionState()
)
