package com.ywb.focusguard.data.repository

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.ywb.focusguard.domain.model.SleepAlarmSettings
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 闹钟设置使用独立的 DataStore 文件 sleep_alarm。
 * 顶层属性委托保证整个进程只有一个实例：同一个文件被两个 DataStore 同时打开会直接抛异常。
 */
private val Context.sleepAlarmDataStore by preferencesDataStore(name = "sleep_alarm")

/**
 * 智能闹钟设置的 DataStore 实现。
 *
 * 读取方：睡眠页（编辑、展示）、SleepMonitorService（开始监测时计算今晚闹钟）。
 */
@Singleton
class DataStoreSleepAlarmRepository @Inject constructor(
    @param:ApplicationContext private val context: Context
) : SleepAlarmRepository {

    /** Key 与 SleepAlarmSettings 字段一一对应。 */
    private object Keys {
        val enabled = booleanPreferencesKey("enabled")
        val wakeHour = intPreferencesKey("wake_hour")
        val wakeMinute = intPreferencesKey("wake_minute")
        val windowMinutes = intPreferencesKey("window_minutes")
    }

    /** 缺失的 Key 回退到 SleepAlarmSettings 的默认值。 */
    override val settings: Flow<SleepAlarmSettings> = context.sleepAlarmDataStore.data.map { prefs ->
        val defaults = SleepAlarmSettings()
        SleepAlarmSettings(
            enabled = prefs[Keys.enabled] ?: defaults.enabled,
            wakeHour = prefs[Keys.wakeHour] ?: defaults.wakeHour,
            wakeMinute = prefs[Keys.wakeMinute] ?: defaults.wakeMinute,
            windowMinutes = prefs[Keys.windowMinutes] ?: defaults.windowMinutes
        )
    }

    /** edit 是原子事务：四个字段要么一起写入，要么都不写。 */
    override suspend fun update(settings: SleepAlarmSettings) {
        context.sleepAlarmDataStore.edit { prefs ->
            prefs[Keys.enabled] = settings.enabled
            prefs[Keys.wakeHour] = settings.wakeHour.coerceIn(0, 23)
            prefs[Keys.wakeMinute] = settings.wakeMinute.coerceIn(0, 59)
            prefs[Keys.windowMinutes] = settings.windowMinutes.coerceIn(5, 90)
        }
    }
}
