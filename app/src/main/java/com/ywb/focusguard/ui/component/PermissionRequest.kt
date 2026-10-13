package com.ywb.focusguard.ui.component

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

/**
 * 开始专注 / 睡眠监测前需要的运行时权限：麦克风（噪声、鼾声）和通知（Android 13+ 前台服务通知）。
 */
fun monitoringPermissions(): List<String> = buildList {
    add(Manifest.permission.RECORD_AUDIO)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) add(Manifest.permission.POST_NOTIFICATIONS)
}

/**
 * "用到时再申请"（just-in-time）：返回一个函数，调用时先补齐缺失的权限，再执行 [action]。
 *
 * **为什么在点击"开始"时申请，而不是一打开 App 就弹**：
 * 用户在有明确目的的那一刻看到权限弹窗，最容易理解"为什么需要"，同意率也最高；
 * 一启动就连弹几个窗口，用户往往直接全部拒绝。这也是 Google 官方推荐的权限申请时机。
 *
 * **无论授权结果如何都会执行 [action]**：拒绝只是降级（没有噪声数据 / 没有通知），不能挡住核心功能。
 * 用户之前拒绝过两次时系统会直接返回"拒绝"、不再弹窗，此时同样直接执行。
 *
 * @param permissions 需要的权限列表，已授予的会被跳过。
 * @param action 权限流程结束后要执行的操作。
 */
@Composable
fun rememberRunAfterPermissions(
    permissions: List<String>,
    action: () -> Unit
): () -> Unit {
    val context = LocalContext.current
    // rememberUpdatedState：回调里总是拿到最新的 action，又不需要因为 action 变化重新注册 launcher
    val latestAction by rememberUpdatedState(action)
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { latestAction() }
    return remember(permissions, launcher) {
        {
            val missing = permissions.filter {
                ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
            }
            if (missing.isEmpty()) latestAction() else launcher.launch(missing.toTypedArray())
        }
    }
}
