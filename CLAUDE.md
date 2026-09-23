# FocusGuard 项目协作规范

> 本文件为 Claude Code 在 FocusGuard 项目中的协作指南。任何开发任务前应优先阅读本文件。

## 用户背景

- 软件工程大三学生，Android 方向，Kotlin + Jetpack Compose 技术路线
- 正在实习，使用 AI 辅助开发，但希望掌握关键决策和底层逻辑
- 第一个项目是通知栏/无障碍自动记账方向
- FocusGuard 是第二个项目，目标是日常可用、现场可展示、技术深度扎实
- 每晚约有 2 小时学习和开发时间
- **目标：冲击 1w+ 月薪的 Android 岗位**

## 项目定位

FocusGuard 是专注环境助手 App，不是普通番茄钟，而是结合手机麦克风、光照传感器、加速度计和系统后台能力，在专注过程中实时监测环境噪声、光照和手机移动状态。

**技术核心点**：
- SensorManager（光照、加速度传感器）
- AudioRecord（噪声检测）
- Foreground Service（后台监测）
- Room（本地持久化）
- DataStore（设置存储）
- WorkManager（后台任务）
- StateFlow/SharedFlow（状态管理）
- Compose Canvas 性能优化

## 当前进度

| 阶段 | 状态 | 说明 |
|------|------|------|
| 阶段 1：基础骨架 | ✅ 已完成 | 导航、分层、依赖配置 |
| 阶段 2：专注计时 + Room | ✅ 已完成 | 会话持久化、今日统计、详情页 |
| 阶段 3：光照 + 移动 | ✅ 已完成 | 传感器已接入，MotionEventDetector 防抖 |
| 阶段 4：噪声检测 | ✅ 已完成 | AudioRecord 采集 + RMS + 相对 dB |
| 阶段 5：后台服务 | ✅ 已完成 | 前台服务 + 通知栏控制 + SharedFlow 通信 |
| 阶段 6：报告优化 | ✅ 已完成 | 周/月聚合、DataStore 设置、趋势图 |

## 协作原则

### 1. 语言偏好

- 对话默认中文
- 代码注释、文档、说明使用中文
- 保留代码标识符、API 名称、库名等必要英文

### 2. 教学为主

用户希望在 AI 辅助中真正学会 Android 工程能力。实现关键模块时：

- 解释数据流、架构边界、关键代码和面试讲法
- 生成代码时必须有对应的中文学习注释
- 不只给代码，要帮助用户理解"为什么这样设计"

### 3. 动手边界

**建议用户亲自动手**（AI 讲解、审查、提示，但不直接全包）：
- ViewModel 状态流和计时状态机
- Room DAO 基础查询
- SensorManager 注册、监听、注销
- callbackFlow 把传感器回调转成 Flow
- AudioRecord 读取 PCM buffer 和 RMS 计算
- Foreground Service 通知渠道创建

**AI 可生成代码，用户负责理解**：
- Compose 页面布局和组件
- Room 查询样板和测试样板
- Hilt Module 绑定
- 文档整理
- 图表样式

### 4. 小步提交

- 每完成一个小功能闭环就提醒用户 Git 提交
- 提交前检查：能编译、无无关文件、文档注释已同步
- 提交格式：`类型: 简短描述`（feat/fix/docs/refactor/test/build/chore）

## 代码注释规范

AI 新增或修改代码时，**必须同步补充中文学习注释**：

### 注释要求

1. **类/接口/重要 data class**：说明在架构中的职责，由谁创建、被谁消费

2. **关键属性**：说明用途，特别是 StateFlow、Job、计时字段、阈值、时间窗口、数据库字段

3. **公开方法和核心私有方法**：说明调用时机、输入输出、副作用，涉及生命周期必须说明资源何时申请、何时释放

4. **重点解释"为什么这样设计"**，不只是翻译代码表面行为

5. **修改逻辑时同步检查旧注释**，删除或修正过期说明

### 注释示例

```kotlin
/**
 * 光照传感器数据源
 * 
 * 职责：封装 SensorManager.TYPE_LIGHT，通过 callbackFlow 暴露实时光照数据
 * 生命周期：由 ViewModel 通过 collect 触发，Flow 停止时自动释放监听器
 * 
 * @param context Application Context，用于获取 SensorManager 系统服务
 */
@Singleton
class LightSensorDataSource @Inject constructor(
    @ApplicationContext private val context: Context
) {
    /**
     * 观察实时光照数据
     * 
     * 实现：使用 callbackFlow 将 SensorEventListener 回调转为 Flow
     * 释放：当 Flow 被 cancel 时，awaitClose 块执行 unregisterListener
     * 
     * @return Flow<LightSample> 每次传感器变化时发射一个样本
     */
    fun observeLight(): Flow<LightSample> = callbackFlow {
        // ...
    }
}
```

## 开发流程

### 每次开发前回答

1. 当前要做的是哪个阶段？
2. 本次改动涉及哪些模块？
3. 是否会影响已有架构边界？
4. 是否需要新增依赖、权限或系统能力？
5. 完成后如何验证？

### 开发后完成

1. 运行构建验证：`.\gradlew.bat :app:assembleDebug`
2. 更新 `docs/开发进度.md`
3. 补充必要代码注释
4. 告知用户是否形成小功能闭环
5. 如形成闭环，提醒 Git 提交

## AI 禁止事项

- 为了快而绕过架构边界
- 把所有逻辑塞进 Activity/Composable/ViewModel
- 未说明原因就引入大型第三方库
- 生成无法编译的代码后不验证
- 在核心模块只给代码不给解释
- 把演示数据说成真实采样数据
- 忽略权限拒绝、传感器缺失、后台限制等真实 Android 问题

## 相关文档

| 文档 | 用途 |
|------|------|
| `docs/FocusGuard项目方案.md` | 完整项目方案，功能、技术栈、架构设计 |
| `docs/开发计划.md` | 6 个阶段的计划和状态 |
| `docs/开发进度.md` | 按日期记录开发活动 |
| `docs/项目背景与发现.md` | 项目简介和注意事项 |
| `Git提交规范.md` | Git 提交格式和时机 |

## 冲击 1w+ 月薪的差距分析

### 当前短板

1. **项目完成度不够** — ~~阶段 3-6 未完成，半成品是负资产~~ 已全部完成
2. **测试覆盖仍需扩展** — 已有 25 个单元测试与 5 个 Compose 截图回归，下一步补充真机/集成测试
3. **性能优化未实践** — Compose 重组、Canvas 性能、内存泄漏
4. **代码未提交** — ~~61 个文件堆着，git log 看不到进度~~ 已清理

### P0 必须完成

- [x] 阶段 3 收尾：Session 期间记录采样，详情页曲线切真实数据
- [x] 阶段 4：AudioRecord 噪声检测
- [x] 阶段 5：前台服务 + 通知
- [x] 提交代码：清理 git 状态

### P1 强烈建议

- [x] DataStore 替换内存设置
- [x] 写测试：DAO、Repository、Analyzer
- [ ] 性能优化并记录
- [ ] 真机校准传感器阈值

## 环境注意

- 当前可用 JDK：`E:\environment\jdk17`
- PowerShell 临时验证命令：`$env:JAVA_HOME='E:\environment\jdk17'`
- Compose Preview Screenshot Testing 可在没有模拟器时生成 390 × 844 dp 的真实页面渲染图
