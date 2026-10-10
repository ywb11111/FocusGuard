package com.ywb.focusguard.domain.analyzer

import com.ywb.focusguard.domain.model.ActiveInterval
import com.ywb.focusguard.domain.model.AppForegroundEvent
import com.ywb.focusguard.domain.model.DistractionEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 验证分心分析：前台时间线还原、暂停与锁屏排除、忽略名单、短停留过滤、同 App 合并和按 App 聚合。
 * 时间单位都是毫秒，从 0 开始构造，结果完全确定。
 */
class DistractionAnalyzerTest {
    private val analyzer = DistractionAnalyzer()
    private val self = "com.ywb.focusguard"
    private val ignored = setOf(self, "com.launcher")

    private fun fg(time: Long, pkg: String) = AppForegroundEvent(time, pkg, isForeground = true)
    private fun bg(time: Long, pkg: String) = AppForegroundEvent(time, pkg, isForeground = false)

    @Test
    fun `切到其他 App 停留 30 秒算一次分心`() {
        val events = listOf(bg(10_000, self), fg(10_000, "com.video"), bg(40_000, "com.video"), fg(40_000, self))

        val episodes = analyzer.detect(events, listOf(ActiveInterval(0, 100_000)), ignored)

        assertEquals(1, episodes.size)
        assertEquals("com.video", episodes[0].packageName)
        assertEquals(30_000L, episodes[0].end - episodes[0].start)
    }

    @Test
    fun `桌面和自身不算分心`() {
        val events = listOf(fg(10_000, "com.launcher"), fg(50_000, self))

        val episodes = analyzer.detect(events, listOf(ActiveInterval(0, 100_000)), ignored)

        assertTrue(episodes.isEmpty())
    }

    @Test
    fun `锁屏后退到后台的时间不算分心`() {
        // 在视频 App 里 10 秒后锁屏，之后没有任何前台 App
        val events = listOf(fg(10_000, "com.video"), bg(20_000, "com.video"))

        val episodes = analyzer.detect(events, listOf(ActiveInterval(0, 100_000)), ignored)

        assertEquals(1, episodes.size)
        assertEquals(20_000L, episodes[0].end)
    }

    @Test
    fun `暂停期间使用其他 App 不算分心`() {
        // 计时 0~30 秒，暂停 30~90 秒，继续 90~120 秒；在暂停期间刷了 50 秒视频
        val events = listOf(fg(35_000, "com.video"), fg(85_000, self))
        val intervals = listOf(ActiveInterval(0, 30_000), ActiveInterval(90_000, 120_000))

        val episodes = analyzer.detect(events, intervals, ignored)

        assertTrue(episodes.isEmpty())
    }

    @Test
    fun `停留不足 5 秒的误触被过滤`() {
        val events = listOf(fg(10_000, "com.chat"), fg(13_000, self))

        val episodes = analyzer.detect(events, listOf(ActiveInterval(0, 100_000)), ignored)

        assertTrue(episodes.isEmpty())
    }

    @Test
    fun `同一 App 内切换页面产生的短暂间隔被合并为一次`() {
        // App 内跳转：先退出旧页面再进入新页面，中间 1 秒空档
        val events = listOf(
            fg(10_000, "com.chat"),
            bg(20_000, "com.chat"),
            fg(21_000, "com.chat"),
            fg(40_000, self)
        )

        val episodes = analyzer.detect(events, listOf(ActiveInterval(0, 100_000)), ignored)

        assertEquals(1, episodes.size)
        assertEquals(10_000L, episodes[0].start)
        assertEquals(40_000L, episodes[0].end)
    }

    @Test
    fun `按 App 聚合并按累计时长降序`() {
        val events = listOf(
            DistractionEvent("com.chat", "微信", 0, 10_000),
            DistractionEvent("com.video", "抖音", 20_000, 80_000),
            DistractionEvent("com.chat", "微信", 90_000, 100_000)
        )

        val summaries = analyzer.summarize(events)

        assertEquals(listOf("抖音", "微信"), summaries.map { it.appLabel })
        assertEquals(2, summaries[1].count)
        assertEquals(20_000L, summaries[1].totalMillis)
    }
}
