package com.ywb.focusguard.ui.state

import com.ywb.focusguard.domain.model.SessionDetail
import com.ywb.focusguard.ui.screen.formatDuration

/** 专注详情页的加载、空数据和正常内容三种互斥状态。 */
sealed interface SessionDetailUiState {
    /** Repository 首次发射数据前显示加载提示。 */
    data object Loading : SessionDetailUiState

    /**
     * sessionId 无对应记录时的空状态。
     *
     * @property message 页面展示的友好提示。
     */
    data class Empty(
        val message: String = "没有找到这次专注记录"
    ) : SessionDetailUiState

    /**
     * 详情页可直接渲染的展示模型，字符串格式化已在映射层完成。
     *
     * @property sessionId 当前会话 id。
     * @property title 页面标题。
     * @property durationText 格式化后的时长。
     * @property scoreText 格式化后的总分。
     * @property scoreBreakdownText 各项扣分说明。
     * @property noiseValues 噪声曲线数值。
     * @property lightValues 光照曲线数值。
     * @property suggestionText 首条改进建议或默认文案。
     */
    data class Content(
        val sessionId: Long,
        val title: String,
        val durationText: String,
        val scoreText: String,
        val scoreBreakdownText: String,
        val noiseValues: List<Float>,
        val lightValues: List<Float>,
        val suggestionText: String
    ) : SessionDetailUiState
}

/** 把领域模型整理成 UI 展示模型，避免 Composable 混入字符串拼接和格式化逻辑。 */
fun SessionDetail.toUiState(): SessionDetailUiState.Content = SessionDetailUiState.Content(
    sessionId = session.id,
    title = "专注详情 #${session.id}",
    durationText = formatDuration(session.durationMillis),
    scoreText = session.score.toString(),
    scoreBreakdownText = "噪声 -${score.noisePenalty} · 光照 -${score.lightPenalty} · 移动 -${score.motionPenalty} · 分心 -${score.distractionPenalty}",
    noiseValues = noiseSamples.map { it.decibel },
    lightValues = lightSamples.map { it.lux },
    suggestionText = score.suggestions.firstOrNull() ?: "本次专注记录已保存，后续接入采样后会给出更具体的建议。"
)
