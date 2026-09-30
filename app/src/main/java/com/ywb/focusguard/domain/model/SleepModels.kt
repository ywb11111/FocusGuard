package com.ywb.focusguard.domain.model

/**
 * 睡眠监测时手机的摆放位置，决定"体动"信号从哪里来。
 *
 * 为什么要让用户选：手机只有放在床垫上，翻身才会通过床垫传到加速度计；
 * 放在床头柜上时加速度计整晚几乎为 0，如果仍按体动判断就会把整晚都算成"安稳睡眠"，结论是错的。
 *
 * @property label 页面展示名称。
 * @property description 页面上的简短说明，提醒用户准确度差异。
 */
enum class SleepPlacement(val label: String, val description: String) {
    BED("枕边床垫上", "可检测翻身和体动，推荐"),
    NIGHTSTAND("床头柜上", "只分析声音和屏幕，准确度较低");

    companion object {
        /** 数据库存的是枚举名；遇到未知值时退回推荐的 BED，避免旧数据导致崩溃。 */
        fun fromName(name: String?): SleepPlacement =
            entries.firstOrNull { it.name == name } ?: BED
    }
}

/**
 * 一分钟睡眠"时间片"（epoch）的聚合数据，是睡眠分析的最小单位。
 *
 * 为什么按分钟聚合而不是保存原始样本：
 * 加速度计约 5 次/秒、麦克风 10 帧/秒，一晚原始样本有几十万条；
 * 按分钟聚合后一晚只有约 480 行，数据库和分析都很轻，也和医学上体动记录仪按 epoch 计分的做法一致。
 *
 * 由 SleepEpochAccumulator 在 SleepMonitorService 中每分钟生成，写入 Room，结束后交给 SleepAnalyzer。
 *
 * @property startTime 这一分钟开始的 Unix 毫秒时间戳。
 * @property movementSeconds 这一分钟里检测到体动的秒数，范围 0..60；床头柜模式恒为 0。
 * @property averageNoiseDb 平均相对噪声，单位 dB；未开麦克风时为 0。
 * @property maxNoiseDb 最大相对噪声，单位 dB。
 * @property soundEventCount 明显高于环境底噪的短促声音事件数（翻身摩擦、咳嗽、说话等）。
 * @property snoreEventCount 其中被判定为"疑似鼾声"的事件数。
 * @property averageLightLux 平均光照，单位 lux。
 * @property screenOnSeconds 这一分钟里屏幕亮着的秒数，用于识别半夜看手机。
 */
data class SleepEpoch(
    val startTime: Long,
    val movementSeconds: Int,
    val averageNoiseDb: Float,
    val maxNoiseDb: Float,
    val soundEventCount: Int,
    val snoreEventCount: Int,
    val averageLightLux: Float,
    val screenOnSeconds: Int
)

/**
 * 每分钟的估算睡眠状态。
 *
 * 注意：手机没有脑电和心率，无法区分医学意义上的深睡、浅睡、REM。
 * 这里只根据体动和声音估算"清醒 / 有体动的睡眠 / 安稳睡眠"，页面文案也必须说明是估算。
 *
 * @property label 页面展示名称。
 */
enum class SleepStage(val label: String) {
    AWAKE("清醒"),
    RESTLESS("浅睡·体动"),
    CALM("安稳")
}

/**
 * 一晚睡眠记录的领域模型，供 Repository、ViewModel 和 UI 共享。
 *
 * @property id Room 自增主键；0 表示尚未插入。
 * @property startTime 开始监测的 Unix 毫秒时间戳。
 * @property endTime 结束监测时间；null 表示仍在监测中。
 * @property placement 手机摆放位置。
 * @property audioEnabled 本晚是否成功启用麦克风分析。
 * @property sleepOnsetTime 估算的入睡时间；数据不足时为 null。
 * @property finalWakeTime 估算的最终醒来时间；数据不足时为 null。
 * @property monitoredMillis 实际记录到数据的时长（epoch 数 × 1 分钟），可能小于 endTime - startTime。
 * @property totalSleepMillis 估算的总睡眠时长。
 * @property sleepLatencyMillis 入睡耗时：从开始监测到入睡。
 * @property wakeCount 入睡后持续 3 分钟以上的夜醒次数。
 * @property snoreEventCount 整晚疑似鼾声事件数。
 * @property averageNoiseDb 整晚平均相对噪声。
 * @property averageLightLux 整晚平均光照。
 * @property score 睡眠评分 0..100。
 * @property note 系统说明，例如"记录被系统中断"。
 */
data class SleepSession(
    val id: Long,
    val startTime: Long,
    val endTime: Long?,
    val placement: SleepPlacement,
    val audioEnabled: Boolean,
    val sleepOnsetTime: Long?,
    val finalWakeTime: Long?,
    val monitoredMillis: Long,
    val totalSleepMillis: Long,
    val sleepLatencyMillis: Long,
    val wakeCount: Int,
    val snoreEventCount: Int,
    val averageNoiseDb: Float,
    val averageLightLux: Float,
    val score: Int,
    val note: String?
)

/**
 * 睡眠评分及各项扣分，和专注评分一样采用"满分减扣分"的可解释模型。
 *
 * @property total 最终得分 0..100。
 * @property durationPenalty 睡眠时长扣分。
 * @property efficiencyPenalty 睡眠效率扣分。
 * @property latencyPenalty 入睡耗时扣分。
 * @property wakePenalty 夜醒扣分。
 * @property snorePenalty 鼾声扣分。
 * @property environmentPenalty 卧室噪声、光照扣分。
 * @property suggestions 根据扣分项生成的改进建议。
 */
data class SleepScore(
    val total: Int,
    val durationPenalty: Int,
    val efficiencyPenalty: Int,
    val latencyPenalty: Int,
    val wakePenalty: Int,
    val snorePenalty: Int,
    val environmentPenalty: Int,
    val suggestions: List<String>
)

/**
 * SleepAnalyzer 对一晚 epoch 序列的完整分析结果。
 *
 * @property stages 与输入 epoch 一一对应的估算状态。
 * @property sleepOnsetIndex 入睡 epoch 下标；-1 表示整晚没有识别到入睡。
 * @property finalWakeIndex 最后一个睡眠 epoch 的下标；-1 表示没有睡眠。
 * @property monitoredMillis 有数据的总时长。
 * @property totalSleepMillis 睡眠 epoch 总时长。
 * @property sleepLatencyMillis 入睡耗时。
 * @property wakeAfterSleepOnsetMillis 入睡后到最终醒来之间的清醒时长（WASO）。
 * @property wakeCount 入睡后持续 ≥ 3 分钟的夜醒次数。
 * @property sleepEfficiency 睡眠效率 = 总睡眠 / 记录时长，0..1。
 * @property calmMillis 安稳睡眠时长。
 * @property restlessMillis 有体动的睡眠时长。
 * @property snoreEventCount 疑似鼾声事件数。
 * @property averageNoiseDb 平均相对噪声。
 * @property averageLightLux 平均光照。
 * @property lowMotionWarning 床垫模式下整晚几乎没有体动，提示手机可能没放在床上。
 * @property score 评分拆解。
 */
data class SleepAnalysis(
    val stages: List<SleepStage>,
    val sleepOnsetIndex: Int,
    val finalWakeIndex: Int,
    val monitoredMillis: Long,
    val totalSleepMillis: Long,
    val sleepLatencyMillis: Long,
    val wakeAfterSleepOnsetMillis: Long,
    val wakeCount: Int,
    val sleepEfficiency: Float,
    val calmMillis: Long,
    val restlessMillis: Long,
    val snoreEventCount: Int,
    val averageNoiseDb: Float,
    val averageLightLux: Float,
    val lowMotionWarning: Boolean,
    val score: SleepScore
)

/**
 * 睡眠详情页需要的完整数据：主记录 + 分钟序列 + 实时计算的分析结果。
 *
 * @property session 睡眠主记录。
 * @property epochs 按时间升序的分钟数据。
 * @property analysis 由 SleepAnalyzer 根据 epochs 计算的结果（算法确定，可随时重算）。
 */
data class SleepDetail(
    val session: SleepSession,
    val epochs: List<SleepEpoch>,
    val analysis: SleepAnalysis
)

/**
 * 睡眠服务运行时发布给 UI 的实时状态，只存在内存中。
 *
 * @property sessionId 当前记录的 Room id。
 * @property startedAt 开始时间。
 * @property recordedEpochs 已写入数据库的分钟数。
 * @property lastMovementSeconds 上一分钟体动秒数。
 * @property lastNoiseDb 上一分钟平均噪声。
 * @property audioEnabled 麦克风是否在工作。
 * @property placement 手机摆放位置。
 */
data class SleepLiveStatus(
    val sessionId: Long,
    val startedAt: Long,
    val recordedEpochs: Int,
    val lastMovementSeconds: Int,
    val lastNoiseDb: Float,
    val audioEnabled: Boolean,
    val placement: SleepPlacement
)
