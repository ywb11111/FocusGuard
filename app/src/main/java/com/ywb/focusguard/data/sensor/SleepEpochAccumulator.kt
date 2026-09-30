package com.ywb.focusguard.data.sensor

import com.ywb.focusguard.domain.model.SleepEpoch

/**
 * 把一分钟内的各类传感器数据累加成一条 [SleepEpoch]。纯 Kotlin，可直接单元测试。
 *
 * **调用方**：SleepMonitorService。体动、声音、光照在不同协程里回调，亮灭屏在主线程广播里回调，
 * 每分钟由计时协程调用 [drain] 取出结果并清零。
 *
 * **线程安全**：多个线程会同时调用这里的方法，所以所有公开方法都加 @Synchronized。
 * 每个方法只做几次加法，持锁时间极短，不会造成卡顿；比引入 Mutex（需要 suspend）更简单。
 *
 * **体动的自适应阈值**：
 * 不同手机加速度计的噪声差别很大，固定阈值在噪声大的手机上会把"静止"也算成体动。
 * 这里持续估计静止时 |Δa| 的噪声水平 [motionNoiseEstimate]，阈值取其 3 倍，并且不低于 [minMotionThreshold]。
 *
 * @property minMotionThreshold 体动阈值下限，单位 m/s²，需要真机校准。
 * @param epochStart 第一分钟的开始时间。
 */
class SleepEpochAccumulator(
    epochStart: Long,
    private val minMotionThreshold: Float = 0.05f
) {
    /** 当前这一分钟的开始时间。 */
    private var epochStart: Long = epochStart

    /** 这一分钟里出现体动的"秒"集合（按 Unix 秒去重）。 */
    private val movementSeconds = HashSet<Long>()

    /** 静止噪声的指数平滑估计；NaN 表示还没有样本。 */
    private var motionNoiseEstimate = Float.NaN

    private var decibelSum = 0.0
    private var decibelCount = 0
    private var decibelMax = 0f
    private var soundEventCount = 0
    private var snoreEventCount = 0

    private var luxSum = 0.0
    private var luxCount = 0

    /** 最近一次光照读数。光照传感器只在变化时回调，某分钟没回调就沿用上一次读数。 */
    private var lastLux = 0f

    /** 屏幕点亮的开始时间；null 表示当前熄屏。 */
    private var screenOnSince: Long? = null

    /** 这一分钟已累计的亮屏毫秒数。 */
    private var screenOnMillis = 0L

    /** 当前这一分钟的开始时间；Service 结束时据此判断最后不足一分钟的数据是否值得保存。 */
    @Synchronized
    fun currentEpochStart(): Long = epochStart

    /** 当前体动阈值：噪声估计的 3 倍，不低于下限。 */
    private val motionThreshold: Float
        get() = if (motionNoiseEstimate.isNaN()) minMotionThreshold
        else maxOf(minMotionThreshold, motionNoiseEstimate * 3f)

    /**
     * 接收一次加速度变化量。
     *
     * @param delta 相邻两帧的 |Δa|，单位 m/s²。
     * @param timestamp 现实时间毫秒戳，用于按秒去重。
     */
    @Synchronized
    fun onMotionDelta(delta: Float, timestamp: Long) {
        if (motionNoiseEstimate.isNaN()) {
            motionNoiseEstimate = delta
            return
        }
        if (delta >= motionThreshold) {
            movementSeconds.add(timestamp / 1_000L)
        } else {
            // 只用"静止"样本更新噪声估计，避免翻身把阈值越抬越高
            motionNoiseEstimate += (delta - motionNoiseEstimate) * 0.01f
        }
    }

    /** 接收一帧声音音量。 */
    @Synchronized
    fun onSoundFrame(frame: SoundFrame) {
        decibelSum += frame.decibel
        decibelCount++
        decibelMax = maxOf(decibelMax, frame.decibel)
    }

    /** 接收一次完整声音事件。 */
    @Synchronized
    fun onSoundEvent(event: SoundEvent) {
        soundEventCount++
        if (event.isSnoreLike) snoreEventCount++
    }

    /** 接收一次光照读数。 */
    @Synchronized
    fun onLight(lux: Float) {
        luxSum += lux
        luxCount++
        lastLux = lux
    }

    /**
     * 接收亮灭屏变化。
     *
     * @param isOn true 表示屏幕点亮。
     * @param timestamp 变化发生的现实时间。
     */
    @Synchronized
    fun onScreenChanged(isOn: Boolean, timestamp: Long) {
        if (isOn) {
            if (screenOnSince == null) screenOnSince = timestamp
        } else {
            screenOnSince?.let { since ->
                screenOnMillis += (timestamp - maxOf(since, epochStart)).coerceAtLeast(0L)
            }
            screenOnSince = null
        }
    }

    /**
     * 结束当前这一分钟：生成 [SleepEpoch]，清零计数，并把下一分钟的开始时间设为 [epochEnd]。
     *
     * 屏幕如果仍亮着，只把本分钟内的部分计入，剩余部分留给下一分钟。
     */
    @Synchronized
    fun drain(epochEnd: Long): SleepEpoch {
        val since = screenOnSince
        val totalScreenOnMillis = if (since != null) {
            screenOnMillis + (epochEnd - maxOf(since, epochStart)).coerceAtLeast(0L)
        } else {
            screenOnMillis
        }

        val epoch = SleepEpoch(
            startTime = epochStart,
            movementSeconds = movementSeconds.size.coerceAtMost(60),
            averageNoiseDb = if (decibelCount > 0) (decibelSum / decibelCount).toFloat() else 0f,
            maxNoiseDb = decibelMax,
            soundEventCount = soundEventCount,
            snoreEventCount = snoreEventCount,
            averageLightLux = if (luxCount > 0) (luxSum / luxCount).toFloat() else lastLux,
            screenOnSeconds = (totalScreenOnMillis / 1_000L).toInt().coerceIn(0, 60)
        )

        epochStart = epochEnd
        movementSeconds.clear()
        decibelSum = 0.0
        decibelCount = 0
        decibelMax = 0f
        soundEventCount = 0
        snoreEventCount = 0
        luxSum = 0.0
        luxCount = 0
        screenOnMillis = 0L
        return epoch
    }
}
