package com.ywb.focusguard.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 经过窗口判断和冷却防抖后确认的一次移动事件。
 *
 * @property id 数据库自增主键。
 * @property sessionId 所属专注会话的 id。
 * @property timestamp 移动事件确认时的 Unix 时间戳，单位毫秒。
 * @property magnitude 事件确认时三轴加速度的合成模长，单位约为 m/s²。
 */
@Entity(tableName = "motion_events")
data class MotionEventEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sessionId: Long,
    val timestamp: Long,
    val magnitude: Float
)
