package com.ywb.focusguard.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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

private const val TOP_LEVEL_ENTER_DURATION_MILLIS = 160
private const val TOP_LEVEL_EXIT_DURATION_MILLIS = 120
private const val DETAIL_TRANSITION_DURATION_MILLIS = 240

/** 只有底部导航之间切换时使用轻量淡入淡出，沉浸任务和详情页保留方向感。 */
internal fun isTopLevelTransition(initialRoute: String?, targetRoute: String?): Boolean {
    val topLevelRoutes = topLevelDestinations.mapTo(mutableSetOf()) { it.destination.route }
    return initialRoute in topLevelRoutes && targetRoute in topLevelRoutes
}

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
        modifier = modifier,
        enterTransition = {
            if (isTopLevelTransition(initialState.destination.route, targetState.destination.route)) {
                topLevelEnterTransition()
            } else {
                detailEnterTransition(AnimatedContentTransitionScope.SlideDirection.Left)
            }
        },
        exitTransition = {
            if (isTopLevelTransition(initialState.destination.route, targetState.destination.route)) {
                topLevelExitTransition()
            } else {
                detailExitTransition(AnimatedContentTransitionScope.SlideDirection.Left)
            }
        },
        popEnterTransition = {
            if (isTopLevelTransition(initialState.destination.route, targetState.destination.route)) {
                topLevelEnterTransition()
            } else {
                detailEnterTransition(AnimatedContentTransitionScope.SlideDirection.Right)
            }
        },
        popExitTransition = {
            if (isTopLevelTransition(initialState.destination.route, targetState.destination.route)) {
                topLevelExitTransition()
            } else {
                detailExitTransition(AnimatedContentTransitionScope.SlideDirection.Right)
            }
        }
    ) {
        composable(
            route = Destination.Today.route
        ) {
            TodayRoute(
                onStartFocus = onStartFocus,
                onOpenSettings = { navController.navigate(Destination.Settings.route) },
                onOpenSessionDetail = { id ->
                    navController.navigate(Destination.SessionDetail.createRoute(id))
                }
            )
        }
        composable(
            route = Destination.Session.route
        ) {
            SessionRoute(
                onFinish = { id -> navController.navigate(Destination.SessionDetail.createRoute(id)) }
            )
        }
        composable(
            route = Destination.SessionQuickStart.route,
            arguments = listOf(navArgument(Destination.SessionQuickStart.ARG_DURATION_MINUTES) {
                type = NavType.IntType
            })
        ) { entry ->
            SessionRoute(
                initialDurationMinutes = entry.arguments
                    ?.getInt(Destination.SessionQuickStart.ARG_DURATION_MINUTES),
                onFinish = { id -> navController.navigate(Destination.SessionDetail.createRoute(id)) }
            )
        }
        composable(
            route = Destination.Reports.route
        ) {
            ReportsRoute(
                onOpenSessionDetail = { id ->
                    navController.navigate(Destination.SessionDetail.createRoute(id))
                }
            )
        }
        composable(
            route = Destination.Settings.route
        ) {
            SettingsRoute(
                onOpenPermissionGuide = { navController.navigate(Destination.PermissionGuide.route) }
            )
        }
        composable(Destination.PermissionGuide.route) {
            PermissionGuideScreen(onBack = { navController.popBackStack() })
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
            SessionDetailRoute(onBack = { navController.popBackStack() })
        }
    }
}

private fun topLevelEnterTransition(): EnterTransition =
    fadeIn(animationSpec = tween(TOP_LEVEL_ENTER_DURATION_MILLIS))

private fun topLevelExitTransition(): ExitTransition =
    fadeOut(animationSpec = tween(TOP_LEVEL_EXIT_DURATION_MILLIS))

private fun AnimatedContentTransitionScope<*>.detailEnterTransition(
    direction: AnimatedContentTransitionScope.SlideDirection
): EnterTransition = slideIntoContainer(
    direction,
    animationSpec = tween(DETAIL_TRANSITION_DURATION_MILLIS)
) + fadeIn(animationSpec = tween(TOP_LEVEL_ENTER_DURATION_MILLIS))

private fun AnimatedContentTransitionScope<*>.detailExitTransition(
    direction: AnimatedContentTransitionScope.SlideDirection
): ExitTransition = slideOutOfContainer(
    direction,
    animationSpec = tween(DETAIL_TRANSITION_DURATION_MILLIS)
) + fadeOut(animationSpec = tween(TOP_LEVEL_EXIT_DURATION_MILLIS))
