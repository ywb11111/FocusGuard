# SessionDetail 真实数据 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 让专注详情页通过真实 `sessionId` 查询 Repository，并展示 Room 中的专注记录详情。

**Architecture:** `SessionDetailViewModel` 使用 `SavedStateHandle` 读取导航参数，再订阅 `FocusRepository.observeSessionDetail(sessionId)`。页面只接收 `SessionDetailUiState`，不直接访问 Repository、Room 或导航参数。

**Tech Stack:** Kotlin、StateFlow、SavedStateHandle、Hilt、Navigation Compose、Room Flow、Jetpack Compose。

## Global Constraints

- 默认中文注释、中文文档和中文总结。
- 不新增依赖。
- 本次只接详情页真实会话数据，不接真实噪声/光照/移动采样。
- 完成后更新开发进度并提醒 Git 提交。

---

### Task 1: 详情 UI 状态映射

**Files:**
- Create: `app/src/main/java/com/ywb/focusguard/ui/state/SessionDetailUiState.kt`
- Create: `app/src/test/java/com/ywb/focusguard/ui/state/SessionDetailUiStateTest.kt`

**Interfaces:**
- Produces: `fun SessionDetail.toUiState(): SessionDetailUiState.Content`

- [x] **Step 1: Write failing mapper test**
- [x] **Step 2: Run test and verify failure**
- [x] **Step 3: Implement minimal UI state mapper**
- [x] **Step 4: Run test and verify pass**

### Task 2: ViewModel 接入 Repository

**Files:**
- Create: `app/src/main/java/com/ywb/focusguard/ui/viewmodel/SessionDetailViewModel.kt`

**Interfaces:**
- Consumes: `SavedStateHandle[Destination.SessionDetail.ARG_SESSION_ID]`
- Consumes: `FocusRepository.observeSessionDetail(sessionId)`
- Produces: `val uiState: StateFlow<SessionDetailUiState>`

- [x] **Step 1: Create ViewModel**
- [x] **Step 2: Map null detail to Empty state**
- [x] **Step 3: Map existing detail to Content state**

### Task 3: 页面接线

**Files:**
- Modify: `app/src/main/java/com/ywb/focusguard/ui/screen/SessionDetailScreen.kt`
- Modify: `app/src/main/java/com/ywb/focusguard/ui/navigation/FocusGuardNavHost.kt`

**Interfaces:**
- Consumes: `SessionDetailViewModel.uiState`
- Produces: `SessionDetailRoute`

- [x] **Step 1: Add Route composable**
- [x] **Step 2: Replace hardcoded values with uiState values**
- [x] **Step 3: Let NavHost call SessionDetailRoute**

### Task 4: 验证与文档

**Files:**
- Modify: `docs/开发计划.md`
- Modify: `docs/开发进度.md`

**Verification:**
- Run: `.\gradlew.bat :app:testDebugUnitTest --tests "com.ywb.focusguard.ui.state.SessionDetailUiStateTest"`
- Run: `.\gradlew.bat :app:assembleDebug`

- [x] **Step 1: Update docs**
- [x] **Step 2: Run tests**
- [x] **Step 3: Run build**
