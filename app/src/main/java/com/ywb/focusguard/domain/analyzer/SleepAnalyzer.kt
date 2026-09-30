package com.ywb.focusguard.domain.analyzer

import com.ywb.focusguard.domain.model.SleepAnalysis
import com.ywb.focusguard.domain.model.SleepEpoch
import com.ywb.focusguard.domain.model.SleepPlacement
import com.ywb.focusguard.domain.model.SleepScore
import com.ywb.focusguard.domain.model.SleepStage
import kotlin.math.roundToInt

/**
 * 睡眠分析器：把一晚的分钟数据（epoch）判定为 清醒 / 浅睡·体动 / 安稳，并计算各项指标和评分。
 *
 * 放在 domain 层、不依赖 Android，和 FocusScoreAnalyzer 一样可以直接单元测试，也方便调参。
 *
 * **算法思路（体动记录仪 actigraphy 的简化版）**
 * 1. 每分钟一个活动量 a：床垫模式 = 体动秒数；床头柜模式 = 非鼾声的声音事件数 × 4（翻身摩擦声的近似）。
 * 2. 参考 Cole-Kripke 算法，用"前 4 分钟 + 当前 + 后 2 分钟"的加权和判断清醒：
 *    单独一次翻身只影响一分钟，不会被判成醒；连续几分钟辗转反侧，加权和超过阈值才判成醒。
 * 3. 亮屏 ≥ 10 秒的分钟直接判为清醒：人在看手机，一定是醒着的。
 * 4. 入睡 = 第一次出现连续 10 分钟睡眠；入睡前的分钟统一视为清醒（躺着还没睡着）。
 * 5. 入睡后持续 ≥ 3 分钟的清醒段计为一次夜醒，1~2 分钟的短暂清醒通常是翻身，不计次数。
 *
 * **已知局限（面试时要主动讲）**
 * 静静躺着但没睡着时没有体动，会被高估为睡眠——所有只靠体动的方案都有这个问题，
 * 手环会用心率辅助区分。因此页面上一律写"估算"。
 */
class SleepAnalyzer {

    companion object {
        /** 一个 epoch 的时长。 */
        const val EPOCH_MILLIS = 60_000L

        /** 连续多少分钟睡眠才算真正入睡。 */
        const val SLEEP_ONSET_RUN = 10

        /** 入睡后清醒持续多少分钟才算一次夜醒。 */
        const val MIN_WAKE_EPISODE = 3

        /** 一分钟内亮屏超过多少秒直接判为清醒。 */
        const val SCREEN_AWAKE_SECONDS = 10

        /** 加权活动量达到多少判为清醒，单位"体动秒"。 */
        const val WAKE_ACTIVITY_THRESHOLD = 8f

        /** 床头柜模式下一次非鼾声声音事件折算成多少体动秒。 */
        const val SOUND_EVENT_ACTIVITY = 4

        /**
         * 滑动窗口权重，下标 0..6 对应相对偏移 -4..+2 分钟。
         * 取自 Cole-Kripke 原始系数（404, 598, 326, 441, 1408, 508, 350）按当前分钟归一化，
         * 当前分钟权重最大，前后分钟起平滑作用。
         */
        val WINDOW_WEIGHTS = floatArrayOf(0.29f, 0.42f, 0.23f, 0.31f, 1.0f, 0.36f, 0.25f)

        /** 窗口起点相对当前分钟的偏移。 */
        private const val WINDOW_START_OFFSET = -4
    }

    /**
     * 分析一晚数据。
     *
     * @param epochs 按时间升序的分钟数据。
     * @param placement 手机摆放位置，决定活动量来源。
     * @param audioEnabled 是否有声音数据，决定噪声和鼾声指标是否参与评分。
     * @return 完整分析结果；没有数据时返回全 0 的结果和提示建议。
     */
    fun analyze(
        epochs: List<SleepEpoch>,
        placement: SleepPlacement,
        audioEnabled: Boolean
    ): SleepAnalysis {
        if (epochs.isEmpty()) return emptyAnalysis()

        val activity = epochs.map { activityOf(it, placement) }
        val stages = MutableList(epochs.size) { index ->
            classifyEpoch(index, epochs[index], activity)
        }

        val onsetIndex = findSleepOnset(stages)
        val finalWakeIndex = if (onsetIndex >= 0) stages.indexOfLast { it != SleepStage.AWAKE } else -1
        if (onsetIndex < 0) {
            // 整晚没有连续 10 分钟睡眠：不承认任何零散的"睡眠"分钟
            stages.fill(SleepStage.AWAKE)
        } else {
            for (i in 0 until onsetIndex) stages[i] = SleepStage.AWAKE
        }

        val monitoredMillis = epochs.size * EPOCH_MILLIS
        val sleepEpochs = stages.count { it != SleepStage.AWAKE }
        val totalSleepMillis = sleepEpochs * EPOCH_MILLIS
        val latencyMillis = if (onsetIndex >= 0) onsetIndex * EPOCH_MILLIS else monitoredMillis
        val sleepWindow = if (onsetIndex >= 0) stages.subList(onsetIndex, finalWakeIndex + 1) else emptyList()
        val wasoMillis = sleepWindow.count { it == SleepStage.AWAKE } * EPOCH_MILLIS
        val wakeCount = countWakeEpisodes(sleepWindow)
        val efficiency = totalSleepMillis.toFloat() / monitoredMillis
        val calmMillis = stages.count { it == SleepStage.CALM } * EPOCH_MILLIS
        val restlessMillis = stages.count { it == SleepStage.RESTLESS } * EPOCH_MILLIS
        val snoreCount = if (audioEnabled) epochs.sumOf { it.snoreEventCount } else 0
        val averageNoise = if (audioEnabled) epochs.map { it.averageNoiseDb }.average().toFloat() else 0f
        val averageLux = epochs.map { it.averageLightLux }.average().toFloat()
        // 床垫模式记录 2 小时以上，却只有不到 3 分钟出现过体动：手机很可能不在床上
        val lowMotionWarning = placement == SleepPlacement.BED &&
            epochs.size >= 120 &&
            epochs.count { it.movementSeconds > 0 } < 3

        val score = calculateScore(
            onsetFound = onsetIndex >= 0,
            totalSleepMillis = totalSleepMillis,
            efficiency = efficiency,
            latencyMillis = latencyMillis,
            wakeCount = wakeCount,
            snoreCount = snoreCount,
            audioEnabled = audioEnabled,
            averageNoiseDb = averageNoise,
            averageLightLux = averageLux,
            placement = placement,
            lowMotionWarning = lowMotionWarning
        )

        return SleepAnalysis(
            stages = stages,
            sleepOnsetIndex = onsetIndex,
            finalWakeIndex = finalWakeIndex,
            monitoredMillis = monitoredMillis,
            totalSleepMillis = totalSleepMillis,
            sleepLatencyMillis = latencyMillis,
            wakeAfterSleepOnsetMillis = wasoMillis,
            wakeCount = wakeCount,
            sleepEfficiency = efficiency,
            calmMillis = calmMillis,
            restlessMillis = restlessMillis,
            snoreEventCount = snoreCount,
            averageNoiseDb = averageNoise,
            averageLightLux = averageLux,
            lowMotionWarning = lowMotionWarning,
            score = score
        )
    }

    /** 计算一分钟的活动量，单位"体动秒"。 */
    private fun activityOf(epoch: SleepEpoch, placement: SleepPlacement): Float = when (placement) {
        SleepPlacement.BED -> epoch.movementSeconds.toFloat()
        SleepPlacement.NIGHTSTAND ->
            ((epoch.soundEventCount - epoch.snoreEventCount).coerceAtLeast(0) * SOUND_EVENT_ACTIVITY).toFloat()
    }

    /**
     * 判定单个分钟的状态。
     *
     * @param index 当前分钟下标。
     * @param epoch 当前分钟数据。
     * @param activity 整晚活动量序列，用于滑动窗口加权。
     */
    private fun classifyEpoch(index: Int, epoch: SleepEpoch, activity: List<Float>): SleepStage {
        if (epoch.screenOnSeconds >= SCREEN_AWAKE_SECONDS) return SleepStage.AWAKE

        var weighted = 0f
        WINDOW_WEIGHTS.forEachIndexed { weightIndex, weight ->
            val neighbor = index + WINDOW_START_OFFSET + weightIndex
            // 窗口越界（开头 4 分钟、结尾 2 分钟）时缺失的邻居按 0 活动处理
            if (neighbor in activity.indices) weighted += weight * activity[neighbor]
        }
        return when {
            weighted >= WAKE_ACTIVITY_THRESHOLD -> SleepStage.AWAKE
            activity[index] > 0f -> SleepStage.RESTLESS
            else -> SleepStage.CALM
        }
    }

    /** 找到第一段连续 [SLEEP_ONSET_RUN] 分钟睡眠的起点；找不到返回 -1。 */
    private fun findSleepOnset(stages: List<SleepStage>): Int {
        var runStart = -1
        var runLength = 0
        stages.forEachIndexed { index, stage ->
            if (stage == SleepStage.AWAKE) {
                runLength = 0
                runStart = -1
            } else {
                if (runLength == 0) runStart = index
                runLength++
                if (runLength >= SLEEP_ONSET_RUN) return runStart
            }
        }
        return -1
    }

    /** 统计睡眠窗口内持续 ≥ [MIN_WAKE_EPISODE] 分钟的清醒段数量。 */
    private fun countWakeEpisodes(window: List<SleepStage>): Int {
        var episodes = 0
        var awakeRun = 0
        window.forEach { stage ->
            if (stage == SleepStage.AWAKE) {
                awakeRun++
                if (awakeRun == MIN_WAKE_EPISODE) episodes++
            } else {
                awakeRun = 0
            }
        }
        return episodes
    }

    /**
     * 满分 100 减各项扣分，每项设上限，避免单一指标吞掉全部分数。
     * 阈值参考常见睡眠卫生建议（成人 7~9 小时、效率 ≥ 85%、入睡 ≤ 20 分钟）。
     */
    private fun calculateScore(
        onsetFound: Boolean,
        totalSleepMillis: Long,
        efficiency: Float,
        latencyMillis: Long,
        wakeCount: Int,
        snoreCount: Int,
        audioEnabled: Boolean,
        averageNoiseDb: Float,
        averageLightLux: Float,
        placement: SleepPlacement,
        lowMotionWarning: Boolean
    ): SleepScore {
        if (!onsetFound) {
            return SleepScore(
                total = 0,
                durationPenalty = 0,
                efficiencyPenalty = 0,
                latencyPenalty = 0,
                wakePenalty = 0,
                snorePenalty = 0,
                environmentPenalty = 0,
                suggestions = listOf("本次没有识别到持续 10 分钟以上的睡眠，可能记录时间太短或手机未放好。")
            )
        }

        val sleepHours = totalSleepMillis / 3_600_000f
        val latencyMinutes = latencyMillis / 60_000L

        val durationPenalty = when {
            sleepHours < 7f -> ((7f - sleepHours) * 8f).roundToInt().coerceAtMost(30)
            sleepHours > 10f -> 5
            else -> 0
        }
        val efficiencyPenalty = if (efficiency < 0.85f) {
            ((0.85f - efficiency) * 80f).roundToInt().coerceAtMost(20)
        } else {
            0
        }
        val latencyPenalty = when {
            latencyMinutes > 45 -> 12
            latencyMinutes > 30 -> 8
            latencyMinutes > 20 -> 3
            else -> 0
        }
        val wakePenalty = (wakeCount * 4).coerceAtMost(16)
        // 鼾声按"每小时次数"扣分，否则睡得越久扣得越多
        val snorePerHour = if (sleepHours > 0f) snoreCount / sleepHours else 0f
        val snorePenalty = if (!audioEnabled) 0 else when {
            snorePerHour > 60f -> 10
            snorePerHour > 20f -> 6
            snorePerHour > 5f -> 3
            else -> 0
        }
        val noisePenalty = if (!audioEnabled) 0 else when {
            averageNoiseDb > 50f -> 8
            averageNoiseDb > 42f -> 4
            else -> 0
        }
        val lightPenalty = when {
            averageLightLux > 30f -> 6
            averageLightLux > 5f -> 3
            else -> 0
        }
        val environmentPenalty = (noisePenalty + lightPenalty).coerceAtMost(12)

        val total = (100 - durationPenalty - efficiencyPenalty - latencyPenalty -
            wakePenalty - snorePenalty - environmentPenalty).coerceIn(0, 100)

        val suggestions = buildList {
            if (lowMotionWarning) add("整晚几乎没有检测到体动，手机可能没放在床垫上，体动相关结果仅供参考。")
            if (durationPenalty > 0 && sleepHours < 7f) add("睡眠不足 7 小时，可以尝试把上床时间提前 30 分钟。")
            if (latencyPenalty > 0) add("入睡用了 $latencyMinutes 分钟，睡前 1 小时尽量少看屏幕、避免咖啡因。")
            if (efficiencyPenalty > 0) add("在床上清醒的时间偏多，困了再上床，避免躺在床上刷手机。")
            if (wakePenalty > 0) add("夜里醒来 $wakeCount 次，可以留意噪声、室温或睡前饮水。")
            if (snorePenalty > 0) add("检测到较多疑似鼾声，可尝试侧睡；长期严重打鼾建议咨询医生。")
            if (noisePenalty > 0) add("卧室偏吵，可以试试耳塞或白噪声。")
            if (lightPenalty > 0) add("卧室光线偏亮，建议使用遮光窗帘、关闭夜灯。")
            if (placement == SleepPlacement.NIGHTSTAND) add("床头柜模式只分析声音，想要更准确可以把手机放在枕边床垫上。")
            if (isEmpty()) add("这一晚睡得不错，继续保持规律作息。")
        }

        return SleepScore(
            total = total,
            durationPenalty = durationPenalty,
            efficiencyPenalty = efficiencyPenalty,
            latencyPenalty = latencyPenalty,
            wakePenalty = wakePenalty,
            snorePenalty = snorePenalty,
            environmentPenalty = environmentPenalty,
            suggestions = suggestions
        )
    }

    /** 没有任何分钟数据时的结果（例如刚开始就结束）。 */
    private fun emptyAnalysis(): SleepAnalysis = SleepAnalysis(
        stages = emptyList(),
        sleepOnsetIndex = -1,
        finalWakeIndex = -1,
        monitoredMillis = 0L,
        totalSleepMillis = 0L,
        sleepLatencyMillis = 0L,
        wakeAfterSleepOnsetMillis = 0L,
        wakeCount = 0,
        sleepEfficiency = 0f,
        calmMillis = 0L,
        restlessMillis = 0L,
        snoreEventCount = 0,
        averageNoiseDb = 0f,
        averageLightLux = 0f,
        lowMotionWarning = false,
        score = SleepScore(
            total = 0,
            durationPenalty = 0,
            efficiencyPenalty = 0,
            latencyPenalty = 0,
            wakePenalty = 0,
            snorePenalty = 0,
            environmentPenalty = 0,
            suggestions = listOf("没有记录到数据，至少需要监测 1 分钟以上。")
        )
    )
}
