package com.ywb.focusguard.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ywb.focusguard.data.repository.FocusRepository
import com.ywb.focusguard.domain.model.FocusSession
import com.ywb.focusguard.ui.state.PeriodSummary
import com.ywb.focusguard.ui.state.ReportPeriod
import com.ywb.focusguard.ui.state.ReportsUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.util.Calendar
import javax.inject.Inject

/**
 * 报告页 ViewModel：支持周/月切换，从 Room 按日期范围查询并聚合统计。
 *
 * 使用 flatMapLatest 切换周期时自动取消旧查询、启动新查询。
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ReportsViewModel @Inject constructor(
    private val focusRepository: FocusRepository
) : ViewModel() {

    /** 当前选中的报告周期。 */
    private val _period = MutableStateFlow(ReportPeriod.WEEK)

    /** 报告页完整状态。 */
    val uiState: StateFlow<ReportsUiState> = _period.flatMapLatest { period ->
        val (start, end) = periodRange(period)
        focusRepository.observeSessionsInRange(start, end).map { sessions ->
            buildUiState(period, sessions)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ReportsUiState()
    )

    /** 切换周/月周期。 */
    fun switchPeriod(period: ReportPeriod) {
        _period.value = period
    }

    /** 根据周期类型计算起止时间戳。 */
    private fun periodRange(period: ReportPeriod): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        val end = cal.apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.timeInMillis + 1

        val start = when (period) {
            ReportPeriod.WEEK -> {
                cal.apply {
                    add(Calendar.DAY_OF_YEAR, -6)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
            }
            ReportPeriod.MONTH -> {
                cal.apply {
                    add(Calendar.DAY_OF_YEAR, -29)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
            }
        }
        return Pair(start, end)
    }

    /** 从会话列表构建完整的报告页状态。 */
    private fun buildUiState(period: ReportPeriod, sessions: List<FocusSession>): ReportsUiState {
        val totalFocus = sessions.sumOf { it.durationMillis }
        val avgScore = sessions.map { it.score }.average().takeIf { !it.isNaN() }?.toInt() ?: 0
        val avgNoise = sessions.map { it.averageNoiseDb }.average().takeIf { !it.isNaN() }?.toFloat() ?: 0f
        val avgLight = sessions.map { it.averageLightLux }.average().takeIf { !it.isNaN() }?.toFloat() ?: 0f
        val totalMovement = sessions.sumOf { it.movementCount }

        return ReportsUiState(
            period = period,
            sessions = sessions,
            periodSummary = PeriodSummary(
                totalFocusMillis = totalFocus,
                averageScore = avgScore,
                sessionCount = sessions.size,
                averageNoiseDb = avgNoise,
                averageLightLux = avgLight,
                totalMovementCount = totalMovement
            ),
            trendValues = sessions.reversed().map { it.score.toFloat() }
        )
    }
}
