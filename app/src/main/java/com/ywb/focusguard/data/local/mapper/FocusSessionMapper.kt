package com.ywb.focusguard.data.local.mapper

import com.ywb.focusguard.data.local.entity.FocusSessionEntity
import com.ywb.focusguard.domain.model.FocusSession

// mapper 把数据库模型和领域模型隔离开。
// Room Entity 关心表结构，Domain Model 关心业务含义，二者不要在项目里随意混用。
fun FocusSessionEntity.toDomain(): FocusSession = FocusSession(
    id = id,
    startTime = startTime,
    endTime = endTime,
    durationMillis = durationMillis,
    averageNoiseDb = averageNoiseDb,
    maxNoiseDb = maxNoiseDb,
    averageLightLux = averageLightLux,
    movementCount = movementCount,
    distractionCount = distractionCount,
    score = score,
    note = note
)

fun FocusSession.toEntity(): FocusSessionEntity = FocusSessionEntity(
    id = id,
    startTime = startTime,
    endTime = endTime,
    durationMillis = durationMillis,
    averageNoiseDb = averageNoiseDb,
    maxNoiseDb = maxNoiseDb,
    averageLightLux = averageLightLux,
    movementCount = movementCount,
    distractionCount = distractionCount,
    score = score,
    note = note
)
