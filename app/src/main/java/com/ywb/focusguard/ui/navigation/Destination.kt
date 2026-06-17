package com.ywb.focusguard.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Destination(val route: String) {
    data object Today : Destination("today")
    data object Session : Destination("session")
    data object Reports : Destination("reports")
    data object Settings : Destination("settings")
    data object PermissionGuide : Destination("permission_guide")
    data object Onboarding : Destination("onboarding")
    data object SessionDetail : Destination("session/{sessionId}") {
        const val ARG_SESSION_ID = "sessionId"
        fun createRoute(sessionId: Long) = "session/$sessionId"
    }
}

data class TopLevelDestination(
    val destination: Destination,
    val label: String,
    val icon: ImageVector
)

val topLevelDestinations = listOf(
    TopLevelDestination(Destination.Today, "今日", Icons.Outlined.Home),
    TopLevelDestination(Destination.Session, "专注", Icons.Outlined.PlayCircle),
    TopLevelDestination(Destination.Reports, "报告", Icons.Outlined.Analytics),
    TopLevelDestination(Destination.Settings, "设置", Icons.Outlined.Settings)
)
