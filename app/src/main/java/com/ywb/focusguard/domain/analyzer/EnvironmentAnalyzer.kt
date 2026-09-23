package com.ywb.focusguard.domain.analyzer

import com.ywb.focusguard.domain.model.EnvironmentSnapshot
import com.ywb.focusguard.domain.model.EnvironmentStatus
import com.ywb.focusguard.domain.model.LightLevel
import com.ywb.focusguard.domain.model.MotionSample
import com.ywb.focusguard.domain.model.NoiseLevel

/**
 * 环境判断规则放在独立 Analyzer 中，避免 ViewModel 同时承担状态管理和业务计算。
 *
 * 职责：根据噪声、光照、移动数据判断整体环境状态，供首页和专注页使用。
 */
class EnvironmentAnalyzer {
    /**
     * 按"移动 > 噪声 > 光照"的优先级输出一个整体状态，确保首页只展示最需要处理的问题。
     *
     * 注意：使用 motion.isMoving（持续状态）而不是 isSignificantMove（瞬时事件）。
     */
    fun statusFor(
        noiseLevel: NoiseLevel,
        lightLevel: LightLevel,
        motion: MotionSample
    ): EnvironmentStatus = when {
        motion.isMoving -> EnvironmentStatus.MOVING
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

    /**
     * 生成 0..100 的环境准备度分数。
     *
     * 该分数只描述“此刻环境是否适合开始”，不等同于一次专注结束后的表现评分。
     * 扣分规则显式写出，后续可以在设置接入用户自定义阈值而不修改 UI。
     */
    fun readinessScore(snapshot: EnvironmentSnapshot): Int {
        val noisePenalty = when (snapshot.noise.level) {
            NoiseLevel.QUIET -> 0
            NoiseLevel.NORMAL -> 6
            NoiseLevel.NOISY -> 18
            NoiseLevel.LOUD -> 30
        }
        val lightPenalty = when (snapshot.light.level) {
            LightLevel.COMFORTABLE -> 0
            LightLevel.DIM -> 8
            LightLevel.DARK -> 22
            LightLevel.BRIGHT -> 12
        }
        val motionPenalty = if (snapshot.motion.isMoving) 18 else 0
        return (100 - noisePenalty - lightPenalty - motionPenalty).coerceIn(0, 100)
    }

    /** 将准备度转换成用户可以直接行动的短说明。 */
    fun guidance(snapshot: EnvironmentSnapshot): String = when (snapshot.status) {
        EnvironmentStatus.FOCUSED -> "当前环境较为理想，可以开始专注。"
        EnvironmentStatus.NOISY -> "声音偏高，换个更安静的位置会更好。"
        EnvironmentStatus.TOO_DARK -> "光线偏暗，建议先调整台灯或屏幕亮度。"
        EnvironmentStatus.TOO_BRIGHT -> "光线偏亮，减少直射光后再开始更舒适。"
        EnvironmentStatus.MOVING -> "先把手机放稳，能减少专注过程中的干扰。"
    }
}
