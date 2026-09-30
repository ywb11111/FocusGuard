package com.ywb.focusguard.domain.analyzer

import com.ywb.focusguard.domain.model.SleepEpoch
import com.ywb.focusguard.domain.model.SleepPlacement
import com.ywb.focusguard.domain.model.SleepStage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 验证睡眠分析的核心规则：入睡判定、夜醒计数、亮屏判醒、床头柜模式和评分。
 * 输入全部是构造的分钟数据，不依赖传感器，结果完全确定。
 */
class SleepAnalyzerTest {
    private val analyzer = SleepAnalyzer()

    /** 构造一分钟数据，未指定的字段都是"安静、黑暗、熄屏"。 */
    private fun epoch(
        index: Int,
        movementSeconds: Int = 0,
        screenOnSeconds: Int = 0,
        soundEvents: Int = 0,
        snoreEvents: Int = 0,
        lux: Float = 0f
    ) = SleepEpoch(
        startTime = index * SleepAnalyzer.EPOCH_MILLIS,
        movementSeconds = movementSeconds,
        averageNoiseDb = 30f,
        maxNoiseDb = 35f,
        soundEventCount = soundEvents,
        snoreEventCount = snoreEvents,
        averageLightLux = lux,
        screenOnSeconds = screenOnSeconds
    )

    @Test
    fun `没有数据时评分为 0 并给出提示`() {
        val result = analyzer.analyze(emptyList(), SleepPlacement.BED, audioEnabled = true)

        assertEquals(0, result.score.total)
        assertEquals(-1, result.sleepOnsetIndex)
        assertTrue(result.score.suggestions.isNotEmpty())
    }

    @Test
    fun `整晚 8 小时安稳且黑暗时满分`() {
        val epochs = List(480) { epoch(it, movementSeconds = if (it % 60 == 0) 3 else 0) }

        val result = analyzer.analyze(epochs, SleepPlacement.BED, audioEnabled = true)

        assertEquals(0, result.sleepOnsetIndex)
        assertEquals(480 * SleepAnalyzer.EPOCH_MILLIS, result.totalSleepMillis)
        assertEquals(0, result.wakeCount)
        assertEquals(100, result.score.total)
    }

    @Test
    fun `入睡前亮屏的时间计入入睡耗时且标记为清醒`() {
        val epochs = List(30) { epoch(it, screenOnSeconds = 60) } + List(450) { epoch(30 + it) }

        val result = analyzer.analyze(epochs, SleepPlacement.BED, audioEnabled = false)

        assertEquals(30, result.sleepOnsetIndex)
        assertEquals(30 * SleepAnalyzer.EPOCH_MILLIS, result.sleepLatencyMillis)
        assertTrue(result.stages.take(30).all { it == SleepStage.AWAKE })
    }

    @Test
    fun `单次翻身只算浅睡不算夜醒`() {
        val epochs = List(120) { epoch(it, movementSeconds = if (it == 60) 5 else 0) }

        val result = analyzer.analyze(epochs, SleepPlacement.BED, audioEnabled = false)

        assertEquals(SleepStage.RESTLESS, result.stages[60])
        assertEquals(0, result.wakeCount)
    }

    @Test
    fun `连续多分钟辗转反侧计为一次夜醒`() {
        val epochs = List(60) { epoch(it) } +
            List(5) { epoch(60 + it, movementSeconds = 30) } +
            List(60) { epoch(65 + it) }

        val result = analyzer.analyze(epochs, SleepPlacement.BED, audioEnabled = false)

        assertEquals(SleepStage.AWAKE, result.stages[62])
        assertEquals(1, result.wakeCount)
        assertTrue(result.wakeAfterSleepOnsetMillis > 0)
    }

    @Test
    fun `没有连续 10 分钟睡眠时不承认入睡`() {
        val epochs = List(5) { epoch(it) }

        val result = analyzer.analyze(epochs, SleepPlacement.BED, audioEnabled = false)

        assertEquals(-1, result.sleepOnsetIndex)
        assertEquals(0L, result.totalSleepMillis)
        assertEquals(0, result.score.total)
        assertTrue(result.stages.all { it == SleepStage.AWAKE })
    }

    @Test
    fun `床头柜模式下鼾声不算清醒而其他声音算活动`() {
        val snoring = List(60) { epoch(it, soundEvents = 4, snoreEvents = 4) }
        val rustling = List(60) { epoch(it, soundEvents = 3) }

        val snoreResult = analyzer.analyze(snoring, SleepPlacement.NIGHTSTAND, audioEnabled = true)
        val rustleResult = analyzer.analyze(rustling, SleepPlacement.NIGHTSTAND, audioEnabled = true)

        assertEquals(0, snoreResult.sleepOnsetIndex)
        assertEquals(240, snoreResult.snoreEventCount)
        assertEquals(-1, rustleResult.sleepOnsetIndex)
    }

    @Test
    fun `床垫模式整晚无体动时提示手机可能没放好`() {
        val epochs = List(180) { epoch(it) }

        val result = analyzer.analyze(epochs, SleepPlacement.BED, audioEnabled = false)

        assertTrue(result.lowMotionWarning)
    }

    @Test
    fun `睡眠不足和卧室偏亮会扣分`() {
        val epochs = List(300) { epoch(it, movementSeconds = if (it % 30 == 0) 2 else 0, lux = 40f) }

        val result = analyzer.analyze(epochs, SleepPlacement.BED, audioEnabled = false)

        assertTrue(result.score.durationPenalty > 0)
        assertEquals(6, result.score.environmentPenalty)
        assertTrue(result.score.total < 100)
    }
}
