package com.ywb.focusguard.service

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * 专注会话状态共享对象，用于 ViewModel 和 Service 之间通信。
 *
 * **设计说明**：
 * - 使用 StateFlow 管理状态，支持多订阅者
 * - 使用 SharedFlow 发送一次性事件（暂停/结束）
 * - ViewModel 更新状态，Service 订阅并更新通知
 * - 状态存储在内存中，App 被杀死后会丢失，但 Service 内部会独立计时
 */
object SessionStateHolder {

    // ==================== 状态（StateFlow，支持多订阅） ====================

    /** 当前会话 ID，0 表示无会话。 */
    private val _sessionId = MutableStateFlow(0L)
    val sessionId: StateFlow<Long> = _sessionId.asStateFlow()

    /** 剩余时间（毫秒），用于通知显示。 */
    private val _remainingMillis = MutableStateFlow(0L)
    val remainingMillis: StateFlow<Long> = _remainingMillis.asStateFlow()

    /** 是否暂停。 */
    private val _isPaused = MutableStateFlow(false)
    val isPaused: StateFlow<Boolean> = _isPaused.asStateFlow()

    // ==================== 事件（SharedFlow，一次性消费） ====================

    /**
     * 控制事件：暂停/继续/结束。
     *
     * 使用 SharedFlow 而不是回调，避免静态变量内存泄漏。
     */
    private val _controlEvents = MutableSharedFlow<ControlEvent>(extraBufferCapacity = 1)
    val controlEvents = _controlEvents.asSharedFlow()

    /** 控制事件类型。 */
    sealed class ControlEvent {
        data object TogglePause : ControlEvent()
        data object Finish : ControlEvent()
    }

    // ==================== 状态更新方法 ====================

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

    // ==================== 事件发送方法 ====================

    /**
     * 发送切换暂停/继续事件。
     */
    fun sendTogglePause() {
        _controlEvents.tryEmit(ControlEvent.TogglePause)
    }

    /**
     * 发送结束事件。
     */
    fun sendFinish() {
        _controlEvents.tryEmit(ControlEvent.Finish)
    }
}