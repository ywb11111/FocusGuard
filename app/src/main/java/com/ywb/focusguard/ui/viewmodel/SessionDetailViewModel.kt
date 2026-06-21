package com.ywb.focusguard.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ywb.focusguard.data.repository.FocusRepository
import com.ywb.focusguard.ui.navigation.Destination
import com.ywb.focusguard.ui.state.SessionDetailUiState
import com.ywb.focusguard.ui.state.toUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class SessionDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    focusRepository: FocusRepository
) : ViewModel() {
    // Navigation Compose 会把 route 参数放入 SavedStateHandle。
    // ViewModel 读取参数后再请求 Repository，页面不用知道参数从哪里来。
    private val sessionId: Long = savedStateHandle[Destination.SessionDetail.ARG_SESSION_ID] ?: 0L

    val uiState: StateFlow<SessionDetailUiState> = focusRepository
        .observeSessionDetail(sessionId)
        .map { detail ->
            detail?.toUiState() ?: SessionDetailUiState.Empty()
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = SessionDetailUiState.Loading
        )
}
