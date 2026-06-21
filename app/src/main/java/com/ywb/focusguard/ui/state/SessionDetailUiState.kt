package com.ywb.focusguard.ui.state

import com.ywb.focusguard.domain.model.SessionDetail
import com.ywb.focusguard.ui.screen.formatDuration

sealed interface SessionDetailUiState {
    data object Loading : SessionDetailUiState

    data class Empty(
        val message: String = "没有找到这次专注记录"
    ) : SessionDetailUiState

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

// 把领域模型整理成 UI 直接展示的数据，避免 Composable 里混入字符串拼接和格式化逻辑。
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
