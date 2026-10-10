package com.ywb.focusguard.domain.model

/**
 * 洞察类型，页面据此选择图标，周报通知据此挑选最有价值的一条。
 */
enum class InsightKind {
    /** 前一晚睡眠时长与第二天专注评分的关系。 */
    SLEEP_DURATION,

    /** 入睡早晚与第二天专注评分的关系。 */
    BEDTIME,

    /** 最近两周睡眠评分的变化。 */
    SLEEP_TREND,

    /** 一天中哪个时段专注评分最高。 */
    BEST_TIME_OF_DAY,

    /** 数据不足，提示还需记录多少天。 */
    NEED_MORE_DATA
}

/**
 * 一条由 InsightAnalyzer 从真实记录中算出的结论。
 *
 * @property kind 洞察类型。
 * @property title 一句话结论，例如"睡够 7 小时的第二天，专注评分平均高 12 分"。
 * @property detail 支撑结论的数据，例如"睡够 7 小时 82 分（5 天）· 不足 7 小时 70 分（4 天）"。
 */
data class Insight(
    val kind: InsightKind,
    val title: String,
    val detail: String
)
