package com.ywb.focusguard.domain.model

/**
 * 用户可持久化的应用设置。
 *
 * @property defaultFocusMinutes 默认专注时长，单位分钟。
 * @property noiseThresholdDb 噪声提醒阈值，单位相对 dB。
 * @property comfortableLightMinLux 舒适光照下限，单位 lux。
 * @property comfortableLightMaxLux 舒适光照上限，单位 lux。
 * @property backgroundMonitoringEnabled 是否允许专注期间后台监测。
 * @property dailyReportEnabled 是否生成每日总结。
 */
data class UserSettings(
    val defaultFocusMinutes: Int = 25,
    val noiseThresholdDb: Float = 65f,
    val comfortableLightMinLux: Float = 100f,
    val comfortableLightMaxLux: Float = 500f,
    val backgroundMonitoringEnabled: Boolean = false,
    val dailyReportEnabled: Boolean = true
)

/**
 * UI 使用的权限状态快照。
 *
 * @property audioGranted 是否已获得录音权限。
 * @property notificationGranted 是否已获得通知权限。
 * @property usageStatsGranted 是否已获得应用使用情况访问权。
 */
data class PermissionState(
    val audioGranted: Boolean = false,
    val notificationGranted: Boolean = false,
    val usageStatsGranted: Boolean = false
)
