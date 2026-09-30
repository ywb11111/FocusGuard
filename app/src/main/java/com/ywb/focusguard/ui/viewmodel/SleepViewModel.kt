package com.ywb.focusguard.ui.viewmodel

import android.app.Application
import android.content.Context
import android.os.PowerManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ywb.focusguard.data.repository.SleepRepository
import com.ywb.focusguard.domain.model.SleepPlacement
import com.ywb.focusguard.service.SleepMonitorService
import com.ywb.focusguard.service.SleepStateHolder
import com.ywb.focusguard.ui.state.SleepChecklist
import com.ywb.focusguard.ui.state.SleepUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 睡眠页 ViewModel：只负责"观察状态 + 发出开始/结束指令"，不做任何采集。
 *
 * 采集全部在 SleepMonitorService 中进行。这样即使页面被销毁、整晚都没打开 App，记录也不受影响；
 * ViewModel 被重建后，只要重新观察 Room 和 SleepStateHolder 就能恢复页面。
 */
@HiltViewModel
class SleepViewModel @Inject constructor(
    application: Application,
    /** 睡眠记录的读取和中断记录的收尾。 */
    private val sleepRepository: SleepRepository,
    /** 复用已有的麦克风、通知权限检查。 */
    private val permissionManager: PermissionManager
) : AndroidViewModel(application) {

    /** 用户本次手动选择的摆放位置；null 表示沿用上一晚。 */
    private val selectedPlacement = MutableStateFlow<SleepPlacement?>(null)

    /** 睡前检查结果，页面恢复（onResume）时刷新。 */
    private val checklist = MutableStateFlow(SleepChecklist())

    /** 是否已发出结束请求、正在等待分析完成。 */
    private val finishing = MutableStateFlow(false)

    /**
     * 一次性导航事件：某晚记录已完成分析，携带记录 id。
     * Channel + receiveAsFlow 保证每个事件只被页面消费一次，屏幕旋转后不会重复跳转。
     */
    private val _finishedEvents = Channel<Long>(Channel.BUFFERED)
    val finishedEvents: Flow<Long> = _finishedEvents.receiveAsFlow()

    /**
     * 页面状态：Room 进行中记录 + Service 是否存活 + 实时状态 + 本地选择，合成一个状态机。
     * 判断顺序很重要：先看"结束中"，再看"有进行中记录"，最后才是"准备"。
     */
    val uiState: StateFlow<SleepUiState> = combine(
        sleepRepository.observeActiveSession(),
        sleepRepository.observeSessions(),
        SleepStateHolder.serviceRunning,
        SleepStateHolder.liveStatus,
        combine(selectedPlacement, checklist, finishing, ::Triple)
    ) { active, sessions, serviceRunning, live, (placement, checks, isFinishing) ->
        when {
            isFinishing -> SleepUiState.Finishing
            active != null && serviceRunning -> SleepUiState.Recording(
                session = active,
                live = live?.takeIf { it.sessionId == active.id }
            )
            active != null -> SleepUiState.Interrupted(active)
            else -> {
                val recent = sessions.take(7)
                SleepUiState.Ready(
                    placement = placement ?: sessions.firstOrNull()?.placement ?: SleepPlacement.BED,
                    checklist = checks,
                    recentSessions = recent,
                    trendScores = recent.reversed().map { it.score.toFloat() }
                )
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SleepUiState.Loading
    )

    init {
        // Service 分析完成后转发成页面导航事件
        viewModelScope.launch {
            SleepStateHolder.finishedEvents.collect { sessionId ->
                finishing.value = false
                _finishedEvents.send(sessionId)
            }
        }
        refreshChecklist()
    }

    /** 选择手机摆放位置。 */
    fun selectPlacement(placement: SleepPlacement) {
        selectedPlacement.value = placement
    }

    /** 重新检查权限和电池优化状态；从系统设置页返回时调用。 */
    fun refreshChecklist() {
        val powerManager = getApplication<Application>().getSystemService(Context.POWER_SERVICE) as PowerManager
        checklist.value = SleepChecklist(
            audioGranted = permissionManager.isAudioPermissionGranted(),
            notificationGranted = permissionManager.isNotificationPermissionGranted(),
            batteryOptimizationIgnored = powerManager.isIgnoringBatteryOptimizations(
                getApplication<Application>().packageName
            )
        )
        permissionManager.refreshPermissionState()
    }

    /** 开始睡眠监测：只发指令，记录由 Service 创建。 */
    fun startSleep() {
        val state = uiState.value as? SleepUiState.Ready ?: return
        SleepMonitorService.start(getApplication(), state.placement)
    }

    /**
     * 正常结束：请 Service 保存最后数据并分析，完成后通过 finishedEvents 跳转报告页。
     * 15 秒兜底：万一 Service 异常没有回应，页面不会一直停在"分析中"。
     */
    fun stopSleep() {
        finishing.value = true
        SleepMonitorService.stop(getApplication())
        viewModelScope.launch {
            delay(15_000)
            finishing.value = false
        }
    }

    /** 被中断的记录：Service 已不在，直接由 Repository 收尾并生成报告。 */
    fun finishInterrupted(sessionId: Long) {
        viewModelScope.launch {
            finishing.value = true
            val session = sleepRepository.finishInterruptedSession(sessionId)
            finishing.value = false
            session?.let { _finishedEvents.send(it.id) }
        }
    }
}
