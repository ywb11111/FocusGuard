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
    fun `单轴超过阈值时判定为移动`() {
        assertTrue(isSignificantLinearAcceleration(x = 2.0f, y = 0.0f, z = 0.0f))
        assertTrue(isSignificantLinearAcceleration(x = 0.0f, y = 0.0f, z = -2.0f))
    }

    @Test
    fun `所有轴都未超过阈值时判定为稳定`() {
        assertFalse(isSignificantLinearAcceleration(x = 0.1f, y = 0.1f, z = 0.1f))
        assertFalse(isSignificantLinearAcceleration(x = 0.5f, y = 0.5f, z = 0.5f))
    }
}
