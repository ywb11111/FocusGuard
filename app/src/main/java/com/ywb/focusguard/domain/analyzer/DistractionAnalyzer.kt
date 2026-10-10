package com.ywb.focusguard.domain.analyzer

import com.ywb.focusguard.domain.model.ActiveInterval
import com.ywb.focusguard.domain.model.AppForegroundEvent
import com.ywb.focusguard.domain.model.DistractionEvent
import com.ywb.focusguard.domain.model.DistractionSummary

/**
 * 分心分析器：把系统的"App 进入/退出前台"事件流，还原成"专注期间在哪个 App 停留了多久"。
 * 纯 Kotlin，不依赖 Android，可直接单元测试。
 *
 * **算法分三步**
 * 1. 还原前台时间线：按时间顺序扫描事件，某个 App 进入前台就开始一段，换成别的 App 或它退到后台就结束这段。
 *    熄屏时当前 App 会退到后台，时间线变成"无前台 App"，所以锁屏不会被误算成分心。
 * 2. 只保留"计时区间"内、且不在忽略名单里的片段：暂停期间、FocusGuard 自己、桌面、来电界面都不算。
 * 3. 同一个 App 间隔很短的相邻片段合并（App 内切换页面会产生一对 退出/进入 事件），
 *    再丢掉停留太短的片段（误触、通知栏一闪）。
 *
 * @property minDistractionMillis 至少停留多久才算一次分心，需要真机按使用习惯调整。
 * @property mergeGapMillis 同一 App 两段之间间隔小于它就合并为一次。
 */
class DistractionAnalyzer(
    private val minDistractionMillis: Long = 5_000L,
    private val mergeGapMillis: Long = 3_000L
) {

    /**
     * 一段原始片段（分析过程中的中间结果）。
     *
     * @property packageName 当时在前台的 App。
     * @property start 片段开始时间。
     * @property end 片段结束时间。
     */
    data class Episode(
        val packageName: String,
        val start: Long,
        val end: Long
    )

    /**
     * 计算分心片段。
     *
     * @param events 时间范围内的前台事件，顺序不限。
     * @param intervals 专注计时区间。
     * @param ignoredPackages 不算分心的包名（自身、桌面、来电等）。
     * @return 按时间排序的分心片段。
     */
    fun detect(
        events: List<AppForegroundEvent>,
        intervals: List<ActiveInterval>,
        ignoredPackages: Set<String>
    ): List<Episode> {
        if (intervals.isEmpty()) return emptyList()
        val rangeStart = intervals.minOf { it.start }
        val rangeEnd = intervals.maxOf { it.end }

        // 第 1 步：还原前台时间线。开始时前台默认是 FocusGuard 自己（用户刚点了开始），记为 null 即"不算分心"
        val timeline = mutableListOf<Episode>()
        var currentPackage: String? = null
        var currentStart = rangeStart
        fun closeCurrent(end: Long) {
            currentPackage?.let { pkg -> if (end > currentStart) timeline += Episode(pkg, currentStart, end) }
        }
        events.sortedBy { it.timestamp }.forEach { event ->
            if (event.isForeground) {
                if (event.packageName != currentPackage) {
                    closeCurrent(event.timestamp)
                    currentPackage = event.packageName
                    currentStart = event.timestamp
                }
            } else if (event.packageName == currentPackage) {
                closeCurrent(event.timestamp)
                currentPackage = null
                currentStart = event.timestamp
            }
        }
        closeCurrent(rangeEnd)

        // 第 2 步：去掉忽略名单，并裁剪到计时区间内
        val clipped = timeline
            .filter { it.packageName !in ignoredPackages }
            .flatMap { episode ->
                intervals.mapNotNull { interval ->
                    val start = maxOf(episode.start, interval.start)
                    val end = minOf(episode.end, interval.end)
                    if (end > start) Episode(episode.packageName, start, end) else null
                }
            }
            .sortedBy { it.start }

        // 第 3 步：合并同一 App 的相邻片段，再过滤太短的
        val merged = mutableListOf<Episode>()
        clipped.forEach { episode ->
            val last = merged.lastOrNull()
            if (last != null && last.packageName == episode.packageName &&
                episode.start - last.end <= mergeGapMillis
            ) {
                merged[merged.lastIndex] = last.copy(end = maxOf(last.end, episode.end))
            } else {
                merged += episode
            }
        }
        return merged.filter { it.end - it.start >= minDistractionMillis }
    }

    /**
     * 按 App 聚合分心记录，按累计时长从多到少排序，详情页把"最占时间的 App"放在最前。
     */
    fun summarize(events: List<DistractionEvent>): List<DistractionSummary> =
        events
            .groupBy { it.packageName }
            .map { (packageName, items) ->
                DistractionSummary(
                    packageName = packageName,
                    appLabel = items.first().appLabel,
                    count = items.size,
                    totalMillis = items.sumOf { it.endTime - it.startTime }
                )
            }
            .sortedByDescending { it.totalMillis }
}
