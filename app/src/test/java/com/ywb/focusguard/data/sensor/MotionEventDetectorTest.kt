package com.ywb.focusguard.data.sensor

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 验证移动事件状态机的连续命中、时间窗口、冷却期和重置规则。
 * 测试使用显式单调时间，运行稳定且不依赖真实传感器。
 */
class MotionEventDetectorTest {
    @Test
    fun `单次超过阈值不会输出移动事件`() {
        val detector = MotionEventDetector()

        val detected = detector.onSample(
            magnitude = 12.5f,
            elapsedRealtimeMillis = 100L
        )

        assertFalse(detected)
    }

    @Test
    fun `600ms 内连续命中三次只输出一次移动事件`() {
        val detector = MotionEventDetector()

        assertFalse(detector.onSample(12.5f, 100L))
        assertFalse(detector.onSample(12.2f, 280L))
        assertTrue(detector.onSample(12.8f, 520L))
    }

    @Test
    fun `命中超过时间窗口后重新累计`() {
        val detector = MotionEventDetector()

        assertFalse(detector.onSample(12.5f, 100L))
        assertFalse(detector.onSample(12.2f, 300L))
        assertFalse(detector.onSample(12.8f, 750L))
        assertFalse(detector.onSample(12.6f, 900L))
        assertTrue(detector.onSample(12.7f, 1_050L))
    }

    @Test
    fun `冷却期内连续命中不会重复输出事件`() {
        val detector = MotionEventDetector()

        detector.onSample(12.5f, 100L)
        detector.onSample(12.2f, 280L)
        assertTrue(detector.onSample(12.8f, 520L))

        assertFalse(detector.onSample(12.5f, 700L))
        assertFalse(detector.onSample(12.5f, 900L))
        assertFalse(detector.onSample(12.5f, 1_100L))
    }

    @Test
    fun `冷却结束后需要重新连续命中三次`() {
        val detector = MotionEventDetector()

        detector.onSample(12.5f, 100L)
        detector.onSample(12.2f, 280L)
        assertTrue(detector.onSample(12.8f, 520L))

        assertFalse(detector.onSample(12.5f, 1_800L))
        assertFalse(detector.onSample(12.5f, 2_000L))
        assertTrue(detector.onSample(12.5f, 2_200L))
    }

    @Test
    fun `低于阈值的样本会中断连续命中`() {
        val detector = MotionEventDetector()

        assertFalse(detector.onSample(12.5f, 100L))
        assertFalse(detector.onSample(9.8f, 200L))
        assertFalse(detector.onSample(12.5f, 300L))
        assertFalse(detector.onSample(12.5f, 400L))
        assertTrue(detector.onSample(12.5f, 500L))
    }
}
