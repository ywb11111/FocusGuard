package com.ywb.focusguard.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.ywb.focusguard.ui.navigation.Destination
import com.ywb.focusguard.ui.navigation.FocusGuardNavHost
import com.ywb.focusguard.ui.navigation.topLevelDestinations
import com.ywb.focusguard.ui.screen.OnboardingScreen
import com.ywb.focusguard.ui.viewmodel.AppViewModel

/**
 * App 的 Compose 根组件，统一持有 NavController、Scaffold 和底部导航栏。
 * 具体页面内容交给 [FocusGuardNavHost]，避免每个 Screen 自己管理顶层导航。
 */
@Composable
fun FocusGuardApp(viewModel: AppViewModel = hiltViewModel()) {
    val onboardingCompleted by viewModel.onboardingCompleted.collectAsStateWithLifecycle()
    if (onboardingCompleted == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }
    if (onboardingCompleted == false) {
        OnboardingScreen(onComplete = viewModel::completeOnboarding)
        return
    }

    // NavController 是 Compose Navigation 的核心对象，负责页面跳转和返回栈管理。
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    // 当前 route 决定底部导航栏哪一项处于选中状态。
    val currentRoute = backStackEntry?.destination?.route
    // 所有顶层入口复用同一导航策略，保证底部 Tab 与页面内入口行为一致。
    val navigateToTopLevel: (Destination) -> Unit = { destination ->
        navController.navigate(destination.route) {
            // 切换顶层页面时复用同一套导航规则，避免 Today 的按钮和底部 Tab 行为不一致。
            popUpTo(navController.graph.startDestinationId) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    val showBottomBar = topLevelDestinations.any { it.destination.route == currentRoute }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (showBottomBar) FocusGuardBottomBar(currentRoute, navigateToTopLevel)
        }
    ) { innerPadding ->
        FocusGuardNavHost(
            navController = navController,
            // 专注是一次沉浸任务而不是长期 Tab；进入后隐藏底部导航。
            onStartFocus = {
                navController.navigate(Destination.Session.route) {
                    launchSingleTop = true
                }
            },
            modifier = Modifier.padding(innerPadding)
        )
    }
}

/** 顶层三项导航；截图预览与真实 App 复用同一实现，避免预览和产品漂移。 */
@Composable
fun FocusGuardBottomBar(
    currentRoute: String?,
    onNavigate: (Destination) -> Unit
) {
    NavigationBar(containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 0.dp) {
        topLevelDestinations.forEach { item ->
            NavigationBarItem(
                selected = currentRoute == item.destination.route,
                onClick = { onNavigate(item.destination) },
                icon = { Icon(item.icon, contentDescription = item.label) },
                label = { Text(item.label) }
            )
        }
    }
}
