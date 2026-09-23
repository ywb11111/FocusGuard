# FocusGuard 1.0 设计说明

## 设计目标

FocusGuard 的核心不是“倒计时”，而是让用户在开始前判断环境、在过程中保持节奏、结束后看懂影响专注的因素。因此信息层级固定为：环境结论 → 主操作 → 环境证据 → 今日反馈。

## 视觉语言

- 主色使用低刺激青绿色，表示稳定、可开始和安全状态；蓝色仅用于导航选中和少量信息强调。
- 背景使用暖白与浅灰绿，卡片以留白和轻微层次区分，不依赖重阴影。
- 大标题和关键数字使用高对比深墨色，解释文案降低对比度，保证快速扫读。
- 图标统一使用 Material Icons，不使用 emoji，避免跨设备字形不一致。

## 关键交互决策

- 底部导航只保留 Today、Reports、Settings；Session 是一次沉浸任务，不占长期 Tab。
- Today 只保留一个高强调主按钮，降低开始专注前的选择成本。
- Session 在开始前允许选择 25/45/60 分钟，并复用用户在 Settings 中保存的默认时长。
- 传感器无数据时显示“检测中”，不把缺失数据包装成积极结论。
- 报告图表只展示 Room 中的真实记录；首页未加入设计稿中的装饰性迷你曲线，避免误导。

## 验证方式

页面由 Compose Preview Screenshot Testing 在 390 × 844 dp 视口中真实渲染，截图基线进入项目版本控制。视觉回归命令：

```powershell
$env:JAVA_HOME = 'E:\environment\jdk17'
.\gradlew.bat :app:validateDebugScreenshotTest
```
