package com.ywb.focusguard

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

// Hilt 的全局入口。没有这个注解，@AndroidEntryPoint、@HiltViewModel、@Inject 等注入链路不会生效。
@HiltAndroidApp
class FocusGuardApplication : Application()
