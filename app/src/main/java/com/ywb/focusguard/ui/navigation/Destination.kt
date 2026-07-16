package com.ywb.focusguard.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.graphics.vector.ImageVector

/** 所有页面路由的集中定义，避免 route 字符串散落在 Screen 中。 */
sealed class Destination(val route: String) {
    /** 今日概览顶层页面。 */
    data object Today : Destination("today")

    /** 专注计时顶层页面。 */
    data object Session : Destination("session")

    /** 历史报告顶层页面。 */
    data object Reports : Destination("reports")

    /** 设置顶层页面。 */
    data object Settings : Destination("settings")

    /** 权限用途说明页面。 */
    data object PermissionGuide : Destination("permission_guide")

    /** 首次使用引导页面，目前尚未接入启动判断。 */
    data object Onboarding : Destination("onboarding")

    /** 带 sessionId 参数的专注详情页面。 */
    data object SessionDetail : Destination("session_detail/{sessionId}") {
        /** Navigation 参数键，必须与 route 占位符和 SavedStateHandle 读取键一致。 */
        const val ARG_SESSION_ID = "sessionId"

        // 带参数页面不要手写 "session_detail/$id"，统一通过函数生成，后续改路由格式时更安全。
        fun createRoute(sessionId: Long) = "session_detail/$sessionId"
    }
}

/**
 * 底部导航项的展示配置。
 *
 * @property destination 点击后进入的顶层页面。
 * @property label 底部导航显示的中文标签。
 * @property icon 与页面语义对应的 Material 图标。
 */
data class TopLevelDestination(
    val destination: Destination,
    val label: String,
    val icon: ImageVector
)

/** App 固定显示的四个顶层导航入口。 */
val topLevelDestinations = listOf(
    TopLevelDestination(Destination.Today, "今日", Icons.Outlined.Home),
    TopLevelDestination(Destination.Session, "专注", Icons.Outlined.PlayCircle),
    TopLevelDestination(Destination.Reports, "报告", Icons.Outlined.Analytics),
    TopLevelDestination(Destination.Settings, "设置", Icons.Outlined.Settings)
)
