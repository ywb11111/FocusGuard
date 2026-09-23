# FocusGuard

专注环境助手 — 面向 Android 求职展示的 Kotlin + Jetpack Compose 项目。

## 简介

FocusGuard 不是普通番茄钟，而是结合手机麦克风、光照传感器、加速度计和 Android 后台能力，记录用户专注过程中的噪声、光照、手机移动和分心情况，并生成可视化报告。

## 项目目标

- 日常可用的学习/工作专注辅助 App
- 面试现场可展示环境数据变化和专注报告
- 掌握 Kotlin、Compose、StateFlow、Room、DataStore、Hilt、WorkManager、SensorManager、AudioRecord、Foreground Service、权限适配、性能优化
- 能讲清楚架构设计、数据流、系统限制和工程取舍

## 当前进度

| 阶段 | 状态 | 说明 |
|------|------|------|
| 阶段 1：基础骨架 | ✅ 已完成 | 导航、分层、依赖配置 |
| 阶段 2：专注计时 + Room | ✅ 已完成 | 会话持久化、今日统计、详情页 |
| 阶段 3：光照 + 移动 | ✅ 已完成 | 真实传感器采样与移动事件防抖 |
| 阶段 4：噪声检测 | ✅ 已完成 | AudioRecord + RMS 相对音量分析 |
| 阶段 5：后台服务 | ✅ 已完成 | 前台服务 + 通知栏控制 |
| 阶段 6：报告优化 | ✅ 已完成 | 周/月报告、趋势图、DataStore 设置 |
| 1.0 体验升级 | ✅ 已完成 | 首次引导、信息架构与全页面视觉重构 |

## 技术栈

- **语言**: Kotlin 2.2.10
- **UI**: Jetpack Compose + Material 3
- **架构**: MVVM + Repository
- **状态管理**: StateFlow / SharedFlow
- **本地数据库**: Room
- **设置存储**: DataStore
- **依赖注入**: Hilt
- **后台任务**: WorkManager
- **后台监测**: Foreground Service
- **传感器**: SensorManager (光照 + 加速度计)
- **音频**: AudioRecord (噪声检测)

## 文档

| 文档 | 用途 |
|------|------|
| `CLAUDE.md` | Claude Code 协作指南 |
| `Git提交规范.md` | Git 提交格式和时机 |
| `docs/FocusGuard项目方案.md` | 完整项目方案 |
| `docs/开发计划.md` | 阶段计划和状态 |
| `docs/项目背景与发现.md` | 背景和注意事项 |
| `docs/开发进度.md` | 开发活动记录 |

## 构建

```powershell
# 使用本机可用的 JDK 17
$env:JAVA_HOME = 'E:\environment\jdk17'

# 构建
.\gradlew.bat :app:assembleDebug

# 单元测试 + Compose 页面截图回归
.\gradlew.bat :app:testDebugUnitTest :app:validateDebugScreenshotTest
```

## 下一步

优先进行真机体验验收：校准不同机型的相对音量阈值，验证锁屏/切后台后的专注会话，并根据真实使用反馈调整报告洞察。
