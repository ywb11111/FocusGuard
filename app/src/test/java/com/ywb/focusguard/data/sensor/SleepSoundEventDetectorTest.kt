package com.ywb.focusguard.data.sensor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 验证声音事件切分和鼾声节律识别。每帧 100ms，用显式时间戳构造，不依赖麦克风。
 */
class SleepSoundEventDetectorTest {

    /**
     * 构造一段帧序列：底噪 30dB，在 [loudStarts] 的每个时间点开始持续 [loudMillis] 的 50dB 声音。
     */
    private fun frames(
        totalMillis: Long,
        loudStarts: List<Long>,
        loudMillis: Long,
        hz: Float
    ): List<SoundFrame> = (0 until totalMillis step 100L).map { t ->
        val loud = loudStarts.any { start -> t >= start && t < start + loudMillis }
        SoundFrame(timestamp = t, decibel = if (loud) 50f else 30f, dominantHz = if (loud) hz else 3_000f)
    }

    private fun runAll(detector: SleepSoundEventDetector, input: List<SoundFrame>): List<SoundEvent> =
        input.mapNotNull(detector::onFrame)

    @Test
    fun `高于底噪的短促声音被识别为一次事件`() {
        val events = runAll(SleepSoundEventDetector(), frames(3_000, listOf(1_000), 300, hz = 1_500f))

        assertEquals(1, events.size)
        assertEquals(1_000L, events[0].startTime)
        assertEquals(1_300L, events[0].endTime)
        assertFalse(events[0].isSnoreLike)
    }

    @Test
    fun `只有一帧的咔哒声被丢弃`() {
        val detector = SleepSoundEventDetector()

        val events = runAll(detector, frames(2_000, listOf(1_000), 100, hz = 1_500f))

        assertTrue(events.isEmpty())
    }

    @Test
    fun `低沉且按呼吸节律重复的声音第三次起判为疑似鼾声`() {
        val events = runAll(
            SleepSoundEventDetector(),
            frames(12_000, listOf(1_000, 4_000, 7_000, 10_000), 500, hz = 200f)
        )

        assertEquals(4, events.size)
        assertFalse(events[0].isSnoreLike)
        assertFalse(events[1].isSnoreLike)
        assertTrue(events[2].isSnoreLike)
        assertTrue(events[3].isSnoreLike)
    }

    @Test
    fun `尖锐的节律声音不会被当成鼾声`() {
        val events = runAll(
            SleepSoundEventDetector(),
            frames(12_000, listOf(1_000, 4_000, 7_000, 10_000), 500, hz = 1_800f)
        )

        assertEquals(4, events.size)
        assertTrue(events.none { it.isSnoreLike })
    }

    @Test
    fun `持续噪声只报一次事件并抬高底噪`() {
        val detector = SleepSoundEventDetector()
        val quiet = (0 until 1_000L step 100L).map { SoundFrame(it, 30f, 3_000f) }
        val fan = (1_000L until 12_000L step 100L).map { SoundFrame(it, 50f, 3_000f) }

        val events = runAll(detector, quiet + fan)

        assertEquals(1, events.size)
        assertEquals(50f, detector.baselineDb, 0.01f)
    }

    @Test
    fun `事件未结束时不返回结果`() {
        val detector = SleepSoundEventDetector()
        detector.onFrame(SoundFrame(0, 30f, 3_000f))

        assertNull(detector.onFrame(SoundFrame(100, 50f, 200f)))
        assertNull(detector.onFrame(SoundFrame(200, 50f, 200f)))
        assertNotNull(detector.onFrame(SoundFrame(300, 30f, 3_000f)))
    }
}
