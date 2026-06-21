package com.ywb.focusguard.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.ywb.focusguard.ui.navigation.FocusGuardNavHost
import com.ywb.focusguard.ui.navigation.topLevelDestinations

@Composable
fun FocusGuardApp() {
    // NavController 是 Compose Navigation 的核心对象，负责页面跳转和返回栈管理。
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            NavigationBar {
                topLevelDestinations.forEach { item ->
                    NavigationBarItem(
                        selected = currentRoute == item.destination.route,
                        onClick = {
                            navController.navigate(item.destination.route) {
                                // 切换底部 Tab 时保留各 Tab 的页面状态，避免每次点击都重新创建整条导航栈。
                                popUpTo(navController.graph.startDestinationId) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.label
                            )
                        },
                        label = { Text(item.label) }
                    )
                }
            }
        }
    ) { innerPadding ->
        FocusGuardNavHost(
            navController = navController,
            modifier = Modifier.padding(innerPadding)
        )
    }
}
