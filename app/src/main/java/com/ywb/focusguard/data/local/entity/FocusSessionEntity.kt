package com.ywb.focusguard.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 一次专注会话在 Room 中的持久化结构。
 *
 * @property id 数据库自增主键，也是采样表关联本次会话的外键值。
 * @property startTime 会话开始的 Unix 时间戳，单位毫秒。
 * @property endTime 会话结束的 Unix 时间戳；为 null 表示会话仍在进行。
 * @property durationMillis 去除暂停时间后的有效专注时长，单位毫秒。
 * @property averageNoiseDb 专注期间的平均相对噪声值，单位 dB。
 * @property maxNoiseDb 专注期间观测到的最大相对噪声值，单位 dB。
 * @property averageLightLux 专注期间的平均光照强度，单位 lux。
 * @property movementCount 经过防抖确认的有效移动事件数量。
 * @property distractionCount 专注期间离开应用等分心行为的次数。
 * @property score 根据环境和专注行为计算出的总分，范围 0 到 100。
 * @property note 用户备注或系统生成的会话状态说明。
 * @property distractionTracked 这场会话是否开启了分心检测（有使用情况访问权限）。
 *   用来区分"0 次分心"和"根本没检测"，避免把未检测展示成表现完美。
 *   数据库版本 3 新增，老数据迁移时默认 0（未检测）；@ColumnInfo 的默认值必须与迁移 SQL 一致。
 */
@Entity(tableName = "focus_sessions")
data class FocusSessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val startTime: Long,
    val endTime: Long?,
    val durationMillis: Long,
    val averageNoiseDb: Float,
    val maxNoiseDb: Float,
    val averageLightLux: Float,
    val movementCount: Int,
    val distractionCount: Int,
    val score: Int,
    val note: String?,
    @ColumnInfo(defaultValue = "0")
    val distractionTracked: Boolean = false
)
