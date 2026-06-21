package com.ywb.focusguard.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ywb.focusguard.data.repository.EnvironmentRepository
import com.ywb.focusguard.data.repository.FocusRepository
import com.ywb.focusguard.ui.state.TodayUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class TodayViewModel @Inject constructor(
    environmentRepository: EnvironmentRepository,
    focusRepository: FocusRepository
) : ViewModel() {
    // combine 用来把多个数据源合成一个页面状态：环境快照 + 今日统计 + 最近记录。
    // 这样 Screen 只需要收集一个 uiState，而不是同时订阅好几个 Flow。
    val uiState = combine(
        environmentRepository.observeEnvironmentSnapshot(),
        focusRepository.observeTodaySummary(),
        focusRepository.observeSessions()
    ) { environment, summary, sessions ->
        TodayUiState(
            isLoading = false,
            environment = environment,
            todaySummary = summary,
            latestSession = sessions.firstOrNull()
        )
    }.stateIn(
        scope = viewModelScope,
        // 页面停止观察 5 秒后再停止上游 Flow，避免短暂切页导致频繁重启数据流。
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = TodayUiState()
    )
}
