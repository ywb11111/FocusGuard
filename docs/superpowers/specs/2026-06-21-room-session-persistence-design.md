# 专注记录 Room 持久化设计

## 目标

把当前内存版专注计时接入 Room：开始专注时创建一条进行中的 `FocusSessionEntity`，结束专注时更新同一条记录，并让 Today / Reports 通过 Room `Flow` 自动刷新。

## 推荐方案

采用 Repository 管理会话生命周期。

- `SessionViewModel` 负责计时状态和用户事件。
- `FocusRepository` 负责把开始、结束动作转换成数据库写入。
- `FocusSessionDao` 负责具体 SQL 查询和更新。
- `FocusSessionMapper` 负责 `FocusSessionEntity` 与领域模型 `FocusSession` 互转。

## 数据流

```text
SessionScreen 点击开始
        ↓
SessionViewModel.startSession()
        ↓
FocusRepository.startSession(config)
        ↓
Room 插入进行中记录，返回真实 sessionId
        ↓
SessionViewModel 用该 id 进入 Running

SessionScreen 点击结束
        ↓
SessionViewModel.finishSession()
        ↓
FocusRepository.finishSession(sessionId)
        ↓
Room 更新同一条记录
        ↓
Today / Reports 通过 observeSessions() 自动刷新
```

## 当前取舍

- 暂不引入传感器采样，噪声、光照、移动次数先使用当前环境快照或默认值。
- 暂不新建 ActiveSession 表，避免第一版 Room 学习过早复杂化。
- `SessionDetailScreen` 仍可后续单独接真实详情，本次先完成列表和统计闭环。

## 验证方式

- mapper 单元测试验证领域模型与数据库模型转换。
- `.\gradlew.bat :app:assembleDebug` 验证编译。
- 手动运行 App 后，完成一次专注，Today 和 Reports 应能看到真实 Room 记录。
