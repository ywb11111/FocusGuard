package com.ywb.focusguard.data.sensor

import kotlin.math.abs

/**
 * 旧版移动检测器：基于模长偏离重力基准判断。
 *
 * 用于不支持 TYPE_LINEAR_ACCELERATION 的设备降级。
 *
 * **局限性**：只能检测模长变化（上下晃动敏感），对水平平移不敏感。
 * 原因：水平移动时加速度模长变化很小，偏离重力基准不明显。
 *
 * @property requiredHits 窗口内需要连续命中的次数。
 * @property windowMillis 累计窗口时长（毫秒）。
 * @property cooldownMillis 冷却期时长（毫秒）。
 */
class LegacyMotionDetector(
    private val requiredHits: Int = 3,
    private val windowMillis: Long = 600L,
    private val cooldownMillis: Long = 1_200L
) {
    /** 重力加速度基准值（m/s²）。 */
    private val gravityEarth = 9.8f

    /** 模长偏离重力基准的阈值。 */
    private val threshold = 2.0f

    /** 当前窗口内连续命中次数。 */
    private var hitCount = 0

    /** 窗口首次命中的时间。 */
    private var firstHitAt: Long? = null

    /** 上一次确认事件的时间。 */
    private var lastEventAt: Long? = null

    /**
     * 消费一帧加速度模长，返回检测结果。
     *
     * @param magnitude 三轴加速度合成模长。
     * @param elapsedRealtimeMillis 单调时间戳（毫秒）。
     * @return 检测结果。
     */
    fun onSample(magnitude: Float, elapsedRealtimeMillis: Long): MotionDetectResult {
        val lastEvent = lastEventAt

        // 冷却期内：isMoving = true，不检测新事件
        if (lastEvent != null && elapsedRealtimeMillis - lastEvent < cooldownMillis) {
            return MotionDetectResult(isSignificantMove = false, isMoving = true)
        }

        // 判断模长是否偏离重力基准
        if (abs(magnitude - gravityEarth) <= threshold) {
            resetHits()
            return MotionDetectResult(isSignificantMove = false, isMoving = false)
        }

        // 窗口判断：600ms 内连续命中 3 次才确认事件
        val firstHit = firstHitAt
        if (firstHit == null || elapsedRealtimeMillis - firstHit > windowMillis) {
            firstHitAt = elapsedRealtimeMillis
            hitCount = 1
            return MotionDetectResult(isSignificantMove = false, isMoving = false)
        }

        hitCount++
        if (hitCount < requiredHits) {
            return MotionDetectResult(isSignificantMove = false, isMoving = false)
        }

        // 确认事件
        lastEventAt = elapsedRealtimeMillis
        resetHits()
        return MotionDetectResult(isSignificantMove = true, isMoving = true)
    }

    /** 重置窗口计数。 */
    private fun resetHits() {
        hitCount = 0
        firstHitAt = null
    }
}