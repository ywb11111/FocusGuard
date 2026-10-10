package com.ywb.focusguard.data.system

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Process
import android.telecom.TelecomManager
import com.ywb.focusguard.domain.model.AppForegroundEvent
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 是否已获得"使用情况访问权限"（PACKAGE_USAGE_STATS）。
 *
 * 这个权限不是普通运行时权限，checkSelfPermission 永远返回拒绝；
 * 真正的授权状态记录在 AppOps 里，必须通过 AppOpsManager 查询。
 * 抽成顶层函数，PermissionManager 和 AppUsageDataSource 共用同一套判断。
 *
 * 两个 API 在新 SDK 中都被标记弃用，但目前仍是查询 OPSTR_GET_USAGE_STATS 的标准做法，因此抑制弃用警告。
 */
@Suppress("DEPRECATION")
fun Context.hasUsageStatsPermission(): Boolean {
    val appOps = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
    val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        appOps.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), packageName)
    } else {
        appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), packageName)
    }
    return mode == AppOpsManager.MODE_ALLOWED
}

/**
 * App 使用记录数据源：封装 UsageStatsManager，提供"一段时间内哪些 App 进出前台"。
 *
 * **为什么用 UsageStatsManager 而不是无障碍服务**：
 * 无障碍能实时拿到当前窗口，但权限过大、审核严格，用户也更难信任；
 * 分心统计只需要专注结束后回看一次，UsageStatsManager 的事件记录完全够用，权限也更轻。
 *
 * **隐私**：只在本机读取包名和时间，结果只存本地数据库，不上传。
 *
 * @param context Application Context。
 */
@Singleton
class AppUsageDataSource @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    /** 是否已授权使用情况访问。 */
    fun hasPermission(): Boolean = context.hasUsageStatsPermission()

    /**
     * 查询时间范围内的前台切换事件。queryEvents 会读取系统文件，放到 IO 线程。
     *
     * @return 未授权时返回空列表。
     */
    suspend fun queryForegroundEvents(startMillis: Long, endMillis: Long): List<AppForegroundEvent> =
        withContext(Dispatchers.IO) {
            if (!hasPermission()) return@withContext emptyList()
            val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
            val usageEvents = usageStatsManager.queryEvents(startMillis, endMillis)
            val result = mutableListOf<AppForegroundEvent>()
            val event = UsageEvents.Event()
            // UsageEvents 是游标式迭代：复用同一个 Event 对象读取，避免为每条事件创建对象
            while (usageEvents.hasNextEvent()) {
                usageEvents.getNextEvent(event)
                val isForeground = when (event.eventType) {
                    EVENT_FOREGROUND -> true
                    EVENT_BACKGROUND -> false
                    else -> null
                } ?: continue
                result += AppForegroundEvent(
                    timestamp = event.timeStamp,
                    packageName = event.packageName,
                    isForeground = isForeground
                )
            }
            result
        }

    /**
     * 不算分心的包名：FocusGuard 自己、桌面、系统界面、来电界面、权限弹窗。
     *
     * 桌面因机型而异（MIUI、EMUI 各不相同），所以通过 HOME Intent 动态查询而不是写死包名；
     * Android 11+ 的包可见性限制要求在 Manifest 的 <queries> 中声明 HOME，否则查不到。
     */
    fun ignoredPackages(): Set<String> {
        val packageManager = context.packageManager
        val launchers = packageManager
            .queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME), 0)
            .map { it.activityInfo.packageName }
        val dialer = runCatching {
            (context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager).defaultDialerPackage
        }.getOrNull()
        return buildSet {
            add(context.packageName)
            addAll(launchers)
            add("com.android.systemui")
            add("android")
            add("com.android.permissioncontroller")
            add("com.google.android.permissioncontroller")
            dialer?.let(::add)
        }
    }

    /**
     * 包名 → App 显示名称。Android 11+ 需要 <queries> 声明 LAUNCHER 才能读取其他 App 的信息；
     * 取不到（已卸载或不可见）时退回包名，保证列表总有内容。
     */
    fun appLabel(packageName: String): String = try {
        val packageManager = context.packageManager
        packageManager.getApplicationLabel(packageManager.getApplicationInfo(packageName, 0)).toString()
    } catch (_: PackageManager.NameNotFoundException) {
        packageName
    }

    private companion object {
        /**
         * 进入前台 / 退到后台的事件类型。Android 10 起常量改名为 ACTIVITY_RESUMED / ACTIVITY_PAUSED，
         * 但数值与旧的 MOVE_TO_FOREGROUND / MOVE_TO_BACKGROUND 相同（1 / 2），直接用数值兼容所有版本。
         */
        const val EVENT_FOREGROUND = 1
        const val EVENT_BACKGROUND = 2
    }
}
