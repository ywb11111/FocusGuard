package com.ywb.focusguard.service

import com.ywb.focusguard.domain.model.SleepLiveStatus
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * SleepMonitorService 与睡眠页面之间的内存通信通道，思路与 SessionStateHolder 相同。
 *
 * **和 Room 的分工**：
 * - Room 中 endTime 为 null 的记录代表"有一晚正在记录"，是持久的真相，进程被杀也不会丢；
 * - 这里的状态只代表"本进程里的 Service 正在工作"，进程一死就归零。
 * 页面把两者组合起来就能区分：Room 有进行中记录 + Service 在跑 = 正常监测；
 * Room 有进行中记录 + Service 不在 = 记录被系统中断，需要用户手动生成报告。
 */
object SleepStateHolder {

    /** Service 是否存活：onCreate 置 true，onDestroy 置 false。 */
    private val _serviceRunning = MutableStateFlow(false)
    val serviceRunning: StateFlow<Boolean> = _serviceRunning.asStateFlow()

    /** Service 每分钟发布的实时状态；null 表示还没有开始采集。 */
    private val _liveStatus = MutableStateFlow<SleepLiveStatus?>(null)
    val liveStatus: StateFlow<SleepLiveStatus?> = _liveStatus.asStateFlow()

    /**
     * 一次性事件：某晚记录已结束并完成分析，携带睡眠记录 id。
     * 用 SharedFlow 而不是 StateFlow：导航到报告页只应发生一次，不能在页面重建时重复触发。
     */
    private val _finishedEvents = MutableSharedFlow<Long>(extraBufferCapacity = 1)
    val finishedEvents: SharedFlow<Long> = _finishedEvents.asSharedFlow()

    /** Service 创建或销毁时调用。 */
    fun setServiceRunning(running: Boolean) {
        _serviceRunning.value = running
        if (!running) _liveStatus.value = null
    }

    /** 发布最新实时状态。 */
    fun updateLiveStatus(status: SleepLiveStatus) {
        _liveStatus.value = status
    }

    /** 发布"记录已结束"事件。 */
    fun emitFinished(sessionId: Long) {
        _finishedEvents.tryEmit(sessionId)
    }
}
