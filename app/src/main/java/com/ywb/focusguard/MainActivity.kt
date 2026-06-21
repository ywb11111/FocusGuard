package com.ywb.focusguard

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dagger.hilt.android.AndroidEntryPoint
import com.ywb.focusguard.ui.FocusGuardApp
import com.ywb.focusguard.ui.theme.FocusGuardTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
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
