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

        val result = detector.onSample(
            x = 12.5f, y = 0f, z = 0f,
            elapsedRealtimeMillis = 100L
        )

        assertFalse(result.isSignificantMove)
    }

    @Test
    fun `600ms 内连续命中三次只输出一次移动事件`() {
        val detector = MotionEventDetector()

        assertFalse(detector.onSample(x = 12.5f, y = 0f, z = 0f, elapsedRealtimeMillis = 100L).isSignificantMove)
        assertFalse(detector.onSample(x = 12.2f, y = 0f, z = 0f, elapsedRealtimeMillis = 280L).isSignificantMove)
        assertTrue(detector.onSample(x = 12.8f, y = 0f, z = 0f, elapsedRealtimeMillis = 520L).isSignificantMove)
    }

    @Test
    fun `命中超过时间窗口后重新累计`() {
        val detector = MotionEventDetector()

        assertFalse(detector.onSample(x = 12.5f, y = 0f, z = 0f, elapsedRealtimeMillis = 100L).isSignificantMove)
        assertFalse(detector.onSample(x = 12.2f, y = 0f, z = 0f, elapsedRealtimeMillis = 300L).isSignificantMove)
        assertFalse(detector.onSample(x = 12.8f, y = 0f, z = 0f, elapsedRealtimeMillis = 750L).isSignificantMove)
        assertFalse(detector.onSample(x = 12.6f, y = 0f, z = 0f, elapsedRealtimeMillis = 900L).isSignificantMove)
        assertTrue(detector.onSample(x = 12.7f, y = 0f, z = 0f, elapsedRealtimeMillis = 1_050L).isSignificantMove)
    }

    @Test
    fun `冷却期内连续命中不会重复输出事件`() {
        val detector = MotionEventDetector()

        detector.onSample(x = 12.5f, y = 0f, z = 0f, elapsedRealtimeMillis = 100L)
        detector.onSample(x = 12.2f, y = 0f, z = 0f, elapsedRealtimeMillis = 280L)
        assertTrue(detector.onSample(x = 12.8f, y = 0f, z = 0f, elapsedRealtimeMillis = 520L).isSignificantMove)

        assertFalse(detector.onSample(x = 12.5f, y = 0f, z = 0f, elapsedRealtimeMillis = 700L).isSignificantMove)
        assertFalse(detector.onSample(x = 12.5f, y = 0f, z = 0f, elapsedRealtimeMillis = 900L).isSignificantMove)
        assertFalse(detector.onSample(x = 12.5f, y = 0f, z = 0f, elapsedRealtimeMillis = 1_100L).isSignificantMove)
    }

    @Test
    fun `冷却结束后需要重新连续命中三次`() {
        val detector = MotionEventDetector()

        detector.onSample(x = 12.5f, y = 0f, z = 0f, elapsedRealtimeMillis = 100L)
        detector.onSample(x = 12.2f, y = 0f, z = 0f, elapsedRealtimeMillis = 280L)
        assertTrue(detector.onSample(x = 12.8f, y = 0f, z = 0f, elapsedRealtimeMillis = 520L).isSignificantMove)

        assertFalse(detector.onSample(x = 12.5f, y = 0f, z = 0f, elapsedRealtimeMillis = 1_800L).isSignificantMove)
        assertFalse(detector.onSample(x = 12.5f, y = 0f, z = 0f, elapsedRealtimeMillis = 2_000L).isSignificantMove)
        assertTrue(detector.onSample(x = 12.5f, y = 0f, z = 0f, elapsedRealtimeMillis = 2_200L).isSignificantMove)
    }

    @Test
    fun `低于阈值的样本会中断连续命中`() {
        val detector = MotionEventDetector()

        assertFalse(detector.onSample(x = 12.5f, y = 0f, z = 0f, elapsedRealtimeMillis = 100L).isSignificantMove)
        assertFalse(detector.onSample(x = 0.5f, y = 0f, z = 0f, elapsedRealtimeMillis = 200L).isSignificantMove)
        assertFalse(detector.onSample(x = 12.5f, y = 0f, z = 0f, elapsedRealtimeMillis = 300L).isSignificantMove)
        assertFalse(detector.onSample(x = 12.5f, y = 0f, z = 0f, elapsedRealtimeMillis = 400L).isSignificantMove)
        assertTrue(detector.onSample(x = 12.5f, y = 0f, z = 0f, elapsedRealtimeMillis = 500L).isSignificantMove)
    }
}
