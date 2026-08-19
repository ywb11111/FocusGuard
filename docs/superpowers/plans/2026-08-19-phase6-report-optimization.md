# 阶段 6：报告优化 & DataStore 设置 实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 完成报告页真实数据（周/月聚合、趋势图）、DataStore 持久化设置、清理过时文案，使项目达到可面试展示的完成度。

**Architecture:** 在 DAO 层增加按日期范围查询的能力，ReportsViewModel 按周/月切换聚合统计，ReportsScreen 移除 demo 数据。SettingsRepository 从内存 MutableStateFlow 切换为 DataStore Preferences。同时清理代码中与实际不符的过时注释和 UI 文案。

**Tech Stack:** Room (日期范围查询)、DataStore Preferences (设置持久化)、Compose (UI 更新)、Flow (状态管理)

---

## 文件结构

| 操作 | 文件 | 职责 |
|------|------|------|
| Modify | `app/.../data/local/dao/FocusSessionDao.kt` | 增加按日期范围查询方法 |
| Modify | `app/.../data/repository/RepositoryContracts.kt` | FocusRepository 增加日期范围查询接口 |
| Modify | `app/.../data/repository/FocusRepositoryImpl.kt` | 实现日期范围查询和周/月聚合 |
| Create | `app/.../data/repository/DataStoreSettingsRepository.kt` | DataStore 版设置实现 |
| Modify | `app/.../di/AppModule.kt` | 绑定 DataStore SettingsRepository |
| Modify | `app/.../ui/state/UiStates.kt` | ReportsUiState 增加周/月摘要字段 |
| Modify | `app/.../ui/viewmodel/ReportsViewModel.kt` | 支持周/月切换、聚合统计 |
| Modify | `app/.../ui/screen/ReportsScreen.kt` | 移除 demo 数据，接入真实聚合 |
| Modify | `app/.../ui/viewmodel/SettingsViewModel.kt` | 读取 DataStore 设置 |
| Modify | `app/.../ui/screen/TodayScreen.kt` | 修复过时文案 |
| Modify | `app/.../ui/screen/SettingsScreen.kt` | 修复过时文案，设置可编辑 |
| Modify | `app/.../data/repository/RepositoryContracts.kt` | 修复过时注释 |
| Modify | `CLAUDE.md` | 更新阶段进度表 |

---

### Task 1: DAO 增加日期范围查询

**Files:**
- Modify: `app/src/main/java/com/ywb/focusguard/data/local/dao/FocusSessionDao.kt`

- [ ] **Step 1: 在 FocusSessionDao 中增加按日期范围查询已完成会话的方法**

```kotlin
// 在 FocusSessionDao.kt 中添加：

/** 按日期范围查询已完成会话（用于周报/月报聚合）。 */
@Query("""
    SELECT * FROM focus_sessions 
    WHERE startTime >= :startMillis AND startTime < :endMillis AND endTime IS NOT NULL 
    ORDER BY startTime DESC
""")
fun observeSessionsInRange(startMillis: Long, endMillis: Long): Flow<List<FocusSessionEntity>>

/** 一次性获取指定日期范围内的已完成会话。 */
@Query("""
    SELECT * FROM focus_sessions 
    WHERE startTime >= :startMillis AND startTime < :endMillis AND endTime IS NOT NULL 
    ORDER BY startTime DESC
""")
suspend fun getSessionsInRange(startMillis: Long, endMillis: Long): List<FocusSessionEntity>
```

- [ ] **Step 2: 构建验证**

Run: `$env:JAVA_HOME='E:\jdk21'; .\gradlew.bat :app:assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/ywb/focusguard/data/local/dao/FocusSessionDao.kt
git commit -m "feat: DAO 增加按日期范围查询会话方法"
```

---

### Task 2: Repository 接口增加日期范围查询

**Files:**
- Modify: `app/src/main/java/com/ywb/focusguard/data/repository/RepositoryContracts.kt`

- [ ] **Step 1: 在 FocusRepository 接口中增加周/月聚合方法**

```kotlin
// 在 FocusRepository 接口中添加：

/** 按日期范围观察已完成会话（用于周报/月报）。 */
fun observeSessionsInRange(startMillis: Long, endMillis: Long): Flow<List<FocusSession>>

/** 一次性获取日期范围内的会话，用于计算周/月统计。 */
suspend fun getSessionsInRange(startMillis: Long, endMillis: Long): List<FocusSession>
```

- [ ] **Step 2: 构建验证**

Run: `$env:JAVA_HOME='E:\jdk21'; .\gradlew.bat :app:assembleDebug`
Expected: BUILD SUCCESSFUL（接口方法未实现，编译会报错，需在 Task 3 中实现）

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/ywb/focusguard/data/repository/RepositoryContracts.kt
git commit -m "feat: FocusRepository 增加日期范围查询接口"
```

---

### Task 3: Repository 实现日期范围查询

**Files:**
- Modify: `app/src/main/java/com/ywb/focusguard/data/repository/FocusRepositoryImpl.kt`

- [ ] **Step 1: 在 FocusRepositoryImpl 中实现日期范围查询**

```kotlin
// 在 FocusRepositoryImpl 中添加，放在现有 observeSessions() 方法附近：

/** 按日期范围观察已完成会话，复用 DAO 的范围查询。 */
override fun observeSessionsInRange(startMillis: Long, endMillis: Long): Flow<List<FocusSession>> =
    focusSessionDao.observeSessionsInRange(startMillis, endMillis).map { entities ->
        entities.map { it.toDomain() }
    }

/** 一次性获取日期范围内的会话。 */
override suspend fun getSessionsInRange(startMillis: Long, endMillis: Long): List<FocusSession> =
    focusSessionDao.getSessionsInRange(startMillis, endMillis).map { it.toDomain() }
```

- [ ] **Step 2: 构建验证**

Run: `$env:JAVA_HOME='E:\jdk21'; .\gradlew.bat :app:assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/ywb/focusguard/data/repository/FocusRepositoryImpl.kt
git commit -m "feat: 实现日期范围查询，支持周报月报聚合"
```

---

### Task 4: ReportsUiState 增加周/月摘要

**Files:**
- Modify: `app/src/main/java/com/ywb/focusguard/ui/state/UiStates.kt`

- [ ] **Step 1: 重写 ReportsUiState，增加周/月切换和聚合统计**

```kotlin
// 替换现有 ReportsUiState：

/** 报告页周期类型。 */
enum class ReportPeriod(val label: String) {
    WEEK("本周"),
    MONTH("本月")
}

/** 报告页周期聚合统计。 */
data class PeriodSummary(
    val totalFocusMillis: Long = 0L,
    val averageScore: Int = 0,
    val sessionCount: Int = 0,
    val averageNoiseDb: Float = 0f,
    val averageLightLux: Float = 0f,
    val totalMovementCount: Int = 0
)

/**
 * 报告页状态。
 *
 * @property period 当前选中的周期（周/月）。
 * @property sessions 当前周期内的已完成会话，按时间倒序。
 * @property periodSummary 当前周期的聚合统计。
 * @property trendValues 趋势图数据（各会话评分）。
 */
data class ReportsUiState(
    val period: ReportPeriod = ReportPeriod.WEEK,
    val sessions: List<FocusSession> = emptyList(),
    val periodSummary: PeriodSummary = PeriodSummary(),
    val trendValues: List<Float> = emptyList()
)
```

- [ ] **Step 2: 构建验证**

Run: `$env:JAVA_HOME='E:\jdk21'; .\gradlew.bat :app:assembleDebug`
Expected: BUILD SUCCESSFUL（ReportsViewModel 和 ReportsScreen 会因字段变化报错，下一步修复）

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/ywb/focusguard/ui/state/UiStates.kt
git commit -m "feat: ReportsUiState 增加周/月周期切换和聚合统计"
```

---

### Task 5: ReportsViewModel 支持周/月切换

**Files:**
- Modify: `app/src/main/java/com/ywb/focusguard/ui/viewmodel/ReportsViewModel.kt`

- [ ] **Step 1: 重写 ReportsViewModel，支持周期切换和聚合计算**

```kotlin
package com.ywb.focusguard.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ywb.focusguard.data.repository.FocusRepository
import com.ywb.focusguard.domain.model.FocusSession
import com.ywb.focusguard.ui.state.PeriodSummary
import com.ywb.focusguard.ui.state.ReportPeriod
import com.ywb.focusguard.ui.state.ReportsUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

/**
 * 报告页 ViewModel：支持周/月切换，从 Room 按日期范围查询并聚合统计。
 *
 * 使用 flatMapLatest 切换周期时自动取消旧查询、启动新查询。
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ReportsViewModel @Inject constructor(
    private val focusRepository: FocusRepository
) : ViewModel() {

    /** 当前选中的报告周期。 */
    private val _period = MutableStateFlow(ReportPeriod.WEEK)

    /** 报告页完整状态。 */
    val uiState: StateFlow<ReportsUiState> = _period.flatMapLatest { period ->
        val (start, end) = periodRange(period)
        focusRepository.observeSessionsInRange(start, end).map { sessions ->
            buildUiState(period, sessions)
        }
    }.stateIn(
        scope = viewModelScope,
        started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5_000),
        initialValue = ReportsUiState()
    )

    /** 切换周/月周期。 */
    fun switchPeriod(period: ReportPeriod) {
        _period.value = period
    }

    /** 根据周期类型计算起止时间戳。 */
    private fun periodRange(period: ReportPeriod): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        val end = cal.apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.timeInMillis + 1

        val start = when (period) {
            ReportPeriod.WEEK -> {
                cal.apply {
                    add(Calendar.DAY_OF_YEAR, -6)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
            }
            ReportPeriod.MONTH -> {
                cal.apply {
                    add(Calendar.DAY_OF_YEAR, -29)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
            }
        }
        return Pair(start, end)
    }

    /** 从会话列表构建完整的报告页状态。 */
    private fun buildUiState(period: ReportPeriod, sessions: List<FocusSession>): ReportsUiState {
        val totalFocus = sessions.sumOf { it.durationMillis }
        val avgScore = sessions.map { it.score }.average().takeIf { !it.isNaN() }?.toInt() ?: 0
        val avgNoise = sessions.map { it.averageNoiseDb }.average().takeIf { !it.isNaN() }?.toFloat() ?: 0f
        val avgLight = sessions.map { it.averageLightLux }.average().takeIf { !it.isNaN() }?.toFloat() ?: 0f
        val totalMovement = sessions.sumOf { it.movementCount }

        return ReportsUiState(
            period = period,
            sessions = sessions,
            periodSummary = PeriodSummary(
                totalFocusMillis = totalFocus,
                averageScore = avgScore,
                sessionCount = sessions.size,
                averageNoiseDb = avgNoise,
                averageLightLux = avgLight,
                totalMovementCount = totalMovement
            ),
            trendValues = sessions.reversed().map { it.score.toFloat() }
        )
    }
}
```

注意：需要添加 `import kotlinx.coroutines.flow.map`。

- [ ] **Step 2: 构建验证**

Run: `$env:JAVA_HOME='E:\jdk21'; .\gradlew.bat :app:assembleDebug`
Expected: BUILD SUCCESSFUL（ReportsScreen 还需更新，可能报错）

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/ywb/focusguard/ui/viewmodel/ReportsViewModel.kt
git commit -m "feat: ReportsViewModel 支持周/月切换和真实聚合统计"
```

---

### Task 6: ReportsScreen 接入真实数据

**Files:**
- Modify: `app/src/main/java/com/ywb/focusguard/ui/screen/ReportsScreen.kt`

- [ ] **Step 1: 重写 ReportsScreen，移除 demo 数据，接入真实聚合**

```kotlin
package com.ywb.focusguard.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ywb.focusguard.domain.model.FocusSession
import com.ywb.focusguard.ui.component.MetricCard
import com.ywb.focusguard.ui.component.SectionHeader
import com.ywb.focusguard.ui.component.SimpleLineChart
import com.ywb.focusguard.ui.state.ReportPeriod
import com.ywb.focusguard.ui.state.ReportsUiState
import com.ywb.focusguard.ui.viewmodel.ReportsViewModel

/** 报告页路由层：收集 Room 驱动的报告状态并处理详情导航。 */
@Composable
fun ReportsRoute(
    onOpenSessionDetail: (Long) -> Unit,
    viewModel: ReportsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ReportsScreen(
        uiState = uiState,
        onOpenSessionDetail = onOpenSessionDetail,
        onSwitchPeriod = viewModel::switchPeriod
    )
}

/**
 * 报告页纯 UI，展示周/月汇总、趋势和历史列表。
 * 所有数据来自 Room，无 demo 占位。
 */
@Composable
fun ReportsScreen(
    uiState: ReportsUiState,
    onOpenSessionDetail: (Long) -> Unit,
    onSwitchPeriod: (ReportPeriod) -> Unit = {}
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "报告",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold
            )
        }
        item {
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                ReportPeriod.entries.forEachIndexed { index, period ->
                    SegmentedButton(
                        selected = uiState.period == period,
                        onClick = { onSwitchPeriod(period) },
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = ReportPeriod.entries.size
                        )
                    ) {
                        Text(period.label)
                    }
                }
            }
        }
        item {
            val summary = uiState.periodSummary
            SectionHeader(title = "${uiState.period.label}总结")
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)
            ) {
                MetricCard("总专注", formatDuration(summary.totalFocusMillis), Modifier.weight(1f))
                MetricCard("平均分", "${summary.averageScore}", Modifier.weight(1f))
                MetricCard("次数", "${summary.sessionCount}", Modifier.weight(1f))
            }
            if (summary.sessionCount > 0) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                ) {
                    MetricCard("平均噪声", "${summary.averageNoiseDb.toInt()} dB", Modifier.weight(1f))
                    MetricCard("平均光照", "${summary.averageLightLux.toInt()} lux", Modifier.weight(1f))
                    MetricCard("移动", "${summary.totalMovementCount} 次", Modifier.weight(1f))
                }
            }
        }
        item {
            SectionHeader(title = "趋势")
            if (uiState.trendValues.isNotEmpty()) {
                SimpleLineChart(
                    values = uiState.trendValues,
                    modifier = Modifier.padding(top = 10.dp)
                )
            } else {
                Text(
                    text = "暂无数据，完成专注后这里会显示评分趋势。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 10.dp)
                )
            }
        }
        item {
            SectionHeader(title = "记录列表")
        }
        if (uiState.sessions.isEmpty()) {
            item {
                Text(
                    text = "本${if (uiState.period == ReportPeriod.WEEK) "周" else "月"}暂无专注记录。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            items(uiState.sessions, key = { it.id }) { session ->
                SessionListItem(
                    session = session,
                    onClick = { onOpenSessionDetail(session.id) }
                )
            }
        }
    }
}

/** 一条可点击的历史专注记录。 */
@Composable
private fun SessionListItem(
    session: FocusSession,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = session.note ?: "专注记录",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "${formatDuration(session.durationMillis)} · 评分 ${session.score} · 平均噪声 ${session.averageNoiseDb.toInt()} dB",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
```

- [ ] **Step 2: 构建验证**

Run: `$env:JAVA_HOME='E:\jdk21'; .\gradlew.bat :app:assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/ywb/focusguard/ui/screen/ReportsScreen.kt
git commit -m "feat: 报告页接入真实 Room 数据，移除 demo 占位"
```

---

### Task 7: DataStore 替换内存设置

**Files:**
- Create: `app/src/main/java/com/ywb/focusguard/data/repository/DataStoreSettingsRepository.kt`
- Modify: `app/src/main/java/com/ywb/focusguard/di/AppModule.kt`
- Modify: `app/src/main/java/com/ywb/focusguard/data/repository/SettingsRepositoryImpl.kt`（保留但不再绑定）

- [ ] **Step 1: 创建 DataStore 版 SettingsRepository**

```kotlin
package com.ywb.focusguard.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.ywb.focusguard.domain.model.UserSettings
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/** DataStore 扩展属性，全局单例。 */
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "focus_guard_settings")

/**
 * 基于 DataStore Preferences 的设置持久化实现。
 *
 * App 重启后设置不丢失，替换原来的内存版 SettingsRepositoryImpl。
 */
@Singleton
class DataStoreSettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) : SettingsRepository {

    private object Keys {
        val DEFAULT_FOCUS_MINUTES = intPreferencesKey("default_focus_minutes")
        val NOISE_THRESHOLD_DB = floatPreferencesKey("noise_threshold_db")
        val COMFORTABLE_LIGHT_MIN_LUX = floatPreferencesKey("comfortable_light_min_lux")
        val COMFORTABLE_LIGHT_MAX_LUX = floatPreferencesKey("comfortable_light_max_lux")
        val BACKGROUND_MONITORING_ENABLED = booleanPreferencesKey("background_monitoring_enabled")
        val DAILY_REPORT_ENABLED = booleanPreferencesKey("daily_report_enabled")
    }

    /** 持续观察设置变化，DataStore 在内存中缓存，读取高效。 */
    override val settings: Flow<UserSettings> = context.dataStore.data.map { prefs ->
        UserSettings(
            defaultFocusMinutes = prefs[Keys.DEFAULT_FOCUS_MINUTES] ?: 25,
            noiseThresholdDb = prefs[Keys.NOISE_THRESHOLD_DB] ?: 65f,
            comfortableLightMinLux = prefs[Keys.COMFORTABLE_LIGHT_MIN_LUX] ?: 100f,
            comfortableLightMaxLux = prefs[Keys.COMFORTABLE_LIGHT_MAX_LUX] ?: 500f,
            backgroundMonitoringEnabled = prefs[Keys.BACKGROUND_MONITORING_ENABLED] ?: false,
            dailyReportEnabled = prefs[Keys.DAILY_REPORT_ENABLED] ?: true
        )
    }

    override suspend fun updateNoiseThreshold(value: Float) {
        context.dataStore.edit { it[Keys.NOISE_THRESHOLD_DB] = value }
    }

    override suspend fun updateLightRange(min: Float, max: Float) {
        context.dataStore.edit {
            it[Keys.COMFORTABLE_LIGHT_MIN_LUX] = min
            it[Keys.COMFORTABLE_LIGHT_MAX_LUX] = max
        }
    }

    override suspend fun updateDefaultFocusMinutes(minutes: Int) {
        context.dataStore.edit { it[Keys.DEFAULT_FOCUS_MINUTES] = minutes }
    }
}
```

- [ ] **Step 2: 修改 Hilt Module，绑定 DataStore 实现**

在 `AppModule.kt` 的 `RepositoryModule` 中，将绑定从 `SettingsRepositoryImpl` 改为 `DataStoreSettingsRepository`：

```kotlin
// 替换原来的 bindSettingsRepository：
@Binds
@Singleton
abstract fun bindSettingsRepository(repository: DataStoreSettingsRepository): SettingsRepository
```

需要添加 import：`import com.ywb.focusguard.data.repository.DataStoreSettingsRepository`

- [ ] **Step 3: 构建验证**

Run: `$env:JAVA_HOME='E:\jdk21'; .\gradlew.bat :app:assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/ywb/focusguard/data/repository/DataStoreSettingsRepository.kt
git add app/src/main/java/com/ywb/focusguard/di/AppModule.kt
git commit -m "feat: DataStore 替换内存设置，App 重启后设置不丢失"
```

---

### Task 8: SettingsScreen 设置可编辑

**Files:**
- Modify: `app/src/main/java/com/ywb/focusguard/ui/viewmodel/SettingsViewModel.kt`
- Modify: `app/src/main/java/com/ywb/focusguard/ui/screen/SettingsScreen.kt`

- [ ] **Step 1: 检查 SettingsViewModel 是否注入 SettingsRepository 并暴露更新方法**

读取 `SettingsViewModel.kt`，确认是否有 `updateNoiseThreshold`、`updateLightRange`、`updateDefaultFocusMinutes` 方法。如果没有，添加：

```kotlin
fun updateDefaultFocusMinutes(minutes: Int) {
    viewModelScope.launch {
        settingsRepository.updateDefaultFocusMinutes(minutes)
    }
}

fun updateNoiseThreshold(value: Float) {
    viewModelScope.launch {
        settingsRepository.updateNoiseThreshold(value)
    }
}
```

- [ ] **Step 2: 更新 SettingsScreen，让专注时长和噪声阈值可编辑**

修改 `SettingRow` 为可点击触发选择器，或使用简单的 `+/-` 步进器。最小改动：让噪声阈值的 SettingRow 支持点击切换预设值。

- [ ] **Step 3: 修复 SettingsScreen 过时文案**

将 `SettingsScreen.kt:144` 的 "噪声检测、前台服务和性能报告将在后续阶段完成" 改为：
```kotlin
text = "噪声检测、前台服务、报告聚合和设置持久化已完成。"
```

- [ ] **Step 4: 构建验证**

Run: `$env:JAVA_HOME='E:\jdk21'; .\gradlew.bat :app:assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/ywb/focusguard/ui/viewmodel/SettingsViewModel.kt
git add app/src/main/java/com/ywb/focusguard/ui/screen/SettingsScreen.kt
git commit -m "feat: 设置页支持编辑，DataStore 持久化生效"
```

---

### Task 9: 清理过时文案

**Files:**
- Modify: `app/src/main/java/com/ywb/focusguard/ui/screen/TodayScreen.kt`
- Modify: `app/src/main/java/com/ywb/focusguard/data/repository/RepositoryContracts.kt`
- Modify: `app/src/main/java/com/ywb/focusguard/ui/viewmodel/SessionViewModel.kt`
- Modify: `CLAUDE.md`
- Modify: `docs/开发计划.md`
- Modify: `docs/开发进度.md`

- [ ] **Step 1: 修复 TodayScreen.kt:143 过时文案**

将 `"噪声检测将在下一阶段接入"` 改为 `"实时环境监测中"`。

- [ ] **Step 2: 修复 RepositoryContracts.kt:47 过时注释**

将 `"噪声暂时仍是演示数据"` 改为 `"噪声为 AudioRecord 真实采集"`。

- [ ] **Step 3: 修复 SessionViewModel.kt:43 过时注释**

将 `"噪声暂为演示数据"` 改为 `"噪声为 AudioRecord 真实采集"`。

- [ ] **Step 4: 更新 CLAUDE.md 阶段进度表**

将阶段 3-5 标记为已完成，阶段 6 标记为进行中。

- [ ] **Step 5: 更新 docs/开发进度.md**

添加今天的开发记录。

- [ ] **Step 6: 构建验证**

Run: `$env:JAVA_HOME='E:\jdk21'; .\gradlew.bat :app:assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 7: Commit**

```bash
git add -A
git commit -m "docs: 清理过时文案，更新阶段进度表"
```

---

### Task 10: 最终验证

- [ ] **Step 1: 完整构建**

Run: `$env:JAVA_HOME='E:\jdk21'; .\gradlew.bat :app:assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 2: 运行单元测试**

Run: `$env:JAVA_HOME='E:\jdk21'; .\gradlew.bat :app:testDebugUnitTest`
Expected: 所有测试通过

- [ ] **Step 3: 检查 git 状态**

Run: `git status --short`
Expected: 干净或只有预期的未跟踪文件

- [ ] **Step 4: 最终 Commit（如有未提交改动）**

```bash
git add -A
git commit -m "chore: 阶段 6 完成，报告页真实数据 + DataStore 设置 + 文案清理"
```
