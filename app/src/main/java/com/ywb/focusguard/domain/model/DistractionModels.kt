package com.ywb.focusguard.domain.model

/**
 * 系统记录的一次"某个 App 进入前台 / 退到后台"事件，来自 UsageStatsManager。
 *
 * 由 AppUsageDataSource 从系统事件转换而来，交给 DistractionAnalyzer 计算分心片段。
 * 用自己的模型而不是直接传 UsageEvents.Event：后者是 Android 类，纯 Kotlin 的分析器和单元测试都用不了。
 *
 * @property timestamp 事件发生的 Unix 毫秒时间戳。
 * @property packageName 事件所属 App 的包名。
 * @property isForeground true 表示进入前台（ACTIVITY_RESUMED），false 表示退到后台（ACTIVITY_PAUSED）。
 */
data class AppForegroundEvent(
    val timestamp: Long,
    val packageName: String,
    val isForeground: Boolean
)

/**
 * 专注真正"在计时"的一段时间。暂停期间切去别的 App 属于休息，不算分心，所以分析只看这些区间。
 *
 * 由 SessionViewModel 在开始、暂停、继续、结束时记录。
 *
 * @property start 区间开始的 Unix 毫秒时间戳。
 * @property end 区间结束的 Unix 毫秒时间戳。
 */
data class ActiveInterval(
    val start: Long,
    val end: Long
)

/**
 * 一次分心：专注计时期间，某个其他 App 在前台停留了一段时间。
 *
 * @property packageName App 包名。
 * @property appLabel App 显示名称；取不到时退回包名。
 * @property startTime 切到这个 App 的时间。
 * @property endTime 离开这个 App 的时间。
 */
data class DistractionEvent(
    val packageName: String,
    val appLabel: String,
    val startTime: Long,
    val endTime: Long
)

/**
 * 按 App 聚合后的分心统计，详情页直接展示"抖音 3 次 · 6 分钟"。
 *
 * @property packageName App 包名。
 * @property appLabel App 显示名称。
 * @property count 分心次数。
 * @property totalMillis 在这个 App 里的累计时长。
 */
data class DistractionSummary(
    val packageName: String,
    val appLabel: String,
    val count: Int,
    val totalMillis: Long
)
