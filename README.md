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
| 阶段 2：专注计时 + Room | ✅ 基本完成 | 会话持久化、今日统计、详情页 |
| 阶段 3：光照 + 移动 | 🔧 进行中 | 传感器已接入，待完成采样记录 |
| 阶段 4：噪声检测 | ❌ 未开始 | AudioRecord + 权限 |
| 阶段 5：后台服务 | ❌ 未开始 | 前台服务 + 通知 |
| 阶段 6：报告优化 | ❌ 未开始 | 周报、趋势图、性能优化 |

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
# 设置 JDK 21 环境
$env:JAVA_HOME = 'E:\jdk21'

# 构建
.\gradlew.bat :app:assembleDebug
```

## 下一步

优先完成阶段 3：Session 期间记录采样到数据库，详情页曲线切换为真实数据。
