package com.ywb.focusguard.data.sensor

/**
 * 一帧（100ms）声音的分析结果，由 SleepSoundDataSource 产生。
 *
 * @property timestamp 帧读取完成时的 Unix 毫秒时间戳。
 * @property decibel 相对 dB（未校准）。
 * @property dominantHz 过零率估计的主频，用于区分低沉鼾声和尖锐声音。
 */
data class SoundFrame(
    val timestamp: Long,
    val decibel: Float,
    val dominantHz: Float
)

/**
 * 一次完整的声音事件（从变响到恢复安静）。
 *
 * @property startTime 事件开始时间。
 * @property endTime 事件结束时间。
 * @property peakDb 事件内最大相对 dB。
 * @property averageHz 事件内平均主频。
 * @property isSnoreLike 是否判定为疑似鼾声。
 */
data class SoundEvent(
    val startTime: Long,
    val endTime: Long,
    val peakDb: Float,
    val averageHz: Float,
    val isSnoreLike: Boolean
)

/**
 * 把连续的声音帧切分成离散的"声音事件"，并识别疑似鼾声。纯 Kotlin，可直接单元测试。
 *
 * **1. 自适应底噪（baseline）**
 * 每个卧室、每台手机的安静读数都不一样，用固定阈值（比如 50dB）会在有的手机上全是事件、有的手机上一个都没有。
 * 因此持续估计环境底噪：安静帧让底噪快速下降，稍响的帧让底噪缓慢上升；
 * 某帧比底噪高出 [triggerAboveBaselineDb] 才算事件开始。
 *
 * **2. 事件切分**
 * 事件开始后，响度回落到"触发线 - 滞回量"以下才结束。滞回（hysteresis）避免响度在阈值附近抖动时
 * 一个事件被切成很多碎片。短于 [minEventMillis] 的视为咔哒声丢弃；
 * 超过 [maxEventMillis] 的视为环境变化（例如风扇开启），抬高底噪，避免之后每 5 秒报一次事件。
 *
 * **3. 疑似鼾声**
 * 鼾声的特征：低沉（主频低）、每次持续零点几秒到几秒、跟着呼吸节律重复（间隔约 1.5~8 秒）。
 * 连续 [snoreStreakRequired] 次满足这些条件才标记为疑似鼾声，单独一次低沉声音（比如咳嗽）不算。
 * 这是启发式规则，所有阈值都需要真机校准，页面文案只能写"疑似鼾声"。
 *
 * @property triggerAboveBaselineDb 高出底噪多少 dB 算事件开始。
 * @property releaseHysteresisDb 事件结束的滞回量。
 * @property minEventMillis 最短有效事件时长。
 * @property maxEventMillis 最长事件时长，超过视为环境变化。
 * @property snoreMaxHz 鼾声主频上限。
 * @property snoreMinDurationMillis 单次鼾声最短时长。
 * @property snoreMaxDurationMillis 单次鼾声最长时长。
 * @property snoreMinIntervalMillis 相邻两次鼾声的最短间隔。
 * @property snoreMaxIntervalMillis 相邻两次鼾声的最长间隔。
 * @property snoreStreakRequired 连续多少次符合节律才判定为鼾声。
 */
class SleepSoundEventDetector(
    private val triggerAboveBaselineDb: Float = 10f,
    private val releaseHysteresisDb: Float = 3f,
    private val minEventMillis: Long = 200L,
    private val maxEventMillis: Long = 5_000L,
    private val snoreMaxHz: Float = 700f,
    private val snoreMinDurationMillis: Long = 300L,
    private val snoreMaxDurationMillis: Long = 3_500L,
    private val snoreMinIntervalMillis: Long = 1_500L,
    private val snoreMaxIntervalMillis: Long = 8_000L,
    private val snoreStreakRequired: Int = 3
) {
    /** 当前估计的环境底噪；NaN 表示还没收到第一帧。 */
    var baselineDb: Float = Float.NaN
        private set

    /** 进行中事件的开始时间；null 表示当前安静。 */
    private var eventStart: Long? = null

    /** 进行中事件的峰值 dB。 */
    private var eventPeakDb = 0f

    /** 进行中事件的主频累加值，结束时求平均。 */
    private var eventHzSum = 0f

    /** 进行中事件包含的帧数。 */
    private var eventFrameCount = 0

    /** 上一次"低沉且时长合适"事件的开始时间，用来判断呼吸节律。 */
    private var lastLowPitchOnset: Long? = null

    /** 当前连续符合节律的低沉事件数。 */
    private var snoreStreak = 0

    /**
     * 消费一帧声音。
     *
     * @return 这一帧让某个事件结束时返回该事件；否则返回 null。
     */
    fun onFrame(frame: SoundFrame): SoundEvent? {
        val baseline = baselineDb
        if (baseline.isNaN()) {
            // 第一帧直接作为初始底噪
            baselineDb = frame.decibel
            return null
        }

        val start = eventStart
        if (start == null) {
            if (frame.decibel >= baseline + triggerAboveBaselineDb) {
                eventStart = frame.timestamp
                eventPeakDb = frame.decibel
                eventHzSum = frame.dominantHz
                eventFrameCount = 1
            } else {
                updateBaseline(frame.decibel)
            }
            return null
        }

        val stillLoud = frame.decibel >= baseline + triggerAboveBaselineDb - releaseHysteresisDb
        val duration = frame.timestamp - start
        if (stillLoud && duration < maxEventMillis) {
            eventPeakDb = maxOf(eventPeakDb, frame.decibel)
            eventHzSum += frame.dominantHz
            eventFrameCount++
            return null
        }

        if (stillLoud) {
            // 持续过长：更像是环境本身变吵了，把底噪直接抬到当前水平
            baselineDb = frame.decibel
        } else {
            updateBaseline(frame.decibel)
        }
        return closeEvent(start, frame.timestamp)
    }

    /**
     * 结束当前事件并分类。
     *
     * @param start 事件开始时间。
     * @param end 事件结束时间（第一帧安静帧的时间）。
     * @return 有效事件；太短的事件返回 null。
     */
    private fun closeEvent(start: Long, end: Long): SoundEvent? {
        val averageHz = if (eventFrameCount > 0) eventHzSum / eventFrameCount else 0f
        val peak = eventPeakDb
        eventStart = null
        eventFrameCount = 0
        eventHzSum = 0f

        val duration = end - start
        if (duration < minEventMillis) return null

        val lowPitch = averageHz <= snoreMaxHz &&
            duration in snoreMinDurationMillis..snoreMaxDurationMillis
        if (lowPitch) {
            val previous = lastLowPitchOnset
            snoreStreak = if (previous != null && start - previous in snoreMinIntervalMillis..snoreMaxIntervalMillis) {
                snoreStreak + 1
            } else {
                1
            }
            lastLowPitchOnset = start
        }
        // 非低沉事件（比如翻身摩擦）不打断鼾声节律：间隔是否合理由上面的时间窗判断

        return SoundEvent(
            startTime = start,
            endTime = end,
            peakDb = peak,
            averageHz = averageHz,
            isSnoreLike = lowPitch && snoreStreak >= snoreStreakRequired
        )
    }

    /**
     * 非对称指数平滑更新底噪：往下快（0.3），往上慢（0.01）。
     * 往上慢是为了让偶发声音几乎不影响底噪；往下快是为了声音停止后尽快恢复灵敏度。
     */
    private fun updateBaseline(decibel: Float) {
        val alpha = if (decibel < baselineDb) 0.3f else 0.01f
        baselineDb += (decibel - baselineDb) * alpha
    }
}
