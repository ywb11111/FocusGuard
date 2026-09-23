package com.ywb.focusguard.ui.state

import com.ywb.focusguard.domain.model.FocusConfig
import com.ywb.focusguard.domain.model.FocusSession
import com.ywb.focusguard.domain.model.LightLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

/** 验证实时字段变化不会再次触发页面级动画，同时合法状态切换仍有独立 key。 */
class SessionVisualPhaseTest {

    @Test
    fun `ready configuration updates keep the same visual phase`() {
        val first = SessionUiState.Ready(FocusConfig(durationMinutes = 25), null)
        val updated = SessionUiState.Ready(FocusConfig(durationMinutes = 45), null)

        assertEquals(first.visualPhase(), updated.visualPhase())
    }

    @Test
    fun `running timer ticks keep the same visual phase`() {
        val first = running(elapsedMillis = 0L, remainingMillis = 1_500_000L)
        val nextSecond = running(elapsedMillis = 1_000L, remainingMillis = 1_499_000L)

        assertEquals(first.visualPhase(), nextSecond.visualPhase())
    }

    @Test
    fun `real state transitions use different visual phases`() {
        val ready = SessionUiState.Ready(FocusConfig(durationMinutes = 25), null)
        val running = running(elapsedMillis = 0L, remainingMillis = 1_500_000L)
        val paused = SessionUiState.Paused(1L, 10_000L, 1_490_000L, LightLevel.COMFORTABLE, 0)
        val finished = SessionUiState.Finished(
            FocusSession(1L, 1L, 2L, 1_000L, 30f, 40f, 250f, 0, 0, 90, null)
        )

        val phases = listOf(ready, running, paused, finished).map(SessionUiState::visualPhase)
        phases.zipWithNext().forEach { (from, to) -> assertNotEquals(from, to) }
    }

    private fun running(elapsedMillis: Long, remainingMillis: Long) = SessionUiState.Running(
        sessionId = 1L,
        elapsedMillis = elapsedMillis,
        remainingMillis = remainingMillis,
        noiseSamples = emptyList(),
        lightLevel = LightLevel.COMFORTABLE,
        movementCount = 0
    )
}
