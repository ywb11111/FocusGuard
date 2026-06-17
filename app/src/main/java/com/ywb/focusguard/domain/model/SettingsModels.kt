package com.ywb.focusguard.domain.model

data class UserSettings(
    val defaultFocusMinutes: Int = 25,
    val noiseThresholdDb: Float = 65f,
    val comfortableLightMinLux: Float = 100f,
    val comfortableLightMaxLux: Float = 500f,
    val backgroundMonitoringEnabled: Boolean = false,
    val dailyReportEnabled: Boolean = true
)

data class PermissionState(
    val audioGranted: Boolean = false,
    val notificationGranted: Boolean = false,
    val usageStatsGranted: Boolean = false
)
