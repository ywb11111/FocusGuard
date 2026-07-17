package com.ywb.focusguard.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * 接收通知栏按钮点击事件，并转发给 ViewModel。
 *
 * 由于 Service 无法直接更新 ViewModel 状态，这里使用 BroadcastReceiver 作为桥梁：
 * - 通知栏按钮点击 -> 发送广播 -> SessionActionReceiver -> 发送事件给 ViewModel
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
     * 收到广播时，发送本地事件通知 ViewModel。
     *
     * 当前简化实现：直接通过 SharedPreferences 或 EventBus 通知，
     * 实际项目中可使用 LiveData、SharedFlow 或 EventBus（如 LocalBroadcastManager）。
     */
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_TOGGLE_PAUSE -> {
                Log.d(TAG, "收到暂停/继续事件")
                // 通知 SessionViewModel 暂停或继续
                // 这里使用静态事件总线，ViewModel 订阅此事件
                SessionEventManager.notifyTogglePause()
            }
            ACTION_FINISH -> {
                Log.d(TAG, "收到结束事件")
                SessionEventManager.notifyFinish()
            }
        }
    }
}

/**
 * 简化的事件管理器，用于通知 ViewModel 响应通知栏操作。
 *
 * 注意：这是一个临时实现，生产环境建议使用更健壮的事件总线。
 */
object SessionEventManager {
    private var togglePauseCallback: (() -> Unit)? = null
    private var finishCallback: (() -> Unit)? = null

    fun registerCallbacks(
        onTogglePause: () -> Unit,
        onFinish: () -> Unit
    ) {
        togglePauseCallback = onTogglePause
        finishCallback = onFinish
    }

    fun unregisterCallbacks() {
        togglePauseCallback = null
        finishCallback = null
    }

    fun notifyTogglePause() {
        togglePauseCallback?.invoke()
    }

    fun notifyFinish() {
        finishCallback?.invoke()
    }
}