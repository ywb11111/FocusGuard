package com.ywb.focusguard.data.sensor

/**
 * 把高频候选移动帧聚合成低频、可计数的有效移动事件，并提供"正在移动"的持续状态。
 *
 * 使用 TYPE_LINEAR_ACCELERATION 数据，检测各轴加速度变化。
 * 任一轴超过阈值即视为候选帧，然后通过 窗口-冷却 机制确认事件。
 *
 * @property requiredHits 一个窗口内必须连续命中的次数。
 * @property windowMillis 从第一次命中开始允许累计的时间窗口，单位毫秒。
 * @property cooldownMillis 确认事件后的冷却时间，单位毫秒，冷却期内 isMoving 为 true。
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
     * 消费一帧线性加速度数据，返回检测结果。
     *
     * 判断逻辑：
     * 1. 冷却期内直接返回 isMoving = true，不检测新事件。
     * 2. 任一轴超过阈值才视为候选帧，否则重置窗口。
     * 3. 窗口内连续命中 requiredHits 次即确认一次有效移动事件。
     *
     * @param x X 轴线性加速度（已去除重力）。
     * @param y Y 轴线性加速度（已去除重力）。
     * @param z Z 轴线性加速度（已去除重力）。
     * @param elapsedRealtimeMillis 必须是单调时间（SystemClock.elapsedRealtime()），
     *                               避免用户修改系统时间破坏窗口判断。
     * @return MotionDetectResult 包含 isSignificantMove（瞬时事件）和 isMoving（持续状态）。
     */
    fun onSample(
        x: Float,
        y: Float,
        z: Float,
        elapsedRealtimeMillis: Long
    ): MotionDetectResult {
        val lastEvent = lastEventAt

        // 冷却期内：isMoving 为 true，不检测新事件
        if (lastEvent != null && elapsedRealtimeMillis - lastEvent < cooldownMillis) {
            return MotionDetectResult(
                isSignificantMove = false,
                isMoving = true
            )
        }

        // 判断是否是候选帧：任一轴超过阈值即为移动
        if (!isSignificantLinearAcceleration(x, y, z)) {
            resetHits()
            return MotionDetectResult(
                isSignificantMove = false,
                isMoving = false
            )
        }

        val firstHit = firstHitAt
        if (firstHit == null || elapsedRealtimeMillis - firstHit > windowMillis) {
            // 开始新的累计窗口
            firstHitAt = elapsedRealtimeMillis
            hitCount = 1
            return MotionDetectResult(
                isSignificantMove = false,
                isMoving = false
            )
        }

        // 窗口内连续命中
        hitCount++
        if (hitCount < requiredHits) {
            return MotionDetectResult(
                isSignificantMove = false,
                isMoving = false
            )
        }

        // 达到 requiredHits，确认一次有效移动事件
        lastEventAt = elapsedRealtimeMillis
        resetHits()
        return MotionDetectResult(
            isSignificantMove = true,
            isMoving = true
        )
    }

    /** 清空当前连续命中状态，但保留上一次事件时间以维持冷却期。 */
    private fun resetHits() {
        hitCount = 0
        firstHitAt = null
    }
}

/**
 * 单帧检测结果，区分瞬时事件和持续状态。
 *
 * @property isSignificantMove 是否刚确认一次有效移动事件（瞬时，只有一帧为 true）。
 * @property isMoving 当前是否处于移动状态（持续，冷却期内一直为 true）。
 */
data class MotionDetectResult(
    val isSignificantMove: Boolean,
    val isMoving: Boolean
)