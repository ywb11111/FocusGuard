package com.ywb.focusguard.domain.analyzer

import com.ywb.focusguard.domain.model.SleepAlarmSettings
import com.ywb.focusguard.domain.model.SleepEpoch
import com.ywb.focusguard.domain.model.SleepPlacement
import com.ywb.focusguard.domain.model.SmartAlarmPlan
import java.util.Calendar
import java.util.TimeZone

/**
 * 智能闹钟的纯逻辑部分：算出今晚的闹钟时间，并判断"现在是不是叫醒的好时机"。
 * 不涉及 AlarmManager 和响铃，单元测试可以直接覆盖。
 *
 * **为什么"浅睡"用体动判断**：
 * 深睡时肌肉放松、几乎不动；浅睡和快醒来时翻身、小动作明显增多。
 * 这和睡眠分析用的是同一个信号，只是这里要实时判断最近几分钟，而不是整晚回看。
 */
class SmartAlarmPlanner {

    companion object {
        /** 最近一分钟体动达到多少秒视为浅睡，需真机校准。 */
        const val LIGHT_SLEEP_MOVEMENT_SECONDS = 3

        /** 最近两分钟体动累计达到多少秒也视为浅睡（连续的小动作）。 */
        const val LIGHT_SLEEP_TWO_MINUTE_SECONDS = 5

        /** 床头柜模式下，最近一分钟非鼾声的声音事件达到多少次视为浅睡（翻身摩擦声）。 */
        const val LIGHT_SLEEP_SOUND_EVENTS = 2

        /**
         * 开始监测后至少过这么久才允许提前唤醒。
         * 防止"6:50 才躺下、窗口 7:00 开始"时，把刚躺下调整姿势的体动误当成浅睡、一进窗口就响铃；
         * 未满时只保留最晚时间的准点闹钟。
         */
        const val MIN_SLEEP_BEFORE_EARLY_WAKE_MILLIS = 60 * 60 * 1000L
    }

    /**
     * 计算 [afterMillis] 之后第一次到达设定时间的闹钟计划。
     * 例如 23:30 开始监测、设定 7:30，得到的是第二天 7:30；凌晨 1 点开始也是当天 7:30。
     *
     * @return 未开启闹钟时返回 null。
     */
    fun plan(
        settings: SleepAlarmSettings,
        afterMillis: Long,
        timeZone: TimeZone = TimeZone.getDefault()
    ): SmartAlarmPlan? {
        if (!settings.enabled) return null
        val latest = Calendar.getInstance(timeZone).apply {
            timeInMillis = afterMillis
            set(Calendar.HOUR_OF_DAY, settings.wakeHour)
            set(Calendar.MINUTE, settings.wakeMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= afterMillis) add(Calendar.DAY_OF_YEAR, 1)
        }.timeInMillis
        return SmartAlarmPlan(
            windowStartMillis = latest - settings.windowMinutes * 60_000L,
            latestMillis = latest
        )
    }

    /**
     * 判断是否应该提前叫醒。
     *
     * @param recentEpochs 最近几分钟的数据，按时间升序，最后一个是刚结束的一分钟。
     * @param nowMillis 当前时间。
     * @param sessionStartMillis 本次开始监测的时间。
     * @return 处于唤醒窗口内、已睡够最短时长且检测到浅睡信号时返回 true。
     */
    fun shouldWakeEarly(
        recentEpochs: List<SleepEpoch>,
        nowMillis: Long,
        plan: SmartAlarmPlan,
        placement: SleepPlacement,
        sessionStartMillis: Long
    ): Boolean {
        if (nowMillis < plan.windowStartMillis || nowMillis >= plan.latestMillis) return false
        if (nowMillis - sessionStartMillis < MIN_SLEEP_BEFORE_EARLY_WAKE_MILLIS) return false
        val last = recentEpochs.lastOrNull() ?: return false
        // 已经在看手机了，说明人醒着，没必要再等
        if (last.screenOnSeconds > 0) return true
        return when (placement) {
            SleepPlacement.BED -> {
                val lastTwo = recentEpochs.takeLast(2).sumOf { it.movementSeconds }
                last.movementSeconds >= LIGHT_SLEEP_MOVEMENT_SECONDS || lastTwo >= LIGHT_SLEEP_TWO_MINUTE_SECONDS
            }
            SleepPlacement.NIGHTSTAND ->
                last.soundEventCount - last.snoreEventCount >= LIGHT_SLEEP_SOUND_EVENTS
        }
    }
}
