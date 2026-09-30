package com.ywb.focusguard.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 一晚睡眠监测在 Room 中的主记录。
 *
 * 开始监测时由 SleepMonitorService 插入（endTime = null 表示进行中）；
 * 结束时由 SleepRepositoryImpl 调用 SleepAnalyzer 汇总后回写各统计字段。
 * 列表页和趋势图只读这张表，不需要每次重算分钟数据。
 *
 * 注意：修改字段必须同步修改 [com.ywb.focusguard.data.local.database.MIGRATION_1_2] 里的建表 SQL，
 * 否则老用户升级时 Room 会校验表结构失败并崩溃。
 *
 * @property id 自增主键，sleep_epochs.sessionId 引用它。
 * @property startTime 开始监测的 Unix 毫秒时间戳。
 * @property endTime 结束时间；null 表示仍在监测。
 * @property placement 手机摆放位置的枚举名（BED / NIGHTSTAND）。
 * @property audioEnabled 是否启用了麦克风分析；SQLite 中存为 0/1。
 * @property sleepOnsetTime 估算入睡时间。
 * @property finalWakeTime 估算最终醒来时间。
 * @property monitoredMillis 实际有数据的时长。
 * @property totalSleepMillis 估算总睡眠时长。
 * @property sleepLatencyMillis 入睡耗时。
 * @property wakeCount 夜醒次数。
 * @property snoreEventCount 疑似鼾声事件数。
 * @property averageNoiseDb 平均相对噪声。
 * @property averageLightLux 平均光照。
 * @property score 睡眠评分。
 * @property note 系统说明。
 */
@Entity(tableName = "sleep_sessions")
data class SleepSessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val startTime: Long,
    val endTime: Long?,
    val placement: String,
    val audioEnabled: Boolean,
    val sleepOnsetTime: Long?,
    val finalWakeTime: Long?,
    val monitoredMillis: Long,
    val totalSleepMillis: Long,
    val sleepLatencyMillis: Long,
    val wakeCount: Int,
    val snoreEventCount: Int,
    val averageNoiseDb: Float,
    val averageLightLux: Float,
    val score: Int,
    val note: String?
)
