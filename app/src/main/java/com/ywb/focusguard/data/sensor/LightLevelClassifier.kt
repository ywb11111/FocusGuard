package com.ywb.focusguard.data.sensor

import com.ywb.focusguard.domain.model.LightLevel

/**
 * 将 lux 转为可解释光照等级。
 * 规则先保持固定且易测试，后续可以从 SettingsRepository 读取用户自定义阈值。
 */
fun classifyLightLevel(lux: Float): LightLevel = when {
    lux < 10f -> LightLevel.DARK
    lux < 100f -> LightLevel.DIM
    lux < 500f -> LightLevel.COMFORTABLE
    else -> LightLevel.BRIGHT
}
