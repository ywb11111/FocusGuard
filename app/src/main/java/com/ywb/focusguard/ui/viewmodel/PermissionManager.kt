package com.ywb.focusguard.ui.viewmodel

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.ywb.focusguard.domain.model.PermissionState
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 权限状态管理器，负责检查和更新 App 关键权限的授权状态。
 *
 * 当前检查三类权限：
 * - RECORD_AUDIO：噪声检测需要
 * - POST_NOTIFICATIONS：前台服务通知需要（Android 13+）
 * - PACKAGE_USAGE_STATS：后续统计专注期间切 App 使用
 *
 * 设计为 Singleton，可在多个 ViewModel 间共享权限状态。
 */
@Singleton
class PermissionManager @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private val _permissionState = MutableStateFlow(PermissionState())

    /** 当前权限状态快照，UI 可收集此 Flow 响应变化。 */
    val permissionState: StateFlow<PermissionState> = _permissionState.asStateFlow()

    /**
     * 检查录音权限是否已授予。
     *
     * @return true 表示已授权，可以启动 AudioRecord。
     */
    fun isAudioPermissionGranted(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * 检查通知权限是否已授予（Android 13+ 需要）。
     */
    fun isNotificationPermissionGranted(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * 刷新所有权限状态，通常在 onResume 或权限回调后调用。
     *
     * 使用情况权限需要特殊处理（引导用户去设置），这里暂不检查。
     */
    fun refreshPermissionState() {
        _permissionState.update { current ->
            current.copy(
                audioGranted = isAudioPermissionGranted(),
                notificationGranted = isNotificationPermissionGranted()
            )
        }
    }

    /**
     * 标记录音权限已授权。
     */
    fun markAudioGranted() {
        _permissionState.update { it.copy(audioGranted = true) }
    }

    /**
     * 标记通知权限已授权。
     */
    fun markNotificationGranted() {
        _permissionState.update { it.copy(notificationGranted = true) }
    }
}