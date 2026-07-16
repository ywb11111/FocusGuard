package com.ywb.focusguard.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ywb.focusguard.data.repository.SettingsRepository
import com.ywb.focusguard.ui.state.SettingsUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/** 把 SettingsRepository 的设置流转换为设置页 UiState。 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    /** 当前为内存实现，后续替换 DataStore 时 ViewModel 接口无需变化。 */
    settingsRepository: SettingsRepository
) : ViewModel() {
    // 当前设置仓库还是内存实现；后续接 DataStore 后，这里的 UI 收集方式不用变。
    /** 设置页的生命周期感知状态。 */
    val uiState = settingsRepository.settings
        .map { settings -> SettingsUiState(settings = settings) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = SettingsUiState()
        )
}
