package com.ywb.focusguard.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 专注期间保存的一条光照采样记录。
 *
 * @property id 数据库自增主键。
 * @property sessionId 所属专注会话的 id，用于查询一场会话的完整曲线。
 * @property timestamp 采样发生的 Unix 时间戳，单位毫秒。
 * @property lux 光照传感器返回的照度值，单位 lux。
 */
@Entity(tableName = "light_samples")
data class LightSampleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sessionId: Long,
    val timestamp: Long,
    val lux: Float
)
