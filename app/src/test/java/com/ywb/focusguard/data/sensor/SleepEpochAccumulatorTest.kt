package com.ywb.focusguard.data.sensor

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 验证分钟聚合器：体动按秒去重、亮屏跨分钟切分、声音与光照统计、drain 后清零。
 */
class SleepEpochAccumulatorTest {

    @Test
    fun `同一秒内多次体动只算 1 秒`() {
        val accumulator = SleepEpochAccumulator(epochStart = 0L)
        accumulator.onMotionDelta(0.01f, 0L) // 第一帧只用于初始化噪声估计
        accumulator.onMotionDelta(0.5f, 1_100L)
        accumulator.onMotionDelta(0.6f, 1_300L)
        accumulator.onMotionDelta(0.5f, 5_000L)
        accumulator.onMotionDelta(0.5f, 9_900L)

        val epoch = accumulator.drain(60_000L)

        assertEquals(3, epoch.movementSeconds)
    }

    @Test
    fun `低于阈值的传感器噪声不算体动`() {
        val accumulator = SleepEpochAccumulator(epochStart = 0L)
        (0 until 300).forEach { index -> accumulator.onMotionDelta(0.02f, index * 200L) }

        val epoch = accumulator.drain(60_000L)

        assertEquals(0, epoch.movementSeconds)
    }

    @Test
    fun `亮屏时间按分钟边界切分`() {
        val accumulator = SleepEpochAccumulator(epochStart = 0L)
        accumulator.onScreenChanged(isOn = true, timestamp = 10_000L)
        accumulator.onScreenChanged(isOn = false, timestamp = 40_000L)
        accumulator.onScreenChanged(isOn = true, timestamp = 50_000L)

        val first = accumulator.drain(60_000L)
        val second = accumulator.drain(120_000L)
        accumulator.onScreenChanged(isOn = false, timestamp = 130_000L)
        val third = accumulator.drain(180_000L)

        assertEquals(40, first.screenOnSeconds)
        assertEquals(60, second.screenOnSeconds)
        assertEquals(10, third.screenOnSeconds)
    }

    @Test
    fun `声音和光照统计后 drain 清零并推进开始时间`() {
        val accumulator = SleepEpochAccumulator(epochStart = 0L)
        accumulator.onSoundFrame(SoundFrame(100L, 30f, 500f))
        accumulator.onSoundFrame(SoundFrame(200L, 50f, 500f))
        accumulator.onSoundEvent(SoundEvent(100L, 400L, 50f, 200f, isSnoreLike = true))
        accumulator.onSoundEvent(SoundEvent(500L, 800L, 50f, 1_500f, isSnoreLike = false))
        accumulator.onLight(2f)
        accumulator.onLight(4f)

        val first = accumulator.drain(60_000L)
        val second = accumulator.drain(120_000L)

        assertEquals(0L, first.startTime)
        assertEquals(40f, first.averageNoiseDb, 0.01f)
        assertEquals(50f, first.maxNoiseDb, 0.01f)
        assertEquals(2, first.soundEventCount)
        assertEquals(1, first.snoreEventCount)
        assertEquals(3f, first.averageLightLux, 0.01f)

        assertEquals(60_000L, second.startTime)
        assertEquals(0, second.soundEventCount)
        // 光照传感器只在变化时回调；这一分钟没有新读数，沿用上一次的 4 lux
        assertEquals(4f, second.averageLightLux, 0.01f)
    }
}
