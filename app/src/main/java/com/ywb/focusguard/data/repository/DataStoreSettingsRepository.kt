package com.ywb.focusguard.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.ywb.focusguard.domain.model.UserSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * DataStore 文件扩展属性，确保全局唯一 DataStore 实例。
 *
 * 为什么用 preferencesDataStore 而不是直接 new DataStore？
 * Jetpack DataStore 官方推荐方式，自动处理文件锁、并发访问和生命周期。
 * Kotlin 属性委托保证整个进程只有一个 DataStore 实例，避免文件损坏。
 */
private val Context.defaultDataStore by preferencesDataStore(name = "user_settings")

/**
 * 设置 Repository 的 DataStore 持久化实现。
 *
 * 与内存实现的差异：
 * 1. 数据写入磁盘 JSON 文件，App 重启后设置不丢失
 * 2. 读取通过 Flow 实现响应式，UI 自动更新
 * 3. 写入是 suspend 函数，需要在协程中调用
 *
 * Key 设计原则：
 * - 每个设置项用独立的 Preferences Key
 * - Key 名称与 UserSettings 属性一一对应
 * - 便于后续迁移和版本管理
 */
class DataStoreSettingsRepository @Inject constructor(
    private val context: Context
) : SettingsRepository {

    /**
     * 每个实例使用独立的 DataStore，避免测试间数据污染。
     * 生产环境只有一个实例，测试环境每个测试用例创建独立实例。
     */
    private val Context.dataStore by preferencesDataStore(name = "user_settings")

    /** DataStore Key 定义，与 UserSettings 字段一一对应 */
    private object Keys {
        val defaultFocusMinutes = intPreferencesKey("default_focus_minutes")
        val noiseThresholdDb = floatPreferencesKey("noise_threshold_db")
        val comfortableLightMinLux = floatPreferencesKey("comfortable_light_min_lux")
        val comfortableLightMaxLux = floatPreferencesKey("comfortable_light_max_lux")
        val backgroundMonitoringEnabled = booleanPreferencesKey("background_monitoring_enabled")
        val dailyReportEnabled = booleanPreferencesKey("daily_report_enabled")
    }

    /**
     * 观察用户设置的变化。
     *
     * 实现原理：
     * 1. dataStore.data 返回 Flow<Preferences>，每次写入都触发新值
     * 2. map 将 Preferences 转换为 UserSettings
     * 3. 如果某个 Key 不存在，fallback 到 UserSettings 默认值
     */
    override val settings: Flow<UserSettings> = context.dataStore.data.map { prefs ->
        UserSettings(
            defaultFocusMinutes = prefs[Keys.defaultFocusMinutes] ?: UserSettings().defaultFocusMinutes,
            noiseThresholdDb = prefs[Keys.noiseThresholdDb] ?: UserSettings().noiseThresholdDb,
            comfortableLightMinLux = prefs[Keys.comfortableLightMinLux] ?: UserSettings().comfortableLightMinLux,
            comfortableLightMaxLux = prefs[Keys.comfortableLightMaxLux] ?: UserSettings().comfortableLightMaxLux,
            backgroundMonitoringEnabled = prefs[Keys.backgroundMonitoringEnabled] ?: UserSettings().backgroundMonitoringEnabled,
            dailyReportEnabled = prefs[Keys.dailyReportEnabled] ?: UserSettings().dailyReportEnabled
        )
    }

    /**
     * 更新噪声阈值。
     *
     * edit 是 suspend 函数，内部处理了事务性写入：
     * - 读取当前 Preferences 快照
     * - 执行 block 修改
     * - 写回磁盘
     * 如果写入失败，修改不会生效，保证数据一致性。
     */
    override suspend fun updateNoiseThreshold(value: Float) {
        context.dataStore.edit { prefs ->
            prefs[Keys.noiseThresholdDb] = value
        }
    }

    /** 更新舒适光照范围，调用方应保证 min 不大于 max。 */
    override suspend fun updateLightRange(min: Float, max: Float) {
        context.dataStore.edit { prefs ->
            prefs[Keys.comfortableLightMinLux] = min
            prefs[Keys.comfortableLightMaxLux] = max
        }
    }

    /** 更新默认专注分钟数。 */
    override suspend fun updateDefaultFocusMinutes(minutes: Int) {
        context.dataStore.edit { prefs ->
            prefs[Keys.defaultFocusMinutes] = minutes
        }
    }
}
