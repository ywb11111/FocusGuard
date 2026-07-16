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
import com.ywb.focusguard.ui.screen.SessionDetailRoute
import com.ywb.focusguard.ui.screen.SessionRoute
import com.ywb.focusguard.ui.screen.SettingsRoute
import com.ywb.focusguard.ui.screen.TodayRoute

/**
 * FocusGuard 的导航图，负责把 route 映射到 Route Composable，并集中处理页面跳转。
 *
 * @param navController 根组件创建的导航控制器。
 * @param onStartFocus 进入专注顶层页面的统一回调，复用底部导航的返回栈策略。
 * @param modifier Scaffold 提供的内容边距等外层修饰符。
 */
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
                // 明确参数类型后，Navigation 会把 id 以 Long 放入 SavedStateHandle。
                type = NavType.LongType
            })
        ) {
            SessionDetailRoute()
        }
    }
}
