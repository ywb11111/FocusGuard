package com.ywb.focusguard.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ywb.focusguard.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * App 壳层状态，只负责首次引导门禁。
 *
 * 业务页面仍由各自 ViewModel 管理，避免把整个应用状态塞进根 ViewModel。
 */
@HiltViewModel
class AppViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {
    /** null 表示 DataStore 尚未返回首个值，防止启动瞬间闪过首页。 */
    val onboardingCompleted: StateFlow<Boolean?> = settingsRepository.settings
        .map { it.onboardingCompleted }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** 用户完成或跳过引导后持久化，后续启动直接进入主界面。 */
    fun completeOnboarding() {
        viewModelScope.launch { settingsRepository.completeOnboarding() }
    }
}
