package com.ywb.focusguard.domain.analyzer

import com.ywb.focusguard.domain.model.EnvironmentSnapshot
import com.ywb.focusguard.domain.model.EnvironmentStatus
import com.ywb.focusguard.domain.model.LightLevel
import com.ywb.focusguard.domain.model.MotionSample
import com.ywb.focusguard.domain.model.NoiseLevel

class EnvironmentAnalyzer {
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

    fun headline(snapshot: EnvironmentSnapshot): String = when (snapshot.status) {
        EnvironmentStatus.FOCUSED -> "当前适合专注"
        EnvironmentStatus.NOISY -> "环境偏吵"
        EnvironmentStatus.TOO_DARK -> "光线偏暗"
        EnvironmentStatus.TOO_BRIGHT -> "光线偏亮"
        EnvironmentStatus.MOVING -> "手机活动频繁"
    }
}
