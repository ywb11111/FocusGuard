package com.ywb.focusguard.data.sensor

import com.ywb.focusguard.domain.model.LightLevel
import org.junit.Assert.assertEquals
import org.junit.Test

class LightLevelClassifierTest {
    @Test
    fun `0 到 10 lux 判定为 DARK`() {
        assertEquals(LightLevel.DARK, classifyLightLevel(0f))
        assertEquals(LightLevel.DARK, classifyLightLevel(9.9f))
    }

    @Test
    fun `10 到 100 lux 判定为 DIM`() {
        assertEquals(LightLevel.DIM, classifyLightLevel(10f))
        assertEquals(LightLevel.DIM, classifyLightLevel(99.9f))
    }

    @Test
    fun `100 到 500 lux 判定为 COMFORTABLE`() {
        assertEquals(LightLevel.COMFORTABLE, classifyLightLevel(100f))
        assertEquals(LightLevel.COMFORTABLE, classifyLightLevel(499.9f))
    }

    @Test
    fun `500 lux 及以上判定为 BRIGHT`() {
        assertEquals(LightLevel.BRIGHT, classifyLightLevel(500f))
        assertEquals(LightLevel.BRIGHT, classifyLightLevel(1_000f))
    }
}
