package com.ywb.focusguard.service

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 专注会话状态共享对象，用于 ViewModel 和 Service 之间通信。
 *
 * ViewModel 更新状态，Service 订阅并更新通知。
 *
 * 这是一个简化实现，生产环境建议使用更健壮的消息机制。
 */
object SessionStateHolder {

    /** 当前会话 ID，0 表示无会话。 */
    private val _sessionId = MutableStateFlow(0L)
    val sessionId: StateFlow<Long> = _sessionId.asStateFlow()

    /** 剩余时间（毫秒），用于通知显示。 */
    private val _remainingMillis = MutableStateFlow(0L)
    val remainingMillis: StateFlow<Long> = _remainingMillis.asStateFlow()

    /** 是否暂停。 */
    private val _isPaused = MutableStateFlow(false)
    val isPaused: StateFlow<Boolean> = _isPaused.asStateFlow()

    /**
     * 开始新会话。
     */
    fun startSession(sessionId: Long, durationMillis: Long) {
        _sessionId.value = sessionId
        _remainingMillis.value = durationMillis
        _isPaused.value = false
    }

    /**
     * 更新剩余时间。
     */
    fun updateRemaining(remaining: Long) {
        _remainingMillis.value = remaining
    }

    /**
     * 设置暂停状态。
     */
    fun setPaused(paused: Boolean) {
        _isPaused.value = paused
    }

    /**
     * 结束会话。
     */
    fun endSession() {
        _sessionId.value = 0L
        _remainingMillis.value = 0L
        _isPaused.value = false
    }
}