# MotionSensorDataSource Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 接入 `SensorManager.TYPE_ACCELEROMETER`，让 Today / Session 页面开始使用真实手机移动状态。

**Architecture:** 新增 `MotionClassifier` 负责纯规则计算，新增 `MotionSensorDataSource` 封装加速度计监听并用 `callbackFlow` 输出 `Flow<MotionSample>`。`EnvironmentRepositoryImpl` 依赖数据源，不直接操作 Android 传感器 API。

**Tech Stack:** Kotlin、Coroutine Flow、callbackFlow、SensorManager、Hilt、Jetpack Compose。

## Global Constraints

- 默认中文注释、中文文档和中文总结。
- 不新增依赖。
- 本次只接移动检测，不接噪声检测。
- 没有加速度计时返回默认稳定样本，不能让页面崩溃。
- 完成后更新开发进度并提醒 Git 提交。

---

### Task 1: 移动判断规则

**Files:**
- Create: `app/src/main/java/com/ywb/focusguard/data/sensor/MotionClassifier.kt`
- Create: `app/src/test/java/com/ywb/focusguard/data/sensor/MotionClassifierTest.kt`

**Interfaces:**
- Produces: `fun calculateMagnitude(x: Float, y: Float, z: Float): Float`
- Produces: `fun isSignificantMove(magnitude: Float): Boolean`

- [x] **Step 1: Write failing classifier test**
- [x] **Step 2: Run test and verify failure**
- [x] **Step 3: Implement classifier**
- [x] **Step 4: Run test and verify pass**

### Task 2: MotionSensorDataSource

**Files:**
- Create: `app/src/main/java/com/ywb/focusguard/data/sensor/MotionSensorDataSource.kt`

**Interfaces:**
- Produces: `fun observeMotion(): Flow<MotionSample>`

- [x] **Step 1: Create callbackFlow wrapper**
- [x] **Step 2: Register TYPE_ACCELEROMETER listener**
- [x] **Step 3: Convert x/y/z to magnitude**
- [x] **Step 4: Unregister listener in awaitClose**
- [x] **Step 5: Provide fallback sample when sensor is missing**

### Task 3: Repository 接入

**Files:**
- Modify: `app/src/main/java/com/ywb/focusguard/data/repository/EnvironmentRepositoryImpl.kt`

**Interfaces:**
- Consumes: `MotionSensorDataSource.observeMotion()`
- Produces: real-motion-backed `EnvironmentRepository.observeMotion()`

- [x] **Step 1: Inject MotionSensorDataSource**
- [x] **Step 2: Replace fixed motion demo Flow**
- [x] **Step 3: Keep noise demo data unchanged**

### Task 4: 验证与文档

**Files:**
- Modify: `docs/开发计划.md`
- Modify: `docs/开发进度.md`

**Verification:**
- Run: `.\gradlew.bat :app:testDebugUnitTest --tests "com.ywb.focusguard.data.sensor.MotionClassifierTest"`
- Run: `.\gradlew.bat :app:assembleDebug`

- [x] **Step 1: Update docs**
- [x] **Step 2: Run tests**
- [x] **Step 3: Run build**
