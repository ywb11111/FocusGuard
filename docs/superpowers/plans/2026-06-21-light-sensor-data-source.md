# LightSensorDataSource Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 接入 `SensorManager.TYPE_LIGHT`，让 Today / Session 页面开始使用真实光照数据。

**Architecture:** 新增 `LightSensorDataSource` 封装 Android 光照传感器，并用 `callbackFlow` 暴露 `Flow<LightSample>`。`EnvironmentRepositoryImpl` 只依赖数据源接口，不直接操作 `SensorManager`。

**Tech Stack:** Kotlin、Coroutine Flow、callbackFlow、SensorManager、Hilt、Jetpack Compose。

## Global Constraints

- 默认中文注释、中文文档和中文总结。
- 不新增依赖。
- 本次只接光照传感器，不接移动检测和噪声检测。
- 没有光照传感器时返回默认样本，不能让页面崩溃。
- 完成后更新开发进度并提醒 Git 提交。

---

### Task 1: 光照等级规则

**Files:**
- Create: `app/src/main/java/com/ywb/focusguard/data/sensor/LightLevelClassifier.kt`
- Create: `app/src/test/java/com/ywb/focusguard/data/sensor/LightLevelClassifierTest.kt`

**Interfaces:**
- Produces: `fun classifyLightLevel(lux: Float): LightLevel`

- [x] **Step 1: Write failing classifier test**
- [x] **Step 2: Run test and verify failure**
- [x] **Step 3: Implement classifier**
- [x] **Step 4: Run test and verify pass**

### Task 2: LightSensorDataSource

**Files:**
- Create: `app/src/main/java/com/ywb/focusguard/data/sensor/LightSensorDataSource.kt`

**Interfaces:**
- Produces: `fun observeLight(): Flow<LightSample>`

- [x] **Step 1: Create callbackFlow wrapper**
- [x] **Step 2: Register TYPE_LIGHT listener**
- [x] **Step 3: Unregister listener in awaitClose**
- [x] **Step 4: Provide fallback sample when sensor is missing**

### Task 3: Repository 接入

**Files:**
- Modify: `app/src/main/java/com/ywb/focusguard/data/repository/EnvironmentRepositoryImpl.kt`

**Interfaces:**
- Consumes: `LightSensorDataSource.observeLight()`
- Produces: real-light-backed `EnvironmentRepository.observeLight()`

- [x] **Step 1: Inject LightSensorDataSource**
- [x] **Step 2: Replace fixed light demo Flow**
- [x] **Step 3: Keep noise and motion demo data unchanged**

### Task 4: 验证与文档

**Files:**
- Modify: `docs/开发计划.md`
- Modify: `docs/开发进度.md`

**Verification:**
- Run: `.\gradlew.bat :app:testDebugUnitTest --tests "com.ywb.focusguard.data.sensor.LightLevelClassifierTest"`
- Run: `.\gradlew.bat :app:assembleDebug`

- [x] **Step 1: Update docs**
- [x] **Step 2: Run tests**
- [x] **Step 3: Run build**
