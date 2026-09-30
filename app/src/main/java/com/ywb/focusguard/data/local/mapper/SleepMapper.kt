package com.ywb.focusguard.data.local.mapper

import com.ywb.focusguard.data.local.entity.SleepEpochEntity
import com.ywb.focusguard.data.local.entity.SleepSessionEntity
import com.ywb.focusguard.domain.model.SleepEpoch
import com.ywb.focusguard.domain.model.SleepPlacement
import com.ywb.focusguard.domain.model.SleepSession

// Entity ↔ 领域模型转换集中在这里：Room 的存储细节（枚举存字符串等）不泄漏到 UI 层。

/** 睡眠主记录 Entity → 领域模型。 */
fun SleepSessionEntity.toDomain(): SleepSession = SleepSession(
    id = id,
    startTime = startTime,
    endTime = endTime,
    placement = SleepPlacement.fromName(placement),
    audioEnabled = audioEnabled,
    sleepOnsetTime = sleepOnsetTime,
    finalWakeTime = finalWakeTime,
    monitoredMillis = monitoredMillis,
    totalSleepMillis = totalSleepMillis,
    sleepLatencyMillis = sleepLatencyMillis,
    wakeCount = wakeCount,
    snoreEventCount = snoreEventCount,
    averageNoiseDb = averageNoiseDb,
    averageLightLux = averageLightLux,
    score = score,
    note = note
)

/** 分钟数据 Entity → 领域模型。 */
fun SleepEpochEntity.toDomain(): SleepEpoch = SleepEpoch(
    startTime = startTime,
    movementSeconds = movementSeconds,
    averageNoiseDb = averageNoiseDb,
    maxNoiseDb = maxNoiseDb,
    soundEventCount = soundEventCount,
    snoreEventCount = snoreEventCount,
    averageLightLux = averageLightLux,
    screenOnSeconds = screenOnSeconds
)

/** 分钟数据领域模型 → Entity，需要补上所属记录 id。 */
fun SleepEpoch.toEntity(sessionId: Long): SleepEpochEntity = SleepEpochEntity(
    sessionId = sessionId,
    startTime = startTime,
    movementSeconds = movementSeconds,
    averageNoiseDb = averageNoiseDb,
    maxNoiseDb = maxNoiseDb,
    soundEventCount = soundEventCount,
    snoreEventCount = snoreEventCount,
    averageLightLux = averageLightLux,
    screenOnSeconds = screenOnSeconds
)
