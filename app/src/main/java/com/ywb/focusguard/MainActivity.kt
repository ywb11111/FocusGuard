package com.ywb.focusguard

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dagger.hilt.android.AndroidEntryPoint
import com.ywb.focusguard.ui.FocusGuardApp
import com.ywb.focusguard.ui.theme.FocusGuardTheme

/** App 唯一 Activity，负责提供 Compose 宿主和 Hilt 的 Activity 注入入口。 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    /** Activity 创建时启用沉浸式边缘布局并挂载 Compose 根组件。 */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            // Activity 只负责挂载 Compose 根组件；业务状态交给 ViewModel，依赖创建交给 Hilt。
            FocusGuardTheme {
                FocusGuardApp()
            }
        }
    }
}
