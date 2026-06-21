# 专注记录 Room 持久化 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将专注计时的开始和结束动作接入 Room，让完成后的专注记录成为真实本地数据。

**Architecture:** `SessionViewModel` 只处理 UI 状态和计时事件，开始/结束会话通过 `FocusRepository` 完成。`FocusRepositoryImpl` 依赖 `FocusSessionDao`，用 mapper 在 Room Entity 和 Domain Model 之间转换。

**Tech Stack:** Kotlin、Coroutine、StateFlow、Room、Hilt、Jetpack Compose。

## Global Constraints

- 默认中文注释、中文文档和中文总结。
- 不新增依赖。
- 不引入传感器、音频或前台服务逻辑。
- 每个小闭环后更新开发进度并提醒 Git 提交。

---

### Task 1: 数据模型转换

**Files:**
- Create: `app/src/main/java/com/ywb/focusguard/data/local/mapper/FocusSessionMapper.kt`
- Create: `app/src/test/java/com/ywb/focusguard/data/local/mapper/FocusSessionMapperTest.kt`

**Interfaces:**
- Produces: `FocusSessionEntity.toDomain(): FocusSession`
- Produces: `FocusSession.toEntity(): FocusSessionEntity`

- [x] **Step 1: Write the failing test**
- [x] **Step 2: Run test to verify it fails**
- [x] **Step 3: Write minimal mapper implementation**
- [x] **Step 4: Run test to verify it passes**

### Task 2: DAO 支持更新进行中记录

**Files:**
- Modify: `app/src/main/java/com/ywb/focusguard/data/local/dao/FocusSessionDao.kt`

**Interfaces:**
- Produces: `suspend fun updateFinishedSession(...)`

- [x] **Step 1: Add DAO update query**
- [x] **Step 2: Build verify Room query compilation**

### Task 3: Repository 切换到 Room

**Files:**
- Modify: `app/src/main/java/com/ywb/focusguard/data/repository/FocusRepositoryImpl.kt`
- Modify: `app/src/main/java/com/ywb/focusguard/di/AppModule.kt`

**Interfaces:**
- Consumes: `FocusSessionDao`
- Consumes: mapper functions
- Produces: Room-backed `observeSessions()` and `observeTodaySummary()`

- [x] **Step 1: Inject `FocusSessionDao`**
- [x] **Step 2: Replace in-memory sessions with DAO Flow**
- [x] **Step 3: Implement start/finish writes**

### Task 4: ViewModel 调用 Repository

**Files:**
- Modify: `app/src/main/java/com/ywb/focusguard/ui/viewmodel/SessionViewModel.kt`

**Interfaces:**
- Consumes: `FocusRepository.startSession(config)`
- Consumes: `FocusRepository.finishSession(sessionId)`

- [x] **Step 1: Inject FocusRepository**
- [x] **Step 2: Start session inside coroutine**
- [x] **Step 3: Finish session inside coroutine and emit Finished**

### Task 5: 文档与验证

**Files:**
- Modify: `docs/开发进度.md`
- Optional: `docs/开发计划.md`

**Verification:**
- Run: `.\gradlew.bat :app:assembleDebug`
- Expected: exit code 0

- [x] **Step 1: Update project progress**
- [x] **Step 2: Run build verification**
- [x] **Step 3: Review git status**
