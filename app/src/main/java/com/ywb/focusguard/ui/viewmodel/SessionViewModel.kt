package com.ywb.focusguard.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ywb.focusguard.data.repository.EnvironmentRepository
import com.ywb.focusguard.data.repository.FocusRepository
import com.ywb.focusguard.domain.model.FocusConfig
import com.ywb.focusguard.domain.model.LightLevel
import com.ywb.focusguard.ui.state.SessionUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SessionViewModel @Inject constructor(
    private val focusRepository: FocusRepository,
    environmentRepository: EnvironmentRepository
) : ViewModel() {
    private val defaultConfig = FocusConfig(durationMinutes = 25)

    val uiState: StateFlow<SessionUiState> = environmentRepository.observeEnvironmentSnapshot()
        .map { environment ->
            SessionUiState.Ready(
                config = defaultConfig,
                environment = environment
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = SessionUiState.Ready(defaultConfig, null)
        )

    fun finishDemoSession() {
        viewModelScope.launch {
            val id = focusRepository.startSession(defaultConfig)
            focusRepository.finishSession(id)
        }
    }

    fun demoRunningState(): SessionUiState = SessionUiState.Running(
        sessionId = 1,
        elapsedMillis = 8 * 60 * 1000L,
        remainingMillis = 17 * 60 * 1000L,
        noiseSamples = emptyList(),
        lightLevel = LightLevel.COMFORTABLE,
        movementCount = 1
    )
}
