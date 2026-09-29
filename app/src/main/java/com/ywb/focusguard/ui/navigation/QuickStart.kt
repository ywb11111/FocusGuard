package com.ywb.focusguard.ui.navigation

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.ywb.focusguard.MainActivity

private const val QUICK_START_SCHEME = "focusguard"
private const val QUICK_START_HOST = "session"
private const val DURATION_QUERY = "duration"

/** 桌面入口交给根组件消费的单次导航请求。 */
data class QuickStartRequest(
    val durationMinutes: Int,
    val requestId: Long
)

/** 创建显式 Intent，避免桌面小组件把内部协议交给其他应用处理。 */
fun createQuickStartIntent(context: Context, durationMinutes: Int): Intent =
    Intent(context, MainActivity::class.java).apply {
        action = Intent.ACTION_VIEW
        data = quickStartUri(durationMinutes)
        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
    }

/** 只接受 FocusGuard 自己定义且时长合法的快速入口。 */
fun Intent.quickStartDurationMinutes(): Int? {
    val uri = data ?: return null
    if (uri.scheme != QUICK_START_SCHEME || uri.host != QUICK_START_HOST) return null
    return uri.getQueryParameter(DURATION_QUERY)
        ?.toIntOrNull()
        ?.takeIf { it in 1..180 }
}

private fun quickStartUri(durationMinutes: Int): Uri = Uri.Builder()
    .scheme(QUICK_START_SCHEME)
    .authority(QUICK_START_HOST)
    .appendQueryParameter(DURATION_QUERY, durationMinutes.coerceIn(1, 180).toString())
    .build()
