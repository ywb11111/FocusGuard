package com.ywb.focusguard.ui.state

import com.ywb.focusguard.domain.model.SleepDetail
import com.ywb.focusguard.domain.model.SleepLiveStatus
import com.ywb.focusguard.domain.model.SleepPlacement
import com.ywb.focusguard.domain.model.SleepSession

/**
 * 睡前检查项。都不是必需的，但缺少任何一项都会降低整晚记录的完整性，所以在准备页逐项提示。
 *
 * @property audioGranted 麦克风权限：没有就无法分析鼾声和噪声。
 * @property notificationGranted 通知权限（Android 13+）：没有时前台服务仍能运行，但用户看不到状态和结束按钮。
 * @property batteryOptimizationIgnored 是否已关闭电池优化：国产 ROM 上不关闭，半夜很容易被系统清理。
 */
data class SleepChecklist(
    val audioGranted: Boolean = false,
    val notificationGranted: Boolean = false,
    val batteryOptimizationIgnored: Boolean = false
)

/**
 * 睡眠页状态机。与专注页一样用 sealed 限制合法阶段，避免多个 Boolean 组合出矛盾状态。
 *
 * 阶段由两个数据源共同决定：
 * - Room 里是否有进行中的记录（持久的真相，进程被杀也在）；
 * - SleepStateHolder 里 Service 是否存活（只在本进程内有效）。
 */
sealed interface SleepUiState {
    /** 首次读取数据库之前。 */
    data object Loading : SleepUiState

    /**
     * 准备开始：选择摆放位置、完成睡前检查、查看最近记录。
     *
     * @property placement 当前选中的摆放位置，默认沿用上一晚。
     * @property checklist 睡前检查项。
     * @property recentSessions 最近 7 晚记录，按时间倒序。
     * @property trendScores 最近 7 晚评分，按时间正序，供趋势图使用。
     */
    data class Ready(
        val placement: SleepPlacement,
        val checklist: SleepChecklist,
        val recentSessions: List<SleepSession>,
        val trendScores: List<Float>
    ) : SleepUiState

    /**
     * 正在监测。
     *
     * @property session Room 中进行中的记录。
     * @property live Service 发布的实时状态；Service 刚启动时可能为 null。
     */
    data class Recording(
        val session: SleepSession,
        val live: SleepLiveStatus?
    ) : SleepUiState

    /**
     * Room 里有进行中的记录，但 Service 不在运行：进程曾被系统杀掉且没能恢复。
     *
     * @property session 被中断的记录。
     */
    data class Interrupted(
        val session: SleepSession
    ) : SleepUiState

    /** 已请求结束，正在保存最后数据并分析。 */
    data object Finishing : SleepUiState
}

/** 睡眠详情页状态。 */
sealed interface SleepDetailUiState {
    /** 数据库尚未返回。 */
    data object Loading : SleepDetailUiState

    /** 记录不存在（例如 id 错误）。 */
    data object Empty : SleepDetailUiState

    /**
     * 可展示的详情。
     *
     * @property detail 主记录、分钟数据和分析结果。
     */
    data class Content(val detail: SleepDetail) : SleepDetailUiState
}

/**
 * 今日页睡眠入口卡片需要的精简数据。
 *
 * @property isRecording 当前是否有进行中的睡眠记录。
 * @property lastSession 最近一晚已完成的记录。
 */
data class TodaySleepSummary(
    val isRecording: Boolean = false,
    val lastSession: SleepSession? = null
)
