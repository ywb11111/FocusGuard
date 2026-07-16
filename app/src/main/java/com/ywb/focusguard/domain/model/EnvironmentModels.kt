package com.ywb.focusguard.domain.model

/**
 * 某一时刻的噪声分析结果。
 *
 * @property timestamp 采样的 Unix 时间戳，单位毫秒。
 * @property decibel 相对噪声值，单位 dB，不代表经过校准的绝对声压级。
 * @property level 根据阈值归类后的噪声等级，供 UI 和环境分析器直接使用。
 */
data class NoiseSample(
    val timestamp: Long,
    val decibel: Float,
    val level: NoiseLevel
)

/** 从安静到高噪声的可解释等级。 */
enum class NoiseLevel {
    QUIET,
    NORMAL,
    NOISY,
    LOUD
}

/**
 * 某一时刻的光照传感器结果。
 *
 * @property timestamp 采样的 Unix 时间戳，单位毫秒。
 * @property lux 环境照度，单位 lux。
 * @property level 根据当前固定阈值得到的光照等级。
 */
data class LightSample(
    val timestamp: Long,
    val lux: Float,
    val level: LightLevel
)

/** 光照舒适度等级，后续可由用户设置替换固定阈值。 */
enum class LightLevel {
    DARK,
    DIM,
    COMFORTABLE,
    BRIGHT
}

/**
 * 加速度计数据转换后的移动样本。
 *
 * @property timestamp 样本的 Unix 时间戳，单位毫秒，适合持久化和展示。
 * @property magnitude 三轴加速度的合成模长，静止时通常接近 9.8。
 * @property isSignificantMove 是否在当前样本上刚确认了一次有效移动事件（瞬时）。
 * @property isMoving 当前是否处于移动状态（持续），冷却期内为 true，用于 UI 展示。
 */
data class MotionSample(
    val timestamp: Long,
    val magnitude: Float,
    val isSignificantMove: Boolean,
    val isMoving: Boolean = isSignificantMove  // 默认值兼容旧代码，但实际由 Detector 决定
)

/**
 * 同一时刻噪声、光照、移动状态的聚合快照，是环境 Repository 对 UI 的主要输出。
 *
 * @property noise 最新噪声样本。
 * @property light 最新光照样本。
 * @property motion 最新移动样本。
 * @property status Analyzer 根据三类数据得出的整体环境结论。
 */
data class EnvironmentSnapshot(
    val noise: NoiseSample,
    val light: LightSample,
    val motion: MotionSample,
    val status: EnvironmentStatus
)

/** 首页状态卡使用的整体环境状态。 */
enum class EnvironmentStatus {
    FOCUSED,
    NOISY,
    TOO_DARK,
    TOO_BRIGHT,
    MOVING
}
