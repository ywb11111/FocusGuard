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
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = TodayUiState()
    )
}
