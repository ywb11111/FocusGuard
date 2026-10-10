package com.ywb.focusguard.service

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 闹钟响铃状态的内存共享对象，AlarmRingService 写入，AlarmActivity 和睡眠页读取。
 *
 * AlarmActivity 靠它知道"响铃已在别处被关掉"（例如用户点了通知上的按钮），然后自动关闭自己；
 * 睡眠页靠它在 App 内也显示"关闭 / 贪睡"，防止通知被屏蔽时无法停止响铃。
 */
object AlarmStateHolder {

    /**
     * 正在响铃的信息。
     *
     * @property startedAt 开始响铃的时间。
     * @property smartEarly 是否为智能提前唤醒（页面文案不同）。
     */
    data class Ringing(
        val startedAt: Long,
        val smartEarly: Boolean
    )

    private val _ringing = MutableStateFlow<Ringing?>(null)

    /** 正在响铃时非 null。 */
    val ringing: StateFlow<Ringing?> = _ringing.asStateFlow()

    /** 开始响铃。 */
    fun startRinging(smartEarly: Boolean) {
        _ringing.value = Ringing(startedAt = System.currentTimeMillis(), smartEarly = smartEarly)
    }

    /** 停止响铃（关闭、贪睡、超时或服务销毁）。 */
    fun stopRinging() {
        _ringing.value = null
    }
}
