package com.ywb.focusguard.service

import android.app.Service
import android.content.Intent
import android.os.IBinder

/**
 * 专注前台监测服务的占位入口。
 * 阶段 5 会在这里创建通知渠道、启动前台通知并协调后台传感器采集。
 */
class FocusMonitorService : Service() {
    /** 本项目使用 startService/startForegroundService 启动，不提供 Binder 客户端连接。 */
    override fun onBind(intent: Intent?): IBinder? = null
}
