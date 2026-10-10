package com.ywb.focusguard.domain.analyzer

import com.ywb.focusguard.domain.model.SleepAlarmSettings
import com.ywb.focusguard.domain.model.SleepEpoch
import com.ywb.focusguard.domain.model.SleepPlacement
import com.ywb.focusguard.domain.model.SmartAlarmPlan
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

/** 验证闹钟时间计算（跨午夜）和浅睡提前唤醒判断。固定使用 UTC，结果不受运行机器时区影响。 */
class SmartAlarmPlannerTest {
    private val planner = SmartAlarmPlanner()
    private val utc = TimeZone.getTimeZone("UTC")

    /** 2026-10-10 指定时刻（UTC）的毫秒时间戳。 */
    private fun at(day: Int, hour: Int, minute: Int): Long = Calendar.getInstance(utc).apply {
        clear()
        set(2026, Calendar.OCTOBER, day, hour, minute)
    }.timeInMillis

    private fun epoch(movementSeconds: Int = 0, screenOnSeconds: Int = 0, soundEvents: Int = 0) = SleepEpoch(
        startTime = 0L,
        movementSeconds = movementSeconds,
        averageNoiseDb = 30f,
        maxNoiseDb = 30f,
        soundEventCount = soundEvents,
        snoreEventCount = 0,
        averageLightLux = 0f,
        screenOnSeconds = screenOnSeconds
    )

    private val enabled = SleepAlarmSettings(enabled = true, wakeHour = 7, wakeMinute = 30, windowMinutes = 30)

    /** 前一晚 23:30 开始监测，到窗口时早已睡够最短时长。 */
    private val asleepAt by lazy { at(10, 23, 30) }

    @Test
    fun `刚躺下不满 1 小时时即使有体动也不提前`() {
        val plan = SmartAlarmPlan(windowStartMillis = at(11, 7, 0), latestMillis = at(11, 7, 30))

        assertFalse(
            planner.shouldWakeEarly(
                listOf(epoch(movementSeconds = 10)),
                nowMillis = at(11, 7, 5),
                plan = plan,
                placement = SleepPlacement.BED,
                sessionStartMillis = at(11, 6, 50)
            )
        )
    }

    @Test
    fun `晚上开始监测时闹钟是第二天早上`() {
        val plan = planner.plan(enabled, afterMillis = at(10, 23, 30), timeZone = utc)!!

        assertEquals(at(11, 7, 30), plan.latestMillis)
        assertEquals(at(11, 7, 0), plan.windowStartMillis)
    }

    @Test
    fun `凌晨开始监测时闹钟是当天早上`() {
        val plan = planner.plan(enabled, afterMillis = at(11, 1, 0), timeZone = utc)!!

        assertEquals(at(11, 7, 30), plan.latestMillis)
    }

    @Test
    fun `未开启闹钟时没有计划`() {
        assertNull(planner.plan(SleepAlarmSettings(enabled = false), afterMillis = at(10, 23, 0), timeZone = utc))
    }

    @Test
    fun `窗口内检测到体动时提前唤醒`() {
        val plan = SmartAlarmPlan(windowStartMillis = at(11, 7, 0), latestMillis = at(11, 7, 30))

        assertTrue(planner.shouldWakeEarly(listOf(epoch(), epoch(movementSeconds = 4)), at(11, 7, 10), plan, SleepPlacement.BED, asleepAt))
        assertTrue(planner.shouldWakeEarly(listOf(epoch(movementSeconds = 3), epoch(movementSeconds = 2)), at(11, 7, 10), plan, SleepPlacement.BED, asleepAt))
    }

    @Test
    fun `窗口外或安稳睡眠时不提前`() {
        val plan = SmartAlarmPlan(windowStartMillis = at(11, 7, 0), latestMillis = at(11, 7, 30))

        assertFalse(planner.shouldWakeEarly(listOf(epoch(movementSeconds = 10)), at(11, 6, 50), plan, SleepPlacement.BED, asleepAt))
        assertFalse(planner.shouldWakeEarly(listOf(epoch(), epoch(movementSeconds = 1)), at(11, 7, 10), plan, SleepPlacement.BED, asleepAt))
    }

    @Test
    fun `已经在看手机时直接叫醒`() {
        val plan = SmartAlarmPlan(windowStartMillis = at(11, 7, 0), latestMillis = at(11, 7, 30))

        assertTrue(planner.shouldWakeEarly(listOf(epoch(screenOnSeconds = 20)), at(11, 7, 5), plan, SleepPlacement.NIGHTSTAND, asleepAt))
    }

    @Test
    fun `床头柜模式用声音事件判断浅睡`() {
        val plan = SmartAlarmPlan(windowStartMillis = at(11, 7, 0), latestMillis = at(11, 7, 30))

        assertTrue(planner.shouldWakeEarly(listOf(epoch(soundEvents = 2)), at(11, 7, 5), plan, SleepPlacement.NIGHTSTAND, asleepAt))
        assertFalse(planner.shouldWakeEarly(listOf(epoch(movementSeconds = 10)), at(11, 7, 5), plan, SleepPlacement.NIGHTSTAND, asleepAt))
    }
}
