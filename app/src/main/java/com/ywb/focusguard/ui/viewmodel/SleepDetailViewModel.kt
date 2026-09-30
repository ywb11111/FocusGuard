package com.ywb.focusguard.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ywb.focusguard.data.repository.SleepRepository
import com.ywb.focusguard.ui.navigation.Destination
import com.ywb.focusguard.ui.state.SleepDetailUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/** 根据导航参数 sleepId 观察一晚睡眠详情，写法与 SessionDetailViewModel 保持一致。 */
@HiltViewModel
class SleepDetailViewModel @Inject constructor(
    /** Navigation 把 route 参数放进 SavedStateHandle。 */
    savedStateHandle: SavedStateHandle,
    /** 提供主记录 + 分钟数据 + 分析结果。 */
    sleepRepository: SleepRepository
) : ViewModel() {
    private val sleepId: Long = savedStateHandle[Destination.SleepDetail.ARG_SLEEP_ID] ?: 0L

    /** Loading 起步，随后发射 Content 或 Empty。 */
    val uiState: StateFlow<SleepDetailUiState> = sleepRepository
        .observeSleepDetail(sleepId)
        .map { detail -> detail?.let { SleepDetailUiState.Content(it) } ?: SleepDetailUiState.Empty }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = SleepDetailUiState.Loading
        )
}
