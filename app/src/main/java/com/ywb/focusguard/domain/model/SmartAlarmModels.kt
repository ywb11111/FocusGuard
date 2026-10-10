package com.ywb.focusguard.domain.model

/**
 * 智能闹钟设置，持久化在 DataStore（sleep_alarm 文件），睡眠页准备阶段编辑。
 *
 * "智能"的含义：在 [最晚时间 - 窗口, 最晚时间] 这段时间里，只要检测到浅睡（体动增多）就提前叫醒；
 * 一直没检测到，就在最晚时间准时响。浅睡时被叫醒比深睡中被打断更不容易昏沉。
 *
 * @property enabled 是否开启。
 * @property wakeHour 最晚起床时间的小时（0..23）。
 * @property wakeMinute 最晚起床时间的分钟（0..59）。
 * @property windowMinutes 提前唤醒窗口长度，单位分钟。
 */
data class SleepAlarmSettings(
    val enabled: Boolean = false,
    val wakeHour: Int = 7,
    val wakeMinute: Int = 30,
    val windowMinutes: Int = 30
)

/**
 * 某一晚的闹钟计划，由 SmartAlarmPlanner 根据设置和开始监测时间算出。
 *
 * @property windowStartMillis 智能唤醒窗口开始时间。
 * @property latestMillis 最晚响铃时间，也是提前注册到 AlarmManager 的兜底闹钟时间。
 */
data class SmartAlarmPlan(
    val windowStartMillis: Long,
    val latestMillis: Long
)
