package com.ywb.focusguard.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * 接收通知栏按钮点击事件，并通过 SharedFlow 转发给订阅者。
 *
 * **通信链路**：
 * 通知栏按钮点击 → BroadcastReceiver → SessionStateHolder.sendXxx() → SharedFlow → ViewModel 订阅
 *
 * **优点**：
 * - 使用 Flow 替代静态回调，避免内存泄漏
 * - 支持多订阅者，未来可扩展（如 Wear OS 控制专注）
 */
class SessionActionReceiver : BroadcastReceiver() {

    companion object {
        const val TAG = "SessionActionReceiver"

        /** Action：暂停/继续专注 */
        const val ACTION_TOGGLE_PAUSE = "com.ywb.focusguard.ACTION_TOGGLE_PAUSE"

        /** Action：结束专注 */
        const val ACTION_FINISH = "com.ywb.focusguard.ACTION_FINISH"

        /**
         * 创建暂停/继续的广播 Intent。
         */
        fun createTogglePauseIntent(context: Context): Intent =
            Intent(context, SessionActionReceiver::class.java).apply {
                action = ACTION_TOGGLE_PAUSE
            }

        /**
         * 创建结束的广播 Intent。
         */
        fun createFinishIntent(context: Context): Intent =
            Intent(context, SessionActionReceiver::class.java).apply {
                action = ACTION_FINISH
            }
    }

    /**
     * 收到广播时，通过 SessionStateHolder 发送事件。
     *
     * ViewModel 通过 collect SessionStateHolder.controlEvents 来响应。
     */
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_TOGGLE_PAUSE -> {
                Log.d(TAG, "收到暂停/继续事件")
                SessionStateHolder.sendTogglePause()
            }
            ACTION_FINISH -> {
                Log.d(TAG, "收到结束事件")
                SessionStateHolder.sendFinish()
            }
        }
    }
}