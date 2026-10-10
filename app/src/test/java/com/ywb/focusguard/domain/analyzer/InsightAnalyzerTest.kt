package com.ywb.focusguard.domain.analyzer

import com.ywb.focusguard.domain.model.FocusSession
import com.ywb.focusguard.domain.model.InsightKind
import com.ywb.focusguard.domain.model.SleepPlacement
import com.ywb.focusguard.domain.model.SleepSession
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

/**
 * 验证睡眠 × 专注洞察：按"醒来那天"配对、样本不足时的提示、分组对比结论和时段洞察。
 * 固定使用 UTC。
 */
class InsightAnalyzerTest {
    private val analyzer = InsightAnalyzer()
    private val utc = TimeZone.getTimeZone("UTC")
    private val hour = 60 * 60 * 1000L

    private fun at(day: Int, h: Int): Long = Calendar.getInstance(utc).apply {
        clear()
        set(2026, Calendar.SEPTEMBER, day, h, 0)
    }.timeInMillis

    /** 第 [day] 天晚上 [bedHour] 点开始、第二天 7 点醒来的一晚睡眠。 */
    private fun night(day: Int, bedHour: Int, sleepHours: Float, score: Int = 80): SleepSession {
        val start = if (bedHour >= 18) at(day, bedHour) else at(day + 1, bedHour)
        return SleepSession(
            id = day.toLong(),
            startTime = start,
            endTime = at(day + 1, 7),
            placement = SleepPlacement.BED,
            audioEnabled = false,
            sleepOnsetTime = start,
            finalWakeTime = at(day + 1, 7),
            monitoredMillis = (sleepHours * hour).toLong(),
            totalSleepMillis = (sleepHours * hour).toLong(),
            sleepLatencyMillis = 0L,
            wakeCount = 0,
            snoreEventCount = 0,
            averageNoiseDb = 0f,
            averageLightLux = 0f,
            score = score,
            note = null
        )
    }

    private fun focus(day: Int, h: Int, score: Int) = FocusSession(
        id = (day * 100 + h).toLong(),
        startTime = at(day, h),
        endTime = at(day, h) + hour / 2,
        durationMillis = hour / 2,
        averageNoiseDb = 30f,
        maxNoiseDb = 40f,
        averageLightLux = 200f,
        movementCount = 0,
        distractionCount = 0,
        score = score,
        note = null
    )

    @Test
    fun `样本不足时提示还需记录几天`() {
        val insights = analyzer.analyze(
            focusSessions = listOf(focus(2, 10, 80)),
            sleepSessions = listOf(night(1, 23, 8f)),
            nowMillis = at(3, 12),
            timeZone = utc
        )

        val hint = insights.single { it.kind == InsightKind.NEED_MORE_DATA }
        assertTrue(hint.title.contains("5 天"))
    }

    @Test
    fun `睡够 7 小时的第二天专注更好时给出分差`() {
        // 第 1~3 晚睡 8 小时，第二天专注 85 分；第 4~6 晚睡 5 小时，第二天专注 70 分
        val sleeps = (1..3).map { night(it, 23, 8f) } + (4..6).map { night(it, 23, 5f) }
        val focus = (2..4).map { focus(it, 10, 85) } + (5..7).map { focus(it, 10, 70) }

        val insights = analyzer.analyze(focus, sleeps, nowMillis = at(8, 12), timeZone = utc)

        val duration = insights.first { it.kind == InsightKind.SLEEP_DURATION }
        assertEquals("睡够 7 小时的第二天，专注评分平均高 15 分", duration.title)
        assertTrue(insights.none { it.kind == InsightKind.NEED_MORE_DATA })
    }

    @Test
    fun `分差很小时如实说差别不大`() {
        val sleeps = (1..3).map { night(it, 23, 8f) } + (4..6).map { night(it, 23, 5f) }
        val focus = (2..4).map { focus(it, 10, 80) } + (5..7).map { focus(it, 10, 79) }

        val insights = analyzer.analyze(focus, sleeps, nowMillis = at(8, 12), timeZone = utc)

        assertTrue(insights.first { it.kind == InsightKind.SLEEP_DURATION }.title.contains("差别不大"))
    }

    @Test
    fun `零点前后入睡分组对比`() {
        val sleeps = (1..3).map { night(it, 22, 8f) } + (4..6).map { night(it, 2, 8f) }
        val focus = (2..4).map { focus(it, 10, 88) } + (5..7).map { focus(it, 10, 75) }

        val insights = analyzer.analyze(focus, sleeps, nowMillis = at(8, 12), timeZone = utc)

        val bedtime = insights.firstOrNull { it.kind == InsightKind.BEDTIME }
        assertNotNull(bedtime)
        assertEquals("零点前入睡的第二天，专注评分平均高 13 分", bedtime!!.title)
    }

    @Test
    fun `午睡不参与前一晚统计`() {
        val nap = night(1, 23, 1.5f)

        val insights = analyzer.analyze(listOf(focus(2, 10, 80)), listOf(nap), nowMillis = at(3, 12), timeZone = utc)

        assertTrue(insights.single { it.kind == InsightKind.NEED_MORE_DATA }.detail.contains("目前有 0 天"))
    }

    @Test
    fun `找出专注评分最高的时段`() {
        val focus = (1..3).map { focus(it, 9, 90) } + (1..3).map { focus(it, 21, 70) }

        val insights = analyzer.analyze(focus, emptyList(), nowMillis = at(5, 12), timeZone = utc)

        assertEquals("你在上午的专注评分最高", insights.first { it.kind == InsightKind.BEST_TIME_OF_DAY }.title)
    }
}
