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

/**
 * 今日页一次可渲染的数据快照，Screen 只读取这些字段，不直接访问硬件或数据库。
 *
 * @property isLoading 首批环境与统计数据是否仍在加载。
 * @property environment 最新环境快照；null 表示传感器流尚未发射。
 * @property todaySummary 今日已完成专注的聚合统计。
 * @property latestSession 最近一条已完成专注记录。
 * @property permissionState 当前权限状态，后续接入真实权限检查。
 * @property activeSession 跨页面展示的进行中会话，当前尚未接入。
 * @property errorMessage 页面可展示的错误信息，null 表示没有错误。
 */
data class TodayUiState(
    val isLoading: Boolean = true,
    val environment: EnvironmentSnapshot? = null,
    val todaySummary: TodaySummary? = null,
    val latestSession: FocusSession? = null,
    val permissionState: PermissionState = PermissionState(),
    val activeSession: ActiveSession? = null,
    val errorMessage: String? = null
)

/** sealed 状态机限制专注页的合法阶段，避免多个 Boolean 组合出矛盾状态。 */
sealed interface SessionUiState {
    /** 预留的初始状态，当前 ViewModel 会直接从 Ready 开始。 */
    data object Idle : SessionUiState

    /**
     * 准备开始，展示配置和最新环境预检。
     *
     * @property config 本次专注使用的目标时长和监测开关。
     * @property environment 最新环境快照；null 表示仍在等待首个样本。
     */
    data class Ready(
        val config: FocusConfig,
        val environment: EnvironmentSnapshot?
    ) : SessionUiState

    /**
     * 正在专注，每秒由 ViewModel 更新计时字段。
     *
     * @property sessionId Room 创建的真实会话 id。
     * @property elapsedMillis 已累计的有效专注时长。
     * @property remainingMillis 距离目标结束的剩余时长；null 可表示无限时长模式。
     * @property noiseSamples 当前曲线窗口中的噪声样本，现阶段仍为空。
     * @property lightLevel 最新光照等级。
     * @property movementCount 本次会话已确认的移动次数；当前累计逻辑尚待接入。
     */
    data class Running(
        val sessionId: Long,
        val elapsedMillis: Long,
        val remainingMillis: Long?,
        val noiseSamples: List<NoiseSample>,
        val lightLevel: LightLevel,
        val movementCount: Int
    ) : SessionUiState

    /**
     * 暂停状态，冻结计时与环境摘要，继续时从 elapsedMillis 接着累计。
     *
     * @property sessionId 当前会话 id。
     * @property elapsedMillis 暂停前累计的有效时长。
     * @property remainingMillis 暂停时剩余时长。
     * @property lightLevel 暂停瞬间的光照等级。
     * @property movementCount 暂停前累计的移动次数。
     */
    data class Paused(
        val sessionId: Long,
        val elapsedMillis: Long,
        val remainingMillis: Long?,
        val lightLevel: LightLevel,
        val movementCount: Int
    ) : SessionUiState

    /**
     * 结束状态，展示 Repository 已写入 Room 的会话结果。
     *
     * @property session 完整会话记录，可用于打开详情页。
     */
    data class Finished(
        val session: FocusSession
    ) : SessionUiState
}

/**
 * 专注流程的“页面级”阶段，只在真正跨阶段时触发转场动画。
 *
 * Ready 的传感器数据和 Running 的倒计时都会高频变化；如果直接把完整 UiState 当动画 key，
 * Compose 会把每个样本、每一秒都视为新页面，从而不断淡入淡出并产生闪烁。
 */
enum class SessionVisualPhase {
    IDLE,
    READY,
    RUNNING,
    PAUSED,
    FINISHED
}

/** 将包含实时数据的 UiState 压缩为稳定的页面阶段 key。 */
fun SessionUiState.visualPhase(): SessionVisualPhase = when (this) {
    SessionUiState.Idle -> SessionVisualPhase.IDLE
    is SessionUiState.Ready -> SessionVisualPhase.READY
    is SessionUiState.Running -> SessionVisualPhase.RUNNING
    is SessionUiState.Paused -> SessionVisualPhase.PAUSED
    is SessionUiState.Finished -> SessionVisualPhase.FINISHED
}

/** 报告页周期类型。 */
enum class ReportPeriod(val label: String) {
    WEEK("本周"),
    MONTH("本月")
}

/** 报告页周期聚合统计。 */
data class PeriodSummary(
    val totalFocusMillis: Long = 0L,
    val averageScore: Int = 0,
    val sessionCount: Int = 0,
    val averageNoiseDb: Float = 0f,
    val averageLightLux: Float = 0f,
    val totalMovementCount: Int = 0
)

/**
 * 报告页状态。
 *
 * @property period 当前选中的周期（周/月）。
 * @property sessions 当前周期内的已完成会话，按时间倒序。
 * @property periodSummary 当前周期的聚合统计。
 * @property trendValues 趋势图数据（各会话评分）。
 */
data class ReportsUiState(
    val period: ReportPeriod = ReportPeriod.WEEK,
    val sessions: List<FocusSession> = emptyList(),
    val periodSummary: PeriodSummary = PeriodSummary(),
    val trendValues: List<Float> = emptyList()
)

/**
 * 设置页状态。
 *
 * @property settings 最新用户设置。
 * @property permissionState 当前权限快照。
 */
data class SettingsUiState(
    val settings: UserSettings = UserSettings(),
    val permissionState: PermissionState = PermissionState()
)
