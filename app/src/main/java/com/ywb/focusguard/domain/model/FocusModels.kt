package com.ywb.focusguard.domain.model

/**
 * 一次完整专注会话的领域模型，供 Repository、ViewModel 和 UI 共享。
 *
 * @property id 会话唯一 id，0 通常表示尚未插入数据库。
 * @property startTime 开始时间的 Unix 毫秒时间戳。
 * @property endTime 结束时间；null 表示仍在进行。
 * @property durationMillis 去除暂停后的有效专注时长。
 * @property averageNoiseDb 平均相对噪声值。
 * @property maxNoiseDb 最大相对噪声值。
 * @property averageLightLux 平均光照强度。
 * @property movementCount 已确认的移动事件数量。
 * @property distractionCount 分心行为数量。
 * @property score 可解释评分，范围 0 到 100。
 * @property note 用户备注或系统说明。
 */
data class FocusSession(
    val id: Long,
    val startTime: Long,
    val endTime: Long?,
    val durationMillis: Long,
    val averageNoiseDb: Float,
    val maxNoiseDb: Float,
    val averageLightLux: Float,
    val movementCount: Int,
    val distractionCount: Int,
    val score: Int,
    val note: String?
)

/**
 * 开始专注时使用的配置。
 *
 * @property durationMinutes 目标专注时长，单位分钟。
 * @property monitorNoise 是否采集噪声。
 * @property monitorLight 是否采集光照。
 * @property monitorMotion 是否检测移动。
 */
data class FocusConfig(
    val durationMinutes: Int,
    val monitorNoise: Boolean = true,
    val monitorLight: Boolean = true,
    val monitorMotion: Boolean = true
)

/**
 * 评分结果及各项扣分，用于详情页提供可解释反馈。
 *
 * @property total 最终总分，范围 0 到 100。
 * @property noisePenalty 噪声扣分。
 * @property lightPenalty 光照扣分。
 * @property motionPenalty 移动扣分。
 * @property distractionPenalty 分心扣分。
 * @property suggestions 根据扣分项生成的改进建议。
 */
data class FocusScore(
    val total: Int,
    val noisePenalty: Int,
    val lightPenalty: Int,
    val motionPenalty: Int,
    val distractionPenalty: Int,
    val suggestions: List<String>
)

/**
 * 今日页和报告页使用的专注汇总。
 *
 * @property totalFocusMillis 已完成会话的总专注时长。
 * @property averageScore 已完成会话的平均分。
 * @property distractionCount 今日分心总次数。
 * @property sessionCount 今日已完成会话数量。
 */
data class TodaySummary(
    val totalFocusMillis: Long,
    val averageScore: Int,
    val distractionCount: Int,
    val sessionCount: Int
)

/**
 * 详情页需要的一次会话及其完整采样、事件和评分信息。
 *
 * @property session 会话主记录。
 * @property noiseSamples 按时间升序排列的噪声样本。
 * @property lightSamples 按时间升序排列的光照样本。
 * @property motionEvents 防抖后确认的移动事件。
 * @property score 本次会话的评分拆解。
 */
data class SessionDetail(
    val session: FocusSession,
    val noiseSamples: List<NoiseSample>,
    val lightSamples: List<LightSample>,
    val motionEvents: List<MotionSample>,
    val score: FocusScore
)

/**
 * 跨页面展示进行中会话时使用的精简状态。
 *
 * @property sessionId Room 中的真实会话 id。
 * @property startedAt 会话开始的 Unix 毫秒时间戳。
 * @property elapsedMillis 当前累计的有效专注时长。
 */
data class ActiveSession(
    val sessionId: Long,
    val startedAt: Long,
    val elapsedMillis: Long
)
