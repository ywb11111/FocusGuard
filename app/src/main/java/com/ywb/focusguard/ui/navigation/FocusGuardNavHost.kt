package com.ywb.focusguard.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.ywb.focusguard.ui.screen.OnboardingScreen
import com.ywb.focusguard.ui.screen.PermissionGuideScreen
import com.ywb.focusguard.ui.screen.ReportsRoute
import com.ywb.focusguard.ui.screen.SessionDetailScreen
import com.ywb.focusguard.ui.screen.SessionRoute
import com.ywb.focusguard.ui.screen.SettingsRoute
import com.ywb.focusguard.ui.screen.TodayRoute

@Composable
fun FocusGuardNavHost(
    navController: NavHostController,
    onStartFocus: () -> Unit,
    modifier: Modifier = Modifier
) {
    // NavHost 是 route 和 Composable 页面之间的映射表：导航到某个 route 时，就显示对应页面。
    NavHost(
        navController = navController,
        startDestination = Destination.Today.route,
        modifier = modifier
    ) {
        composable(Destination.Today.route) {
            TodayRoute(
                onStartFocus = onStartFocus,
                onOpenSettings = { navController.navigate(Destination.Settings.route) },
                onOpenSessionDetail = { id ->
                    navController.navigate(Destination.SessionDetail.createRoute(id))
                }
            )
        }
        composable(Destination.Session.route) {
            SessionRoute(
                onFinish = { id -> navController.navigate(Destination.SessionDetail.createRoute(id)) }
            )
        }
        composable(Destination.Reports.route) {
            ReportsRoute(
                onOpenSessionDetail = { id ->
                    navController.navigate(Destination.SessionDetail.createRoute(id))
                }
            )
        }
        composable(Destination.Settings.route) {
            SettingsRoute(
                onOpenPermissionGuide = { navController.navigate(Destination.PermissionGuide.route) }
            )
        }
        composable(Destination.PermissionGuide.route) {
            PermissionGuideScreen()
        }
        composable(Destination.Onboarding.route) {
            OnboardingScreen()
        }
        composable(
            route = Destination.SessionDetail.route,
            arguments = listOf(navArgument(Destination.SessionDetail.ARG_SESSION_ID) {
                type = NavType.LongType
            })
        ) { backStackEntry ->
            // Navigation 传参最终都会从 backStackEntry 取出；这里拿到 sessionId 后交给详情页显示。
            val sessionId = backStackEntry.arguments?.getLong(Destination.SessionDetail.ARG_SESSION_ID) ?: 0L
            SessionDetailScreen(sessionId = sessionId)
        }
    }
}
