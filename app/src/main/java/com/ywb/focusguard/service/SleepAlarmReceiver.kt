package com.ywb.focusguard.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * AlarmManager 闹钟触发时的入口：只负责把响铃交给 AlarmRingService。
 *
 * 为什么不在这里直接播放铃声：BroadcastReceiver.onReceive 只有约 10 秒执行时间，结束后进程随时可能被回收，
 * 铃声会戛然而止。响铃要持续到用户关掉，必须放在前台服务里。
 * 精确闹钟触发后系统会给 App 一个短暂的"允许启动前台服务"豁免，所以这里可以从后台启动响铃服务。
 */
class SleepAlarmReceiver : BroadcastReceiver() {

    companion object {
        /** Intent Extra：是否为智能提前唤醒。 */
        const val EXTRA_SMART_EARLY = "smart_early"
    }

    override fun onReceive(context: Context, intent: Intent) {
        AlarmRingService.ring(context, smartEarly = intent.getBooleanExtra(EXTRA_SMART_EARLY, false))
    }
}
