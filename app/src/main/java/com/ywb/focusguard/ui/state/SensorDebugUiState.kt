package com.ywb.focusguard.ui.state

/**
 * 传感器调试页状态：把睡眠算法用到的原始信号和实时阈值摆在一起，方便真机校准。
 *
 * @property sleepMonitoring 睡眠监测正在后台运行；此时调试页暂停采集，避免两个 AudioRecord 抢占麦克风。
 * @property audioGranted 是否有录音权限。
 * @property motionValues 最近约 30 秒的 |Δa| 序列，单位 m/s²。
 * @property motionThreshold 当前自适应体动阈值。
 * @property movementSecondsLastMinute 最近 60 秒内检测到体动的秒数（与睡眠记录的统计口径一致）。
 * @property soundDbValues 最近约 15 秒的相对 dB 序列。
 * @property baselineDb 当前估计的环境底噪。
 * @property triggerDb 当前声音事件触发线。
 * @property dominantHz 最新一帧的主频估计。
 * @property recentSoundEvents 最近的声音事件，新的在前。
 * @property lux 最新光照读数。
 * @property exportMessage 导出结果提示；null 表示没有提示。
 */
data class SensorDebugUiState(
    val sleepMonitoring: Boolean = false,
    val audioGranted: Boolean = false,
    val motionValues: List<Float> = emptyList(),
    val motionThreshold: Float = 0f,
    val movementSecondsLastMinute: Int = 0,
    val soundDbValues: List<Float> = emptyList(),
    val baselineDb: Float = 0f,
    val triggerDb: Float = 0f,
    val dominantHz: Float = 0f,
    val recentSoundEvents: List<SoundEventUi> = emptyList(),
    val lux: Float = 0f,
    val exportMessage: String? = null
)

/**
 * 调试页中一条声音事件的展示文案。
 *
 * @property timeText 发生时间 HH:mm:ss。
 * @property detailText 时长、峰值、主频。
 * @property isSnoreLike 是否判定为疑似鼾声，页面高亮显示。
 */
data class SoundEventUi(
    val timeText: String,
    val detailText: String,
    val isSnoreLike: Boolean
)
