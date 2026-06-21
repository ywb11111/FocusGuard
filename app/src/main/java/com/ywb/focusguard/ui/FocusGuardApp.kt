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
import com.ywb.focusguard.ui.navigation.Destination
import com.ywb.focusguard.ui.navigation.FocusGuardNavHost
import com.ywb.focusguard.ui.navigation.topLevelDestinations

@Composable
fun FocusGuardApp() {
    // NavController 是 Compose Navigation 的核心对象，负责页面跳转和返回栈管理。
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
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

    Scaffold(
        bottomBar = {
            NavigationBar {
                topLevelDestinations.forEach { item ->
                    NavigationBarItem(
                        selected = currentRoute == item.destination.route,
                        onClick = { navigateToTopLevel(item.destination) },
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
            onStartFocus = { navigateToTopLevel(Destination.Session) },
            modifier = Modifier.padding(innerPadding)
        )
    }
}
