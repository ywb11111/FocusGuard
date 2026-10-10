package com.ywb.focusguard.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 专注期间的一次分心记录：切到某个 App 并停留了一段时间。
 *
 * 为什么要存下来而不是每次打开详情页时再查系统：
 * 系统的 App 使用事件只保留几天，过期就查不到了；专注结束时分析一次并落库，历史记录才能长期回看。
 *
 * 修改字段必须同步修改 MIGRATION_2_3 的建表 SQL。
 *
 * @property id 自增主键。
 * @property sessionId 所属专注会话 id（focus_sessions.id），建索引以便按会话查询。
 * @property packageName App 包名。
 * @property appLabel 记录时的 App 名称；之后即使 App 被卸载，历史记录仍能显示名字。
 * @property startTime 切到该 App 的时间。
 * @property endTime 离开该 App 的时间。
 */
@Entity(
    tableName = "distraction_events",
    indices = [Index(value = ["sessionId"])]
)
data class DistractionEventEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sessionId: Long,
    val packageName: String,
    val appLabel: String,
    val startTime: Long,
    val endTime: Long
)
