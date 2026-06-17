package com.ywb.focusguard.service

import android.app.Service
import android.content.Intent
import android.os.IBinder

class FocusMonitorService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null
}
