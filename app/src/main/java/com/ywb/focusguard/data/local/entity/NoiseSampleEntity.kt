package com.ywb.focusguard.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 专注期间保存的一条相对噪声采样记录。
 *
 * @property id 数据库自增主键。
 * @property sessionId 所属专注会话的 id。
 * @property timestamp 采样发生的 Unix 时间戳，单位毫秒。
 * @property decibel 由 PCM 振幅换算出的相对噪声值，并非专业声级计读数。
 */
@Entity(tableName = "noise_samples")
data class NoiseSampleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sessionId: Long,
    val timestamp: Long,
    val decibel: Float
)
