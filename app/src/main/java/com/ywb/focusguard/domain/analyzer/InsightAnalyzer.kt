package com.ywb.focusguard.domain.analyzer

import com.ywb.focusguard.domain.model.FocusSession
import com.ywb.focusguard.domain.model.Insight
import com.ywb.focusguard.domain.model.InsightKind
import com.ywb.focusguard.domain.model.SleepSession
import java.util.Calendar
import java.util.TimeZone
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * 睡眠 × 专注关联洞察：把"前一晚睡得怎样"和"第二天专注得怎样"放在一起比较。
 * 纯 Kotlin，可直接单元测试。
 *
 * **怎么把两类数据对上**：以"醒来那天"为键。某晚睡眠的结束时间落在 D 日，就是 D 日专注的"前一晚"。
 * 只统计 3 小时以上的睡眠，排除午睡；同一天有多段时取最长的一段。
 *
 * **为什么要设最少样本数**：
 * 只有 1、2 天数据时，"睡得多那天专注好"很可能只是巧合。每组至少 [minSamplesPerGroup] 天才下结论，
 * 不够时明确告诉用户"还需要记录几天"，而不是给出看似精确、实则不可靠的数字。
 * 即便如此，这仍是相关性而非因果，页面会注明。
 *
 * @property minSamplesPerGroup 对比两组时，每组最少需要的天数。
 * @property minMeaningfulDiff 分差小于它视为"差别不大"，避免把 1、2 分的波动说成规律。
 */
class InsightAnalyzer(
    private val minSamplesPerGroup: Int = 3,
    private val minMeaningfulDiff: Int = 3
) {

    /** 某一天"前一晚睡眠 + 当天专注"的配对结果。 */
    private data class DayPair(
        val sleep: SleepSession,
        val focusScore: Double
    )

    /**
     * 生成洞察列表，最有价值的排在前面。
     *
     * @param focusSessions 统计周期内已完成的专注记录。
     * @param sleepSessions 统计周期内已完成的睡眠记录。
     * @param nowMillis 当前时间，用于划分最近一周和上一周。
     */
    fun analyze(
        focusSessions: List<FocusSession>,
        sleepSessions: List<SleepSession>,
        nowMillis: Long,
        timeZone: TimeZone = TimeZone.getDefault()
    ): List<Insight> {
        val finishedFocus = focusSessions.filter { it.endTime != null }
        val nightSleeps = sleepSessions.filter { it.endTime != null && it.totalSleepMillis >= MIN_NIGHT_SLEEP_MILLIS }

        // 当天平均专注评分，键是当天 0 点
        val focusScoreByDay = finishedFocus
            .groupBy { dayStart(it.startTime, timeZone) }
            .mapValues { (_, items) -> items.map { it.score }.average() }
        // 醒来那天对应的睡眠，键是醒来当天 0 点
        val sleepByWakeDay = nightSleeps
            .groupBy { dayStart(it.endTime ?: it.startTime, timeZone) }
            .mapValues { (_, items) -> items.maxBy { it.totalSleepMillis } }
        val pairs = focusScoreByDay.mapNotNull { (day, score) ->
            sleepByWakeDay[day]?.let { DayPair(sleep = it, focusScore = score) }
        }

        val insights = mutableListOf<Insight>()
        sleepDurationInsight(pairs)?.let(insights::add)
        bedtimeInsight(pairs, timeZone)?.let(insights::add)
        sleepTrendInsight(nightSleeps, nowMillis)?.let(insights::add)
        bestTimeOfDayInsight(finishedFocus, timeZone)?.let(insights::add)

        if (insights.none { it.kind == InsightKind.SLEEP_DURATION }) {
            val needed = (minSamplesPerGroup * 2 - pairs.size).coerceAtLeast(1)
            insights += Insight(
                kind = InsightKind.NEED_MORE_DATA,
                title = "再记录约 $needed 天，就能看到睡眠对专注的影响",
                detail = "需要同一天既有前一晚的睡眠记录、又有专注记录，目前有 ${pairs.size} 天"
            )
        }
        return insights
    }

    /** 睡够 7 小时 vs 不足 7 小时，第二天专注评分对比。 */
    private fun sleepDurationInsight(pairs: List<DayPair>): Insight? {
        val (enough, short) = pairs.partition { it.sleep.totalSleepMillis >= ENOUGH_SLEEP_MILLIS }
        return compareGroups(
            kind = InsightKind.SLEEP_DURATION,
            groupA = enough,
            groupB = short,
            labelA = "睡够 7 小时",
            labelB = "不足 7 小时"
        )
    }

    /** 零点前入睡 vs 零点后入睡，第二天专注评分对比。 */
    private fun bedtimeInsight(pairs: List<DayPair>, timeZone: TimeZone): Insight? {
        val (early, late) = pairs.partition { pair ->
            val onset = pair.sleep.sleepOnsetTime ?: pair.sleep.startTime
            val hour = Calendar.getInstance(timeZone).apply { timeInMillis = onset }.get(Calendar.HOUR_OF_DAY)
            // 18 点到 23 点之间入睡算"零点前"；0 点到 12 点算"零点后"
            hour >= 18
        }
        return compareGroups(
            kind = InsightKind.BEDTIME,
            groupA = early,
            groupB = late,
            labelA = "零点前入睡",
            labelB = "零点后入睡"
        )
    }

    /**
     * 两组对比的通用逻辑：任一组样本不足返回 null；分差太小如实说"差别不大"。
     */
    private fun compareGroups(
        kind: InsightKind,
        groupA: List<DayPair>,
        groupB: List<DayPair>,
        labelA: String,
        labelB: String
    ): Insight? {
        if (groupA.size < minSamplesPerGroup || groupB.size < minSamplesPerGroup) return null
        val averageA = groupA.map { it.focusScore }.average().roundToInt()
        val averageB = groupB.map { it.focusScore }.average().roundToInt()
        val diff = averageA - averageB
        val title = when {
            abs(diff) < minMeaningfulDiff -> "${labelA}和${labelB}的第二天，专注评分差别不大"
            diff > 0 -> "${labelA}的第二天，专注评分平均高 $diff 分"
            else -> "${labelB}的第二天，专注评分反而高 ${-diff} 分"
        }
        return Insight(
            kind = kind,
            title = title,
            detail = "$labelA $averageA 分（${groupA.size} 天）· $labelB $averageB 分（${groupB.size} 天）"
        )
    }

    /** 最近 7 天 vs 之前 7 天的平均睡眠评分。 */
    private fun sleepTrendInsight(nightSleeps: List<SleepSession>, nowMillis: Long): Insight? {
        val weekAgo = nowMillis - WEEK_MILLIS
        val twoWeeksAgo = nowMillis - 2 * WEEK_MILLIS
        val thisWeek = nightSleeps.filter { it.startTime >= weekAgo }
        val lastWeek = nightSleeps.filter { it.startTime in twoWeeksAgo until weekAgo }
        if (thisWeek.size < minSamplesPerGroup || lastWeek.size < minSamplesPerGroup) return null
        val thisAverage = thisWeek.map { it.score }.average().roundToInt()
        val lastAverage = lastWeek.map { it.score }.average().roundToInt()
        val diff = thisAverage - lastAverage
        val title = when {
            abs(diff) < minMeaningfulDiff -> "最近一周睡眠评分保持稳定"
            diff > 0 -> "最近一周睡眠评分提高了 $diff 分"
            else -> "最近一周睡眠评分下降了 ${-diff} 分"
        }
        return Insight(
            kind = InsightKind.SLEEP_TREND,
            title = title,
            detail = "最近 7 天平均 $thisAverage 分（${thisWeek.size} 晚）· 之前 7 天平均 $lastAverage 分（${lastWeek.size} 晚）"
        )
    }

    /** 上午 / 下午 / 晚上，哪个时段专注评分最高；至少两个时段样本充足才比较。 */
    private fun bestTimeOfDayInsight(focusSessions: List<FocusSession>, timeZone: TimeZone): Insight? {
        val buckets = focusSessions.groupBy { session ->
            when (Calendar.getInstance(timeZone).apply { timeInMillis = session.startTime }.get(Calendar.HOUR_OF_DAY)) {
                in 5..11 -> "上午"
                in 12..17 -> "下午"
                else -> "晚上"
            }
        }.filterValues { it.size >= minSamplesPerGroup }
        if (buckets.size < 2) return null
        val ranked = buckets
            .mapValues { (_, items) -> items.map { it.score }.average().roundToInt() }
            .entries
            .sortedByDescending { it.value }
        val best = ranked.first()
        val worst = ranked.last()
        return Insight(
            kind = InsightKind.BEST_TIME_OF_DAY,
            title = "你在${best.key}的专注评分最高",
            detail = ranked.joinToString(" · ") { "${it.key} ${it.value} 分（${buckets.getValue(it.key).size} 次）" } +
                if (best.value - worst.value >= minMeaningfulDiff) "，重要任务可以优先安排在${best.key}" else ""
        )
    }

    /** 时间戳所在自然日的 0 点。 */
    private fun dayStart(timestamp: Long, timeZone: TimeZone): Long =
        Calendar.getInstance(timeZone).apply {
            timeInMillis = timestamp
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

    private companion object {
        /** 少于 3 小时的睡眠视为午睡，不参与"前一晚"统计。 */
        const val MIN_NIGHT_SLEEP_MILLIS = 3 * 60 * 60 * 1000L

        /** "睡够"的标准：7 小时。 */
        const val ENOUGH_SLEEP_MILLIS = 7 * 60 * 60 * 1000L

        const val WEEK_MILLIS = 7 * 24 * 60 * 60 * 1000L
    }
}
