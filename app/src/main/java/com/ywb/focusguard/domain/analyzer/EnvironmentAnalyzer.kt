package com.ywb.focusguard.domain.analyzer

import com.ywb.focusguard.domain.model.EnvironmentSnapshot
import com.ywb.focusguard.domain.model.EnvironmentStatus
import com.ywb.focusguard.domain.model.LightLevel
import com.ywb.focusguard.domain.model.MotionSample
import com.ywb.focusguard.domain.model.NoiseLevel

// 环境判断规则放在独立 Analyzer 中，避免 ViewModel 同时承担状态管理和业务计算。
class EnvironmentAnalyzer {
    /**
     * 按“移动 > 噪声 > 光照”的优先级输出一个整体状态，确保首页只展示最需要处理的问题。
     */
    fun statusFor(
        noiseLevel: NoiseLevel,
        lightLevel: LightLevel,
        motion: MotionSample
    ): EnvironmentStatus = when {
        motion.isSignificantMove -> EnvironmentStatus.MOVING
        noiseLevel == NoiseLevel.NOISY || noiseLevel == NoiseLevel.LOUD -> EnvironmentStatus.NOISY
        lightLevel == LightLevel.DARK || lightLevel == LightLevel.DIM -> EnvironmentStatus.TOO_DARK
        lightLevel == LightLevel.BRIGHT -> EnvironmentStatus.TOO_BRIGHT
        else -> EnvironmentStatus.FOCUSED
    }

    /** 将领域状态转换为首页可直接显示的中文标题。 */
    fun headline(snapshot: EnvironmentSnapshot): String = when (snapshot.status) {
        EnvironmentStatus.FOCUSED -> "当前适合专注"
        EnvironmentStatus.NOISY -> "环境偏吵"
        EnvironmentStatus.TOO_DARK -> "光线偏暗"
        EnvironmentStatus.TOO_BRIGHT -> "光线偏亮"
        EnvironmentStatus.MOVING -> "手机活动频繁"
    }
}
