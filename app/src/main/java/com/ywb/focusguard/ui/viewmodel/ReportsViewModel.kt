package com.ywb.focusguard.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ywb.focusguard.data.repository.FocusRepository
import com.ywb.focusguard.ui.state.ReportsUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class ReportsViewModel @Inject constructor(
    focusRepository: FocusRepository
) : ViewModel() {
    val uiState = combine(
        focusRepository.observeSessions(),
        focusRepository.observeTodaySummary()
    ) { sessions, summary ->
        ReportsUiState(sessions = sessions, summary = summary)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ReportsUiState()
    )
}
