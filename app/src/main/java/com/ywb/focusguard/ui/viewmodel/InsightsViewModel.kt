package com.ywb.focusguard.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ywb.focusguard.data.repository.FocusRepository
import com.ywb.focusguard.data.repository.SleepRepository
import com.ywb.focusguard.domain.analyzer.InsightAnalyzer
import com.ywb.focusguard.domain.model.Insight
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * 报告页"睡眠与专注"洞察的 ViewModel。
 *
 * 为什么不并入 ReportsViewModel：报告页的周/月切换只影响专注统计，而洞察固定看最近 28 天
 * （样本太少的结论不可靠）；拆成独立 ViewModel 后两套时间口径互不干扰，也不用改动已有的报告逻辑。
 */
@HiltViewModel
class InsightsViewModel @Inject constructor(
    focusRepository: FocusRepository,
    sleepRepository: SleepRepository,
    insightAnalyzer: InsightAnalyzer
) : ViewModel() {

    /** 统计范围的起点：28 天前。在 ViewModel 创建时确定，页面重新进入时会重新创建。 */
    private val rangeStart = System.currentTimeMillis() - LOOKBACK_MILLIS

    /** 任一侧数据变化（新完成一次专注或一晚睡眠）都会重新计算。 */
    val insights: StateFlow<List<Insight>> = combine(
        focusRepository.observeSessionsInRange(rangeStart, Long.MAX_VALUE),
        sleepRepository.observeSessions()
    ) { focusSessions, sleepSessions ->
        insightAnalyzer.analyze(
            focusSessions = focusSessions,
            sleepSessions = sleepSessions.filter { it.startTime >= rangeStart },
            nowMillis = System.currentTimeMillis()
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    private companion object {
        const val LOOKBACK_MILLIS = 28 * 24 * 60 * 60 * 1000L
    }
}
