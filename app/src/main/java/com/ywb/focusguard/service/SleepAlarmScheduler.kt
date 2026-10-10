package com.ywb.focusguard.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.ywb.focusguard.MainActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "SleepAlarmScheduler"

/**
 * 闹钟调度：把"几点叫醒"注册到系统 AlarmManager。
 *
 * **为什么闹钟一定要交给 AlarmManager，而不是在 SleepMonitorService 里自己计时**：
 * 睡眠服务半夜可能被系统杀掉，如果闹钟只存在于服务的协程里，服务一死闹钟就没了——这是闹钟类 App 的大忌。
 * AlarmManager 的闹钟由系统保管，进程死了照样准时触发（只有重启手机、强行停止、覆盖安装会清掉）。
 *
 * **为什么用 setAlarmClock**：
 * 它是精度最高、优先级最高的闹钟，不受 Doze 省电模式影响，状态栏会显示闹钟图标；
 * 触发后 App 获得启动前台服务的豁免，响铃服务才能从后台启动。
 *
 * **智能提前**：SleepMonitorService 检测到浅睡时，调用 [schedule] 把同一个闹钟改到"现在"，
 * 响铃路径与准点闹钟完全相同，只需维护一条代码路径。
 */
@Singleton
class SleepAlarmScheduler @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    /**
     * 是否允许设置精确闹钟。Android 12 起需要授权；本 App 声明了 USE_EXACT_ALARM（Android 13+ 自动授予），
     * Android 12/12L 上使用默认授予的 SCHEDULE_EXACT_ALARM，用户可能在设置里关掉。
     */
    fun canScheduleExact(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()

    /**
     * 注册（或替换）闹钟。PendingIntent 使用固定 requestCode，所以重复调用只会更新同一个闹钟，不会出现两个。
     *
     * @param triggerAtMillis 响铃时间。
     * @param smartEarly 是否为智能提前唤醒，透传给响铃页面显示不同文案。
     */
    fun schedule(triggerAtMillis: Long, smartEarly: Boolean) {
        val operation = alarmPendingIntent(smartEarly)
        try {
            if (canScheduleExact()) {
                // showIntent：用户点击状态栏闹钟图标时打开 App
                val showIntent = PendingIntent.getActivity(
                    context,
                    REQUEST_SHOW,
                    Intent(context, MainActivity::class.java),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                alarmManager.setAlarmClock(AlarmManager.AlarmClockInfo(triggerAtMillis, showIntent), operation)
            } else {
                // 没有精确闹钟权限时退化为允许在 Doze 中触发的非精确闹钟，可能晚几分钟，但不会不响
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, operation)
            }
        } catch (e: SecurityException) {
            Log.w(TAG, "精确闹钟权限被撤销，退化为非精确闹钟", e)
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, operation)
        }
    }

    /** 取消已注册的闹钟；没有注册时调用也是安全的。 */
    fun cancel() {
        alarmManager.cancel(alarmPendingIntent(smartEarly = false))
    }

    /**
     * 闹钟触发时发给 SleepAlarmReceiver 的广播。
     * FLAG_UPDATE_CURRENT：同一个 requestCode 下更新 extras（smartEarly），cancel 时也能匹配到同一个闹钟。
     */
    private fun alarmPendingIntent(smartEarly: Boolean): PendingIntent = PendingIntent.getBroadcast(
        context,
        REQUEST_ALARM,
        Intent(context, SleepAlarmReceiver::class.java).putExtra(SleepAlarmReceiver.EXTRA_SMART_EARLY, smartEarly),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    private companion object {
        const val REQUEST_ALARM = 20
        const val REQUEST_SHOW = 21
    }
}
