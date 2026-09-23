package com.ywb.focusguard.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ywb.focusguard.data.repository.SettingsRepository
import com.ywb.focusguard.service.DailyReportScheduler
import com.ywb.focusguard.ui.state.SettingsUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** 把 SettingsRepository 的设置流转换为设置页 UiState。 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    /** 设置仓库，提供读写能力。内存实现阶段后续替换为 DataStore 时接口无需变化。 */
    private val settingsRepository: SettingsRepository,
    /** 提供权限状态检查。 */
    private val permissionManager: PermissionManager,
    /** 把“每日总结”设置转换为唯一的 WorkManager 周期任务。 */
    private val dailyReportScheduler: DailyReportScheduler
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

    /** 更新默认专注时长，由 SettingsScreen 点击调用。 */
    fun updateDefaultFocusMinutes(minutes: Int) {
        viewModelScope.launch {
            settingsRepository.updateDefaultFocusMinutes(minutes)
        }
    }

    /** 更新噪声阈值，由 SettingsScreen 点击调用。 */
    fun updateNoiseThreshold(value: Float) {
        viewModelScope.launch {
            settingsRepository.updateNoiseThreshold(value)
        }
    }

    /** 更新后台监测开关并持久化。 */
    fun updateBackgroundMonitoringEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateBackgroundMonitoringEnabled(enabled)
        }
    }

    /** 更新每日总结开关并持久化。 */
    fun updateDailyReportEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateDailyReportEnabled(enabled)
            dailyReportScheduler.setEnabled(enabled)
        }
    }
}
