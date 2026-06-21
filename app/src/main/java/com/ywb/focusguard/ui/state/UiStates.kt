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

// UiState 是页面要显示的数据快照。Screen 只读取 UiState，不直接碰数据库、传感器或音频 API。
data class TodayUiState(
    val isLoading: Boolean = true,
    val environment: EnvironmentSnapshot? = null,
    val todaySummary: TodaySummary? = null,
    val latestSession: FocusSession? = null,
    val permissionState: PermissionState = PermissionState(),
    val activeSession: ActiveSession? = null,
    val errorMessage: String? = null
)

// sealed interface 用来限制专注页的合法状态，避免多个 Boolean 组合出互相矛盾的状态。
sealed interface SessionUiState {
    data object Idle : SessionUiState

    // 准备开始：显示环境预检和开始按钮。
    data class Ready(
        val config: FocusConfig,
        val environment: EnvironmentSnapshot?
    ) : SessionUiState

    // 正在专注：每秒更新 elapsedMillis / remainingMillis，UI 根据这个状态刷新倒计时。
    data class Running(
        val sessionId: Long,
        val elapsedMillis: Long,
        val remainingMillis: Long?,
        val noiseSamples: List<NoiseSample>,
        val lightLevel: LightLevel,
        val movementCount: Int
    ) : SessionUiState

    // 暂停状态保留已经专注的时间，继续时 ViewModel 会从这个时间点接着计时。
    data class Paused(
        val sessionId: Long,
        val elapsedMillis: Long,
        val remainingMillis: Long?,
        val lightLevel: LightLevel,
        val movementCount: Int
    ) : SessionUiState

    // 结束状态保存本次会话结果；下一阶段会把这里的结果写入 Room。
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
