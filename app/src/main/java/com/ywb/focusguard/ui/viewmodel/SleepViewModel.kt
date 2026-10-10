package com.ywb.focusguard.ui.viewmodel

import android.app.Application
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.PowerManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ywb.focusguard.data.repository.SleepAlarmRepository
import com.ywb.focusguard.data.repository.SleepRepository
import com.ywb.focusguard.domain.analyzer.SmartAlarmPlanner
import com.ywb.focusguard.domain.model.SleepAlarmSettings
import com.ywb.focusguard.domain.model.SleepPlacement
import com.ywb.focusguard.service.AlarmRingService
import com.ywb.focusguard.service.AlarmStateHolder
import com.ywb.focusguard.service.SleepAlarmScheduler
import com.ywb.focusguard.service.SleepMonitorService
import com.ywb.focusguard.service.SleepStateHolder
import com.ywb.focusguard.ui.screen.clockText
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
import kotlinx.coroutines.flow.first
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
    private val permissionManager: PermissionManager,
    /** 智能闹钟设置的读写。 */
    private val sleepAlarmRepository: SleepAlarmRepository,
    /** 用于检查精确闹钟权限。 */
    private val alarmScheduler: SleepAlarmScheduler,
    /** 计算监测中页面展示的闹钟时间段，与 Service 使用同一套规则。 */
    private val smartAlarmPlanner: SmartAlarmPlanner
) : AndroidViewModel(application) {

    /**
     * 本页的本地输入汇总。主 combine 最多接收 5 个 Flow，先把本地状态合成一个再参与组合。
     */
    private data class LocalInputs(
        val placement: SleepPlacement?,
        val checklist: SleepChecklist,
        val finishing: Boolean,
        val alarm: SleepAlarmSettings,
        val alarmRinging: Boolean
    )

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
        combine(
            selectedPlacement,
            checklist,
            finishing,
            sleepAlarmRepository.settings,
            AlarmStateHolder.ringing
        ) { placement, checks, isFinishing, alarm, ringing ->
            LocalInputs(placement, checks, isFinishing, alarm, ringing != null)
        }
    ) { active, sessions, serviceRunning, live, inputs ->
        when {
            inputs.finishing -> SleepUiState.Finishing
            active != null && serviceRunning -> SleepUiState.Recording(
                session = active,
                live = live?.takeIf { it.sessionId == active.id },
                alarmWindowText = smartAlarmPlanner.plan(inputs.alarm, afterMillis = active.startTime)?.let { plan ->
                    "${clockText(plan.windowStartMillis)}–${clockText(plan.latestMillis)}"
                },
                alarmRinging = inputs.alarmRinging
            )
            active != null -> SleepUiState.Interrupted(active)
            else -> {
                val recent = sessions.take(7)
                SleepUiState.Ready(
                    placement = inputs.placement ?: sessions.firstOrNull()?.placement ?: SleepPlacement.BED,
                    checklist = inputs.checklist,
                    recentSessions = recent,
                    trendScores = recent.reversed().map { it.score.toFloat() },
                    alarm = inputs.alarm
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
        val app = getApplication<Application>()
        val powerManager = app.getSystemService(Context.POWER_SERVICE) as PowerManager
        checklist.value = SleepChecklist(
            audioGranted = permissionManager.isAudioPermissionGranted(),
            notificationGranted = permissionManager.isNotificationPermissionGranted(),
            batteryOptimizationIgnored = powerManager.isIgnoringBatteryOptimizations(app.packageName),
            exactAlarmAllowed = alarmScheduler.canScheduleExact(),
            // Android 14 起全屏通知需要用户授权，之前的版本声明权限即可
            fullScreenAllowed = Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE ||
                app.getSystemService(NotificationManager::class.java).canUseFullScreenIntent()
        )
        permissionManager.refreshPermissionState()
    }

    /** 开关智能闹钟。 */
    fun setAlarmEnabled(enabled: Boolean) = updateAlarm { it.copy(enabled = enabled) }

    /** 设置最晚起床时间。 */
    fun setAlarmTime(hour: Int, minute: Int) = updateAlarm { it.copy(wakeHour = hour, wakeMinute = minute) }

    /** 设置提前唤醒窗口长度。 */
    fun setAlarmWindow(minutes: Int) = updateAlarm { it.copy(windowMinutes = minutes) }

    /** 读取最新设置后修改并保存，保证多个字段的修改不会互相覆盖。 */
    private fun updateAlarm(transform: (SleepAlarmSettings) -> SleepAlarmSettings) {
        viewModelScope.launch {
            sleepAlarmRepository.update(transform(sleepAlarmRepository.settings.first()))
        }
    }

    /** 在 App 内关闭正在响的闹钟（同时结束睡眠记录）。 */
    fun dismissAlarm() {
        AlarmRingService.dismiss(getApplication())
    }

    /** 在 App 内贪睡。 */
    fun snoozeAlarm() {
        AlarmRingService.snooze(getApplication())
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
