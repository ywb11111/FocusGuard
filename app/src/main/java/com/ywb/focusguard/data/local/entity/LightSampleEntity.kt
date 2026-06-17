package com.ywb.focusguard.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "light_samples")
data class LightSampleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sessionId: Long,
    val timestamp: Long,
    val lux: Float
)
