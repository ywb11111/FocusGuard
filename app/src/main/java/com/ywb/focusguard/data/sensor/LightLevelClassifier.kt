package com.ywb.focusguard.data.sensor

import com.ywb.focusguard.domain.model.LightLevel

// 光照等级规则先保持简单可解释，后续可以从 SettingsRepository 读取用户自定义阈值。
fun classifyLightLevel(lux: Float): LightLevel = when {
    lux < 10f -> LightLevel.DARK
    lux < 100f -> LightLevel.DIM
    lux < 500f -> LightLevel.COMFORTABLE
    else -> LightLevel.BRIGHT
}
