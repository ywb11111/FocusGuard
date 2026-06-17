# FocusGuard：专注环境助手项目方案

> 面向 Android 方向求职展示的 Kotlin + Jetpack Compose 项目方案。

## 1. 项目定位

### 一句话介绍

FocusGuard 是一个基于手机传感器的专注与环境监测 App，帮助用户在学习、工作、睡前场景中记录环境噪声、光照、手机移动、应用使用情况，并生成可视化报告。

### 面试版本介绍

这是一个 Kotlin + Jetpack Compose 开发的 Android 应用，使用 `SensorManager`、`AudioRecord`、`Foreground Service`、`Room`、`DataStore`、`WorkManager` 等能力，实现专注计时、环境噪声采集、光照检测、手机移动检测、应用使用统计、后台提醒和数据可视化。

项目重点在：

- 传感器采样策略
- 音频采集与噪声分析
- Android 后台服务与通知
- 本地数据持久化
- Compose 实时图表性能优化
- 权限与系统能力适配

### 核心亮点

- 日常可用：可以真的用来学习、睡前检测环境。
- 现场可展示：拍桌子、遮挡光线、晃手机，数据实时变化。
- 技术有深度：音频、传感器、后台服务、通知、权限、性能优化。
- 不依赖硬件：只用手机即可完成。

## 2. 用户场景

### 场景一：学习专注

用户点击“开始专注”，App 开始记录：

- 专注时长
- 环境噪声
- 光照强度
- 手机移动次数
- 是否频繁切出 App
- 是否收到干扰通知，可选

结束后生成一次专注报告。

### 场景二：睡前环境检测

用户点击“睡前检测”，App 检测 3 到 10 分钟：

- 当前房间是否太亮
- 环境是否过吵
- 手机是否频繁移动
- 给出睡前环境评分

### 场景三：每日回顾

App 自动生成日报：

- 今日专注总时长
- 平均噪声
- 最安静时间段
- 手机拿起次数
- 学习环境评分
- 趋势图

## 3. 功能模块

### MVP 版本

- 专注计时
- 环境噪声检测
- 光照检测
- 手机移动检测
- 专注记录保存
- 今日概览
- 历史记录列表
- 简单图表
- 通知提醒

### 进阶版本

- 前台服务后台监测
- 使用情况统计 `UsageStatsManager`
- DataStore 设置页
- 每日报告 `WorkManager`
- 采样频率动态调整
- Compose Canvas 自绘实时曲线
- 性能优化报告

### 面试加分版本

- Macrobenchmark 测启动时间
- Baseline Profile
- JankStats 监控卡顿
- Android Studio Profiler 分析耗电与内存
- 传感器采样策略文档
- 项目技术总结 Markdown

## 4. 推荐技术栈

| 层级 | 技术 |
|---|---|
| 语言 | Kotlin |
| UI | Jetpack Compose + Material3 |
| 架构 | MVVM + Repository |
| 状态 | StateFlow / SharedFlow |
| 异步 | Coroutine |
| 本地数据库 | Room |
| 设置存储 | DataStore |
| 依赖注入 | Hilt |
| 后台任务 | WorkManager |
| 后台监测 | Foreground Service |
| 传感器 | SensorManager |
| 音频 | AudioRecord |
| 权限 | 手写权限处理或 Accompanist Permissions |
| 图表 | Compose Canvas 自绘，或 Vico |
| 性能 | Macrobenchmark / Baseline Profile / Android Studio Profiler |

## 5. 需要重点掌握的基础

### 必须掌握

1. Activity / Service 生命周期
   - 特别是前台服务为什么需要通知，什么时候启动和停止。

2. 协程和 Flow
   - 理解 `StateFlow`、`SharedFlow`、`callbackFlow`、`viewModelScope`、`CoroutineScope`。

3. Room 基础
   - Entity、Dao、Database、Repository、Flow 查询。

4. Compose 状态管理
   - `remember`、`LaunchedEffect`、`collectAsStateWithLifecycle`、重组原因。

5. 权限机制
   - 录音权限、通知权限、传感器权限、使用情况访问权限。

6. Android 后台限制
   - Android 8+ 后台服务限制、前台服务类型、通知渠道。

### 建议深入

1. AudioRecord 原理
   - PCM 是什么，采样率是什么，buffer size 为什么重要。

2. SensorManager 原理
   - 采样频率、注册/注销监听、不同传感器的事件含义。

3. 性能优化
   - 为什么高频数据不能直接每秒几十次触发 Compose 全页面重组。

4. 架构设计
   - 数据源、Repository、UseCase、ViewModel 的边界。

### 可以让 AI 多辅助

- UI 页面代码
- Room DAO 模板
- Compose 组件拆分
- 单元测试样板
- 文档整理
- 权限弹窗文案
- 图表样式打磨

## 6. 整体架构

推荐使用分层架构：

```text
app
├── data
│   ├── local
│   │   ├── entity
│   │   ├── dao
│   │   └── database
│   ├── datastore
│   ├── sensor
│   ├── audio
│   └── repository
├── domain
│   ├── model
│   ├── usecase
│   └── analyzer
├── service
│   ├── FocusMonitorService
│   └── DailyReportWorker
├── ui
│   ├── navigation
│   ├── screen
│   ├── component
│   ├── theme
│   └── state
└── di
```

如果想降低第一阶段复杂度，可以先不建完整 `domain` 层，但第二个项目建议使用。面试时更容易讲清楚工程边界。

### 数据流

```text
SensorManager / AudioRecord / UsageStats
        ↓
DataSource
        ↓
Repository
        ↓
UseCase / Analyzer
        ↓
ViewModel
        ↓
UiState
        ↓
Compose Screen
```

### 架构原则

- UI 不直接碰 `SensorManager` 和 `AudioRecord`。
- ViewModel 不直接操作数据库。
- Service 不直接控制 UI。
- Repository 负责协调数据源。
- Analyzer 负责计算评分、噪声等级、分心次数等业务逻辑。

## 7. 核心模块设计

### 7.1 专注会话模块

领域模型：

```kotlin
data class FocusSession(
    val id: Long,
    val startTime: Long,
    val endTime: Long?,
    val durationMillis: Long,
    val averageNoiseDb: Float,
    val maxNoiseDb: Float,
    val averageLightLux: Float,
    val movementCount: Int,
    val distractionCount: Int,
    val score: Int,
    val note: String?
)
```

作用：

- 记录一次学习或工作专注过程。
- 结束时生成评分。
- 首页和报告页都依赖它。

需要掌握：

- Room Entity
- 时间戳处理
- Flow 查询
- 聚合统计

### 7.2 噪声检测模块

使用 `AudioRecord`。

流程：

```text
申请 RECORD_AUDIO 权限
        ↓
创建 AudioRecord
        ↓
循环读取 PCM buffer
        ↓
计算 RMS
        ↓
转换为近似 dB
        ↓
输出 NoiseSample
```

模型：

```kotlin
data class NoiseSample(
    val timestamp: Long,
    val decibel: Float,
    val level: NoiseLevel
)

enum class NoiseLevel {
    QUIET, NORMAL, NOISY, LOUD
}
```

注意：手机麦克风算出来的不是专业分贝仪，只能叫“相对噪声水平”。面试时要诚实说明，这是工程成熟度。

需要掌握：

- PCM 基础
- buffer 大小
- 后台线程
- 权限处理
- 高频数据节流

AI 可以辅助：

- `AudioRecord` 封装类
- RMS 计算函数
- Compose 波形图

### 7.3 光照检测模块

使用 `SensorManager` 的 `TYPE_LIGHT`。

模型：

```kotlin
data class LightSample(
    val timestamp: Long,
    val lux: Float,
    val level: LightLevel
)

enum class LightLevel {
    DARK, DIM, COMFORTABLE, BRIGHT
}
```

初始阈值：

```text
0-10 lux: DARK
10-100 lux: DIM
100-500 lux: COMFORTABLE
500+ lux: BRIGHT
```

注意：不同手机光线传感器差异很大，所以阈值应允许用户调整。

需要掌握：

- SensorEventListener
- 传感器注册和注销
- 生命周期绑定
- 采样频率选择

### 7.4 手机移动检测模块

使用加速度计 `TYPE_ACCELEROMETER`。

思路：

```text
读取 x/y/z
计算加速度模长
和上一次平滑值比较
超过阈值记为一次移动
```

模型：

```kotlin
data class MotionSample(
    val timestamp: Long,
    val magnitude: Float,
    val isSignificantMove: Boolean
)
```

用途：

- 判断手机是否频繁被拿起。
- 判断专注期间是否有明显干扰。
- 判断睡前手机是否放稳。

需要掌握：

- 向量模长
- 简单低通滤波
- 防抖
- 事件合并

### 7.5 专注评分模块

简单可解释的评分系统：

```text
初始分 100
噪声过高扣 0-25
光照不适扣 0-20
移动次数过多扣 0-20
切出 App 或使用其他 App 扣 0-25
专注时长过短扣 0-10
最低 0，最高 100
```

模型：

```kotlin
data class FocusScore(
    val total: Int,
    val noiseScore: Int,
    val lightScore: Int,
    val motionScore: Int,
    val distractionScore: Int,
    val suggestions: List<String>
)
```

面试时可以强调：评分不是黑盒 AI，而是规则可解释，便于调试和迭代。

## 8. 页面设计

工具型 App 的 UI 要克制、清晰、可扫描。建议底部导航 4 个 Tab：

```text
Today      Session      Reports      Settings
今日        专注          报告          设置
```

### 8.1 今日页 TodayScreen

目标：打开 App 第一眼知道现在环境适不适合学习。

内容：

- 顶部当前状态：适合专注 / 偏吵 / 光线偏暗 / 手机活动频繁
- 今日专注总时长
- 当前噪声
- 当前光照
- 当前移动状态
- 最近一次专注记录
- 开始专注按钮

布局：

```text
[状态标题]
当前适合专注 / 环境偏吵

[实时指标区]
噪声 42 dB    光照 180 lux    移动 稳定

[今日概览]
专注 2h 30m    平均评分 86    分心 4 次

[最近记录]
20:00 - 20:45  评分 88

[主按钮]
开始专注
```

UI 建议：

- 背景用浅色或深色都可以，但不要过度渐变。
- 指标用紧凑卡片。
- 状态颜色明确：绿色、黄色、红色，但面积不要太大。
- 开始专注按钮固定在底部或明显位置。

### 8.2 专注页 SessionScreen

目标：正在专注时减少干扰，信息少但关键。

未开始状态：

```text
[专注模式]
25 min / 45 min / 自定义

[环境预检]
噪声 正常
光照 舒适
手机 稳定

[开始专注]
```

进行中状态：

```text
[倒计时]
24:31

[当前环境]
噪声曲线
光照状态
移动次数

[暂停] [结束]
```

设计重点：

- 进行中页面要安静，不要放太多按钮。
- 倒计时最大。
- 噪声曲线可以是细线图。
- 结束时弹出总结，不要直接跳走。

### 8.3 报告页 ReportsScreen

目标：展示历史趋势和可解释总结。

内容：

- 周/月切换
- 专注时长趋势
- 平均评分趋势
- 噪声趋势
- 光照舒适度分布
- 历史记录列表

布局：

```text
[本周总结]
总专注 12h 20m
平均评分 82
最佳时段 晚上 20:00

[趋势图]
专注时长折线
噪声柱状/折线

[记录列表]
今天 20:00 45min 评分88
昨天 19:30 30min 评分76
```

需要掌握：

- Room 聚合查询
- 日期分组
- 图表绘制
- LazyColumn 性能

### 8.4 详情页 SessionDetailScreen

点击某条记录进入。

内容：

- 本次专注时长
- 开始/结束时间
- 评分拆解
- 噪声曲线
- 光照曲线
- 移动事件时间点
- 建议

评分拆解示例：

```text
总分 84

噪声：-8
20:13 到 20:18 环境偏吵

光照：-4
整体略暗

移动：-6
检测到 3 次明显移动
```

这个页面是面试展示重点，因为它能体现“数据采集 -> 分析 -> 可视化 -> 建议”。

### 8.5 设置页 SettingsScreen

内容：

- 专注默认时长
- 噪声提醒阈值
- 光照舒适范围
- 是否开启后台监测
- 是否开启每日总结
- 权限状态
- 数据导出
- 关于项目

权限状态示例：

```text
录音权限：已开启
通知权限：未开启
使用情况访问：未授权
```

点击未授权，跳系统设置页。

## 9. 页面跳转逻辑

推荐使用 Navigation Compose。

路由：

```kotlin
sealed class Destination(val route: String) {
    data object Today : Destination("today")
    data object Session : Destination("session")
    data object Reports : Destination("reports")
    data object Settings : Destination("settings")
    data object SessionDetail : Destination("session/{sessionId}") {
        fun createRoute(sessionId: Long) = "session/$sessionId"
    }
    data object PermissionGuide : Destination("permission_guide")
    data object Onboarding : Destination("onboarding")
}
```

导航关系：

```text
Onboarding
   ↓
Today

Today
   ├── Session
   ├── SessionDetail
   └── Settings

Session
   ├── SessionDetail, after finish
   └── Today

Reports
   └── SessionDetail

Settings
   └── PermissionGuide
```

启动逻辑：

```text
App 启动
  ↓
判断是否第一次打开
  ↓
是：Onboarding
否：Today
```

权限逻辑：

```text
进入 Today
  ↓
检查录音 / 通知 / 使用情况访问
  ↓
关键权限缺失时显示非阻塞提示
```

注意：不要一进 App 就连续弹权限。先解释为什么需要，再引导授权。

## 10. 数据库设计

建议先设计 4 张表。

### FocusSessionEntity

保存一次专注记录。

```kotlin
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
    val note: String?
)
```

### NoiseSampleEntity

```kotlin
@Entity(tableName = "noise_samples")
data class NoiseSampleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sessionId: Long,
    val timestamp: Long,
    val decibel: Float
)
```

### LightSampleEntity

```kotlin
@Entity(tableName = "light_samples")
data class LightSampleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sessionId: Long,
    val timestamp: Long,
    val lux: Float
)
```

### MotionEventEntity

```kotlin
@Entity(tableName = "motion_events")
data class MotionEventEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sessionId: Long,
    val timestamp: Long,
    val magnitude: Float
)
```

后续如果加 UsageStats，可以增加：

```text
AppUsageEventEntity
DailyReportEntity
```

## 11. Repository 设计

```kotlin
interface FocusRepository {
    fun observeTodaySummary(): Flow<TodaySummary>
    fun observeSessions(): Flow<List<FocusSession>>
    fun observeSessionDetail(sessionId: Long): Flow<SessionDetail>
    suspend fun startSession(config: FocusConfig): Long
    suspend fun finishSession(sessionId: Long): FocusSession
    suspend fun saveNoiseSample(sample: NoiseSample)
    suspend fun saveLightSample(sample: LightSample)
    suspend fun saveMotionEvent(event: MotionEvent)
}
```

传感器相关：

```kotlin
interface EnvironmentRepository {
    fun observeNoise(): Flow<NoiseSample>
    fun observeLight(): Flow<LightSample>
    fun observeMotion(): Flow<MotionSample>
    fun observeEnvironmentSnapshot(): Flow<EnvironmentSnapshot>
}
```

设置相关：

```kotlin
interface SettingsRepository {
    val settings: Flow<UserSettings>
    suspend fun updateNoiseThreshold(value: Float)
    suspend fun updateLightRange(min: Float, max: Float)
    suspend fun updateDefaultFocusMinutes(minutes: Int)
}
```

## 12. Service 设计

### FocusMonitorService

职责：

- 专注期间后台持续监测
- 维护前台通知
- 采集噪声、光照、移动数据
- 达到阈值时提醒
- 专注结束时写入数据库

注意：

- Android 8+ 后台服务限制要理解。
- 前台服务必须显示通知。
- Android 13+ 需要通知权限。
- 录音涉及隐私，UI 上要明确说明用途。

通知内容：

```text
FocusGuard 正在监测学习环境
已专注 23 分钟 · 噪声正常 · 光线舒适
[暂停] [结束]
```

## 13. 状态设计

### TodayUiState

```kotlin
data class TodayUiState(
    val isLoading: Boolean = true,
    val environment: EnvironmentSnapshot? = null,
    val todaySummary: TodaySummary? = null,
    val latestSession: FocusSession? = null,
    val permissionState: PermissionState = PermissionState(),
    val activeSession: ActiveSession? = null,
    val errorMessage: String? = null
)
```

### SessionUiState

```kotlin
sealed interface SessionUiState {
    data object Idle : SessionUiState

    data class Ready(
        val config: FocusConfig,
        val environment: EnvironmentSnapshot
    ) : SessionUiState

    data class Running(
        val sessionId: Long,
        val elapsedMillis: Long,
        val remainingMillis: Long?,
        val noiseSamples: List<NoiseSample>,
        val lightLevel: LightLevel,
        val movementCount: Int
    ) : SessionUiState

    data class Finished(
        val session: FocusSession
    ) : SessionUiState
}
```

## 14. UI 视觉方向

这是工具型 App，建议走：

```text
冷静、清晰、有一点科技感，但不要黑乎乎一片。
```

### 颜色建议

- 背景：接近白色或非常浅的冷灰
- 主色：青绿或蓝绿
- 强提醒：橙色
- 危险：红色
- 文字：高对比深灰
- 图表：不同指标用固定颜色

指标颜色示例：

```text
Noise: 蓝色
Light: 黄色/琥珀
Motion: 紫色
Focus score: 青绿色
Warning: 橙色
```

### 组件风格

- 卡片圆角控制在 8 到 12dp。
- 不要大量阴影。
- 用分组、线条、间距建立层级。
- 数字指标要清楚。
- 图表不要花，重点是读得懂。

### 动效建议

- 开始专注时轻微状态切换。
- 实时曲线平滑更新。
- 分数生成时简单数字滚动。
- 避免大面积炫酷动画。

## 15. 开发顺序

### 阶段 1：基础 App 骨架

目标：能跑起来，有导航和页面。

任务：

- 新建项目或基于现有项目改
- 配置 Compose、Hilt、Room、DataStore
- 搭好 Navigation
- 做 Today / Session / Reports / Settings 空页面
- 做主题和基础组件

你要学：

- Compose Navigation
- Hilt 注入
- 项目分包

AI 可辅助：

- 生成页面骨架
- 生成主题代码

### 阶段 2：专注计时 + 本地记录

目标：不用传感器，也能完成一次专注记录。

任务：

- 开始专注
- 暂停/结束
- 写入 Room
- 今日统计
- 历史列表
- 详情页

你要学：

- ViewModel 状态流
- Room 关系
- 时间计算
- 页面跳转传参

### 阶段 3：光照 + 移动检测

目标：传感器数据实时显示。

任务：

- 封装 `LightSensorDataSource`
- 封装 `MotionSensorDataSource`
- Today 页实时显示
- Session 页记录采样
- 结束后生成平均光照和移动次数

你要学：

- SensorManager
- callbackFlow
- 生命周期释放

### 阶段 4：噪声检测

目标：用麦克风采集环境音量。

任务：

- 申请录音权限
- 封装 `NoiseMonitor`
- 计算 RMS 和相对 dB
- 实时显示噪声
- 记录专注期间噪声样本

你要学：

- AudioRecord
- PCM
- 后台线程
- 数据节流

### 阶段 5：后台服务 + 通知

目标：App 切后台也能监测。

任务：

- 创建 `FocusMonitorService`
- 前台通知
- 通知按钮：暂停/结束
- 后台采样降频
- 异常提醒

你要学：

- Foreground Service
- Notification Channel
- PendingIntent
- Android 后台限制

### 阶段 6：报告和优化

目标：项目可展示、可讲。

任务：

- 周报/月报
- 趋势图
- 评分拆解
- 设置阈值
- 性能优化
- 写项目技术总结

你要学：

- Room 聚合查询
- Compose Canvas 性能
- Macrobenchmark 基础
- Profiler 使用

## 16. 面试讲法

可以这样介绍项目：

```text
我的第二个项目是 FocusGuard，一个专注环境助手。它不是单纯番茄钟，而是结合手机麦克风、光照传感器、加速度计和系统后台能力，在专注过程中实时监测环境噪声、光照和手机移动状态。

应用使用 Kotlin + Compose 开发，整体采用 MVVM + Repository 架构，传感器和音频采集通过 Flow 暴露给 ViewModel，Room 保存专注记录和采样数据，Foreground Service 保证后台监测稳定运行。

项目后期我重点优化了高频采样数据导致的 Compose 重组问题，并用采样降频、数据窗口和 Canvas 局部绘制改善实时曲线性能。
```

## 17. 最小可交付版本

如果时间紧，至少做到这些：

1. Today 首页
2. 专注计时
3. 噪声检测
4. 光照检测
5. 移动检测
6. Room 保存记录
7. Session 详情页
8. 周统计报告
9. 前台服务通知
10. 技术总结文档

做到这 10 个点，就已经比大多数学生项目扎实。

## 18. 开发建议

这个项目最重要的不是堆功能，而是证明：

1. 能设计一个完整 App。
2. 能处理 Android 系统能力。
3. 能解决高频数据、后台任务、权限、性能这些真实问题。
4. 能解释自己的架构，而不是只说“AI 帮我写的”。

可以大胆用 AI 提效，但下面这些地方建议亲自搞懂并能手写或讲清楚：

- `AudioRecord` 如何采样
- `SensorManager` 如何监听和释放
- `callbackFlow` 如何把回调转成 Flow
- 前台服务为什么需要通知
- 高频数据为什么会导致 Compose 卡顿
- Repository 为什么隔离数据源
- Room 表为什么这样设计
- 权限拒绝后 App 怎么降级

如果这个项目完成，简历项目会从“会写 App”提升到“理解 Android 运行机制和工程权衡”。这对安卓方向很有价值。
