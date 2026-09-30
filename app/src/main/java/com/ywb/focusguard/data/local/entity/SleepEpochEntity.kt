package com.ywb.focusguard.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 睡眠监测中每分钟一行的聚合数据。
 *
 * 为什么给 sessionId 建索引：
 * 详情页和结束分析都按 sessionId 查整晚约 480 行；随着记录变多表会越来越大，
 * 没有索引时 SQLite 需要全表扫描，有索引后直接定位到这一晚的数据。
 *
 * 字段含义见 [com.ywb.focusguard.domain.model.SleepEpoch]。
 *
 * @property id 自增主键。
 * @property sessionId 所属睡眠记录 id。
 * @property startTime 这一分钟的开始时间。
 * @property movementSeconds 体动秒数 0..60。
 * @property averageNoiseDb 平均相对噪声。
 * @property maxNoiseDb 最大相对噪声。
 * @property soundEventCount 声音事件数。
 * @property snoreEventCount 疑似鼾声事件数。
 * @property averageLightLux 平均光照。
 * @property screenOnSeconds 亮屏秒数。
 */
@Entity(
    tableName = "sleep_epochs",
    indices = [Index(value = ["sessionId"])]
)
data class SleepEpochEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sessionId: Long,
    val startTime: Long,
    val movementSeconds: Int,
    val averageNoiseDb: Float,
    val maxNoiseDb: Float,
    val soundEventCount: Int,
    val snoreEventCount: Int,
    val averageLightLux: Float,
    val screenOnSeconds: Int
)
