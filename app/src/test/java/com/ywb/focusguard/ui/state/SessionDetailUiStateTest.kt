package com.ywb.focusguard.ui.state

import com.ywb.focusguard.domain.model.FocusScore
import com.ywb.focusguard.domain.model.FocusSession
import com.ywb.focusguard.domain.model.SessionDetail
import org.junit.Assert.assertEquals
import org.junit.Test

class SessionDetailUiStateTest {
    @Test
    fun `SessionDetail 转换为 Content 状态时展示真实会话信息`() {
        val detail = SessionDetail(
            session = FocusSession(
                id = 9L,
                startTime = 1_000L,
                endTime = 181_000L,
                durationMillis = 180_000L,
                averageNoiseDb = 44f,
                maxNoiseDb = 60f,
                averageLightLux = 220f,
                movementCount = 1,
                distractionCount = 0,
                score = 86,
                note = "手动结束"
            ),
            noiseSamples = emptyList(),
            lightSamples = emptyList(),
            motionEvents = emptyList(),
            score = FocusScore(
                total = 86,
                noisePenalty = 8,
                lightPenalty = 2,
                motionPenalty = 4,
                distractionPenalty = 0,
                suggestions = listOf("整体环境稳定。")
            )
        )

        val uiState = detail.toUiState()

        assertEquals(9L, uiState.sessionId)
        assertEquals("专注详情 #9", uiState.title)
        assertEquals("3m", uiState.durationText)
        assertEquals("86", uiState.scoreText)
        assertEquals("噪声 -8 · 光照 -2 · 移动 -4 · 分心 -0", uiState.scoreBreakdownText)
        assertEquals("整体环境稳定。", uiState.suggestionText)
    }
}
