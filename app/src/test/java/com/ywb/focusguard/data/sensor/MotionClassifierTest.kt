package com.ywb.focusguard.data.sensor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** 验证三轴模长公式和单帧候选移动阈值。 */
class MotionClassifierTest {
    @Test
    fun `加速度模长按三轴平方和开方计算`() {
        val magnitude = calculateMagnitude(x = 3f, y = 4f, z = 12f)

        assertEquals(13f, magnitude, 0.001f)
    }

    @Test
    fun `接近重力加速度时判定为稳定`() {
        assertFalse(isSignificantMove(9.8f))
        assertFalse(isSignificantMove(10.6f))
    }

    @Test
    fun `明显偏离重力加速度时判定为移动`() {
        assertTrue(isSignificantMove(12.5f))
        assertTrue(isSignificantMove(6.5f))
    }
}
