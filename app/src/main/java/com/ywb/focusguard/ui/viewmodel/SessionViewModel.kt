package com.ywb.focusguard.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ywb.focusguard.data.repository.EnvironmentRepository
import com.ywb.focusguard.data.repository.FocusRepository
import com.ywb.focusguard.domain.model.EnvironmentSnapshot
import com.ywb.focusguard.domain.model.FocusConfig
import com.ywb.focusguard.domain.model.LightLevel
import com.ywb.focusguard.domain.model.LightSample
import com.ywb.focusguard.domain.model.MotionSample
import com.ywb.focusguard.domain.model.NoiseSample
import com.ywb.focusguard.ui.state.SessionUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

/**
 * 专注页状态机：负责计时、暂停/继续、采样保存和页面状态转换。
 *
 * 数据库写入交给 FocusRepository，硬件监听交给 EnvironmentRepository。
 * Running 期间每 10 秒保存一次光照采样，收到移动事件时保存移动记录。
 */
@HiltViewModel
class SessionViewModel @Inject constructor(
    /** 提供当前环境快照；光照和移动为真实传感器，噪声暂为演示数据。 */
    environmentRepository: EnvironmentRepository,
    /** 创建和结束 Room 会话，保存采样数据。 */
    private val focusRepository: FocusRepository
) : ViewModel() {

    /** 当前固定使用的默认专注配置，后续从 SettingsRepository 读取。 */
    private val defaultConfig = FocusConfig(durationMinutes = 25)

    /** 目标专注总时长，单位毫秒。 */
    private val sessionDurationMillis = defaultConfig.durationMinutes * 60 * 1000L

    /** 采样间隔：每 10 秒保存一次光照数据。 */
    private val sampleIntervalMillis = 10_000L

    /** 环境数据流，用于采样和状态显示。 */
    private val environment: StateFlow<EnvironmentSnapshot?> = environmentRepository
        .observeEnvironmentSnapshot()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null
        )

    /** ViewModel 内部可修改的专注状态源。 */
    private val _uiState = MutableStateFlow<SessionUiState>(
        SessionUiState.Ready(defaultConfig, null)
    )
    val uiState: StateFlow<SessionUiState> = _uiState.asStateFlow()

    /** 计时协程 Job。 */
    private var tickerJob: Job? = null

    /** 采样协程 Job（定时保存光照）。 */
    private var samplingJob: Job? = null

    /** 移动事件监听 Job。 */
    private var motionWatchJob: Job? = null

    /** 当前 Room 会话 id；0 表示尚未成功创建会话。 */
    private var activeSessionId = 0L

    /** 本轮运行开始时间。 */
    private var runStartedAt = 0L

    /** 暂停前已累计的时长。 */
    private var accumulatedMillis = 0L

    init {
        // 准备阶段持续更新环境预检
        viewModelScope.launch {
            environment.collect { snapshot ->
                _uiState.update { current ->
                    if (current is SessionUiState.Ready) {
                        current.copy(environment = snapshot)
                    } else {
                        current
                    }
                }
            }
        }
    }

    /** 在 Room 创建进行中记录，启动计时和采样。 */
    fun startSession() {
        val now = System.currentTimeMillis()
        viewModelScope.launch {
            activeSessionId = focusRepository.startSession(defaultConfig)
            runStartedAt = now
            accumulatedMillis = 0L
            emitRunningState(elapsedMillis = 0L)
            startTicker()
            startSampling()
            startMotionWatch()
        }
    }

    /** 暂停：停止计时和采样。 */
    fun pauseSession() {
        val current = _uiState.value as? SessionUiState.Running ?: return
        tickerJob?.cancel()
        samplingJob?.cancel()
        motionWatchJob?.cancel()
        accumulatedMillis = current.elapsedMillis
        _uiState.value = SessionUiState.Paused(
            sessionId = current.sessionId,
            elapsedMillis = current.elapsedMillis,
            remainingMillis = current.remainingMillis,
            lightLevel = current.lightLevel,
            movementCount = current.movementCount
        )
    }

    /** 继续：恢复计时和采样。 */
    fun resumeSession() {
        val current = _uiState.value as? SessionUiState.Paused ?: return
        runStartedAt = System.currentTimeMillis()
        emitRunningState(elapsedMillis = current.elapsedMillis)
        startTicker()
        startSampling()
        startMotionWatch()
    }

    /** 结束：停止所有协程，保存统计结果。 */
    fun finishSession() {
        val elapsedMillis = currentElapsedMillis()
        tickerJob?.cancel()
        samplingJob?.cancel()
        motionWatchJob?.cancel()
        completeSession(elapsedMillis)
    }

    /** 重置到准备状态。 */
    fun resetSession() {
        tickerJob?.cancel()
        samplingJob?.cancel()
        motionWatchJob?.cancel()
        activeSessionId = 0L
        runStartedAt = 0L
        accumulatedMillis = 0L
        _uiState.value = SessionUiState.Ready(defaultConfig, environment.value)
    }

    /** 启动计时器：每秒更新一次。 */
    private fun startTicker() {
        tickerJob?.cancel()
        tickerJob = viewModelScope.launch {
            while (true) {
                val elapsedMillis = currentElapsedMillis()
                emitRunningState(elapsedMillis)
                if (elapsedMillis >= sessionDurationMillis) {
                    completeSession(elapsedMillis)
                    break
                }
                delay(1_000L.milliseconds)
            }
        }
    }

    /**
     * 启动定时采样：每 10 秒保存一次光照和噪声数据。
     *
     * 采样间隔取 10 秒是权衡数据量和曲线细腻度：
     * - 25 分钟会话产生约 150 条数据
     * - Room 足够处理，UI 曲线也足够平滑
     */
    private fun startSampling() {
        samplingJob?.cancel()
        samplingJob = viewModelScope.launch {
            while (true) {
                val snapshot = environment.value
                val sessionId = activeSessionId
                if (sessionId != 0L && snapshot != null) {
                    // 保存光照采样
                    focusRepository.saveLightSample(
                        sessionId = sessionId,
                        sample = LightSample(
                            timestamp = System.currentTimeMillis(),
                            lux = snapshot.light.lux,
                            level = snapshot.light.level
                        )
                    )
                    // 保存噪声采样
                    focusRepository.saveNoiseSample(
                        sessionId = sessionId,
                        sample = NoiseSample(
                            timestamp = System.currentTimeMillis(),
                            decibel = snapshot.noise.decibel,
                            level = snapshot.noise.level
                        )
                    )
                }
                delay(sampleIntervalMillis.milliseconds)
            }
        }
    }

    /**
     * 启动移动事件监听：收到 isSignificantMove = true 时保存。
     *
     * 移动事件不按定时采样，而是事件触发：
     * - MotionEventDetector 已做防抖（600ms 窗口 + 1200ms 冷却）
     * - 每次确认事件保存一条记录，结束时统计条数即为移动次数
     */
    private fun startMotionWatch() {
        motionWatchJob?.cancel()
        motionWatchJob = viewModelScope.launch {
            environment.collect { snapshot ->
                val sessionId = activeSessionId
                val motion = snapshot?.motion
                if (sessionId != 0L && motion != null && motion.isSignificantMove) {
                    focusRepository.saveMotionEvent(
                        sessionId = sessionId,
                        event = MotionSample(
                            timestamp = motion.timestamp,
                            magnitude = motion.magnitude,
                            isSignificantMove = true,
                            isMoving = motion.isMoving
                        )
                    )
                }
            }
        }
    }

    /** 根据累计时长和最新环境快照生成 Running 状态。 */
    private fun emitRunningState(elapsedMillis: Long) {
        val snapshot = environment.value
        _uiState.value = SessionUiState.Running(
            sessionId = activeSessionId,
            elapsedMillis = elapsedMillis,
            remainingMillis = (sessionDurationMillis - elapsedMillis).coerceAtLeast(0L),
            noiseSamples = emptyList(),
            lightLevel = snapshot?.light?.level ?: LightLevel.COMFORTABLE,
            movementCount = 0 // Running 期间不累计，结束时从数据库统计
        )
    }

    /** 持久化结束结果。 */
    private fun completeSession(elapsedMillis: Long) {
        val sessionId = activeSessionId
        if (sessionId == 0L) return
        viewModelScope.launch {
            val session = focusRepository.finishSession(sessionId, elapsedMillis)
            _uiState.value = SessionUiState.Finished(session)
        }
    }

    /** 计算当前累计时长。 */
    private fun currentElapsedMillis(): Long {
        val runningExtra = if (runStartedAt == 0L) {
            0L
        } else {
            System.currentTimeMillis() - runStartedAt
        }
        return accumulatedMillis + runningExtra
    }
}