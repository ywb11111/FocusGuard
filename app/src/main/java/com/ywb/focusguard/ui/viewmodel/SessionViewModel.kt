package com.ywb.focusguard.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ywb.focusguard.data.repository.EnvironmentRepository
import com.ywb.focusguard.data.repository.FocusRepository
import com.ywb.focusguard.domain.model.EnvironmentSnapshot
import com.ywb.focusguard.domain.model.FocusConfig
import com.ywb.focusguard.domain.model.LightLevel
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

@HiltViewModel
class SessionViewModel @Inject constructor(
    environmentRepository: EnvironmentRepository,
    private val focusRepository: FocusRepository
) : ViewModel() {
    private val defaultConfig = FocusConfig(durationMinutes = 25)
    private val sessionDurationMillis = defaultConfig.durationMinutes * 60 * 1000L

    // 环境数据单独保存成 StateFlow，计时状态需要它时读取最新快照即可。
    // 这里先是演示数据，后续会替换成 SensorManager / AudioRecord 的真实数据源。
    private val environment: StateFlow<EnvironmentSnapshot?> = environmentRepository
        .observeEnvironmentSnapshot()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null
        )

    private val _uiState = MutableStateFlow<SessionUiState>(
        SessionUiState.Ready(defaultConfig, null)
    )
    // 对外只暴露只读 StateFlow，避免 UI 层直接修改状态。
    val uiState: StateFlow<SessionUiState> = _uiState.asStateFlow()

    // tickerJob 表示当前计时协程。暂停、结束、重置时必须 cancel，避免多个计时器同时跑。
    private var tickerJob: Job? = null
    private var activeSessionId = 0L
    // runStartedAt 表示“本轮运行”开始时间；暂停再继续时会重新赋值。
    private var runStartedAt = 0L
    // accumulatedMillis 保存暂停前已经累计的时长。继续后用它加上本轮运行时间。
    private var accumulatedMillis = 0L

    init {
        viewModelScope.launch {
            environment.collect { snapshot ->
                _uiState.update { current ->
                    // 只有准备状态需要被环境预检数据刷新；Running/Paused/Finished 不应被环境流冲掉。
                    if (current is SessionUiState.Ready) {
                        current.copy(environment = snapshot)
                    } else {
                        current
                    }
                }
            }
        }
    }

    fun startSession() {
        val now = System.currentTimeMillis()
        viewModelScope.launch {
            activeSessionId = focusRepository.startSession(defaultConfig)
            runStartedAt = now
            accumulatedMillis = 0L
            // 先立即发出 Running 状态，让用户点开始后 UI 立刻切换，而不是等 1 秒。
            emitRunningState(elapsedMillis = 0L)
            startTicker()
        }
    }

    fun pauseSession() {
        val current = _uiState.value as? SessionUiState.Running ?: return
        tickerJob?.cancel()
        // 暂停时把当前已用时长固化下来，继续时不再从 0 开始。
        accumulatedMillis = current.elapsedMillis
        _uiState.value = SessionUiState.Paused(
            sessionId = current.sessionId,
            elapsedMillis = current.elapsedMillis,
            remainingMillis = current.remainingMillis,
            lightLevel = current.lightLevel,
            movementCount = current.movementCount
        )
    }

    fun resumeSession() {
        val current = _uiState.value as? SessionUiState.Paused ?: return
        // 继续时只重置本轮开始时间，不清空 accumulatedMillis。
        runStartedAt = System.currentTimeMillis()
        emitRunningState(elapsedMillis = current.elapsedMillis)
        startTicker()
    }

    fun finishSession() {
        val elapsedMillis = currentElapsedMillis()
        tickerJob?.cancel()
        completeSession(elapsedMillis)
    }

    fun resetSession() {
        tickerJob?.cancel()
        activeSessionId = 0L
        runStartedAt = 0L
        accumulatedMillis = 0L
        _uiState.value = SessionUiState.Ready(defaultConfig, environment.value)
    }

    private fun startTicker() {
        tickerJob?.cancel()
        tickerJob = viewModelScope.launch {
            while (true) {
                val elapsedMillis = currentElapsedMillis()
                emitRunningState(elapsedMillis)
                if (elapsedMillis >= sessionDurationMillis) {
                    // 倒计时自然归零时自动结束，和用户点击“结束”走同一个完成逻辑。
                    completeSession(elapsedMillis)
                    break
                }
                delay(1_000L.milliseconds)
            }
        }
    }

    private fun emitRunningState(elapsedMillis: Long) {
        val snapshot = environment.value
        // ViewModel 负责把业务数据整理成 UI 能直接显示的状态，Screen 不再自己计算剩余时间。
        _uiState.value = SessionUiState.Running(
            sessionId = activeSessionId,
            elapsedMillis = elapsedMillis,
            remainingMillis = (sessionDurationMillis - elapsedMillis).coerceAtLeast(0L),
            noiseSamples = emptyList(),
            lightLevel = snapshot?.light?.level ?: LightLevel.COMFORTABLE,
            movementCount = if (snapshot?.motion?.isSignificantMove == true) 1 else 0
        )
    }

    private fun completeSession(elapsedMillis: Long) {
        val sessionId = activeSessionId
        if (sessionId == 0L) return
        viewModelScope.launch {
            // ViewModel 负责算出暂停后的净专注时长，Repository 负责保存这次会话结果。
            val session = focusRepository.finishSession(sessionId, elapsedMillis)
            _uiState.value = SessionUiState.Finished(session)
        }
    }

    private fun currentElapsedMillis(): Long {
        // 计时公式：暂停前累计时长 + 当前运行片段时长。
        // 这样暂停/继续不会重复计算，也不会丢失已经专注的时间。
        val runningExtra = if (runStartedAt == 0L) {
            0L
        } else {
            System.currentTimeMillis() - runStartedAt
        }
        return accumulatedMillis + runningExtra
    }
}
