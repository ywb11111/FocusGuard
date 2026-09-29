package com.ywb.focusguard.ui.viewmodel

import com.ywb.focusguard.domain.model.FocusSession
import org.junit.Assert.assertEquals
import org.junit.Test

class ReportsCategorySummaryTest {
    @Test
    fun `标签汇总按总时长排序并计算平均分`() {
        val sessions = listOf(
            session(durationMillis = 1_500_000L, score = 80, note = "学习"),
            session(durationMillis = 1_500_000L, score = 90, note = "学习"),
            session(durationMillis = 2_700_000L, score = 95, note = "编程")
        )

        val summaries = buildCategorySummaries(sessions)

        assertEquals(listOf("学习", "编程"), summaries.map { it.label })
        assertEquals(3_000_000L, summaries.first().totalFocusMillis)
        assertEquals(85, summaries.first().averageScore)
    }

    @Test
    fun `旧自由文本记录统一归入其他`() {
        val summaries = buildCategorySummaries(
            listOf(session(durationMillis = 600_000L, score = 70, note = "算法练习"))
        )

        assertEquals("其他", summaries.single().label)
    }

    private fun session(durationMillis: Long, score: Int, note: String) = FocusSession(
        id = durationMillis,
        startTime = 0L,
        endTime = 1L,
        durationMillis = durationMillis,
        averageNoiseDb = 40f,
        maxNoiseDb = 50f,
        averageLightLux = 250f,
        movementCount = 0,
        distractionCount = 0,
        score = score,
        note = note
    )
}
