package com.ywb.focusguard.data.sensor

/**
 * 把高频候选移动帧聚合成低频、可计数的有效移动事件。
 *
 * @property requiredHits 一个窗口内必须连续命中的次数。
 * @property windowMillis 从第一次命中开始允许累计的时间窗口，单位毫秒。
 * @property cooldownMillis 确认事件后的冷却时间，单位毫秒，避免一次晃动重复计数。
 */
class MotionEventDetector(
    private val requiredHits: Int = 3,
    private val windowMillis: Long = 600L,
    private val cooldownMillis: Long = 1_200L
) {
    /** 当前窗口已经连续命中的候选帧数量。 */
    private var hitCount = 0

    /** 当前累计窗口第一次命中的单调时间；null 表示尚未开始累计。 */
    private var firstHitAt: Long? = null

    /** 上一次确认有效移动事件的单调时间；用于判断冷却期。 */
    private var lastEventAt: Long? = null

    /**
     * 消费一帧加速度模长，只有在当前帧刚确认新事件时返回 true。
     * [elapsedRealtimeMillis] 必须是单调时间，避免用户修改系统时间破坏窗口判断。
     */
    fun onSample(
        magnitude: Float,
        elapsedRealtimeMillis: Long
    ): Boolean {
        val lastEvent = lastEventAt
        if (lastEvent != null && elapsedRealtimeMillis - lastEvent < cooldownMillis) {
            resetHits()
            return false
        }

        if (!isSignificantMove(magnitude)) {
            resetHits()
            return false
        }

        val firstHit = firstHitAt
        if (firstHit == null || elapsedRealtimeMillis - firstHit > windowMillis) {
            firstHitAt = elapsedRealtimeMillis
            hitCount = 1
            return false
        }

        hitCount++
        if (hitCount < requiredHits) {
            return false
        }

        lastEventAt = elapsedRealtimeMillis
        resetHits()
        return true
    }

    /** 清空当前连续命中状态，但保留上一次事件时间以维持冷却期。 */
    private fun resetHits() {
        hitCount = 0
        firstHitAt = null
    }
}
