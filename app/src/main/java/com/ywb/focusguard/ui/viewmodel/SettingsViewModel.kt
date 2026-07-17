package com.ywb.focusguard.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ywb.focusguard.data.repository.SettingsRepository
import com.ywb.focusguard.ui.state.SettingsUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/** 把 SettingsRepository 的设置流转换为设置页 UiState。 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    /** 当前为内存实现，后续替换 DataStore 时 ViewModel 接口无需变化。 */
    settingsRepository: SettingsRepository,
    /** 提供权限状态检查。 */
    private val permissionManager: PermissionManager
) : ViewModel() {
    // 当前设置仓库还是内存实现；后续接 DataStore 后，这里的 UI 收集方式不用变。
    /** 设置页的生命周期感知状态。 */
    val uiState = combine(
        settingsRepository.settings,
        permissionManager.permissionState
    ) { settings, permissions ->
        SettingsUiState(
            settings = settings,
            permissionState = permissions
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SettingsUiState()
    )

    /** 在页面恢复时刷新权限状态。 */
    fun refreshPermissions() {
        permissionManager.refreshPermissionState()
    }
}
