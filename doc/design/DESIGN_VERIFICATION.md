# 前端设计交付与验证记录

- 日期：2026-09-20。
- 本轮目标：检索同类待办 App 页面，形成 TodoInk 页面方案和可审核预览。
- 文档交付完成；Android 实施未启动，设计仍为 v0.1 待审核。
- 产物：[方案](TodoInk_前端设计方案.md)、[可点击预览](todoink-preview.html)、[四页面总览](page-overview.png)。

## 研究依据

搜索并读取 Todoist、Things、TickTick 官方功能与帮助页面，来源链接、观察与设计推断分别登记在方案第 2 节。使用本机 Chrome 无头浏览器实际查看了官网及其页面展示图片；没有把官网宣传图视为所有当前客户端的保证。

内置浏览器在研究时两次超时；改用本机 Chrome + Playwright 成功读取。Playwright 默认随附浏览器不存在，使用已安装 Chrome，无新增安装。参考截图仅供研究，未作为产品素材打包进预览。

## 浏览器验证

验证调用预览实际页面和交互，不修改 Android 文件或数据库。脚本：[check-preview.cjs](check-preview.cjs)。最后验证退出码 0，未出现 pageerror。

可复现命令（本机已安装 Chrome）：

```powershell
$env:TODOINK_PLAYWRIGHT = 'C:\Users\MSI\.cache\codex-runtimes\codex-primary-runtime\dependencies\node\node_modules\playwright'
& 'C:\Users\MSI\.cache\codex-runtimes\codex-primary-runtime\dependencies\node\bin\node.exe' .\doc\design\check-preview.cjs
```

脚本重新生成下列设计截图，使用独立无头浏览器，关闭后不保留演示修改。其他环境需提供 Playwright 模块及 Chrome，不能将此命令当成 Gradle / 设备验证。

| 检查 | 实际结果 |
| --- | --- |
| 初始 3 条候选，确认后移除一条 | PASS |
| “下班前”未选择截止方式时阻止确认 | PASS |
| 明确选“不设截止”，隐藏日期 / 时刻字段 | PASS |
| 编辑标题后确认，全部待办出现编辑后的内容 | PASS |
| 完成任务后，可在已完成列表找到 | PASS |
| 忽略候选后数量减少 | PASS |
| 来源筛选“微信”显示 2 条合成通知 | PASS |
| 来源搜索“QQ”只显示匹配项 | PASS |
| 来源开关更新 aria-checked | PASS |
| 本地候选识别开关独立切换 | PASS |
| 全空白标题阻止确认 | PASS |
| 仅日期精度隐藏时刻字段 | PASS |
| 主题切换与页面导航无脚本错误 | PASS |
| 浏览器宽 320 / 360 / 390 / 412 px，文档与 App 无横向溢出 | PASS；手机框实际内容宽 286 / 326 / 356 / 378 px，不等同 Android dp 验收 |

## 视觉复核

已查看 [总览](page-overview.png)、[深色设置](preview-dark.png)、[窄屏](preview-320.png)。普通任务保持列表，候选使用证据卡片；确认按钮位于固定底部操作区，列表可滚动，底栏未覆盖可滚动区域的末项。

补充截图：[桌面审核视图](preview-desktop.png)、[仅日期确认页](preview-confirm.png)。所有页面为合成数据。

主要文字 token 对比度计算：浅色主文本 / surface 16.75:1、次级文字 / 背景 5.26:1、主按钮 6.44:1、紫色标签 5.34:1、警示标签 5.44:1；深色对应主文本 13.63:1、次级文字 9.45:1、主按钮 9.73:1、警示标签 8.56:1。这只覆盖列出的配色，不替代实际组件、焦点、TalkBack 与动态字号检查。

## 范围与限制

- 本轮前后 58 个应用源码、测试、构建配置、Gradle 和根 README 文件 SHA-256 一致；没有修改应用源码。
- 未执行 Gradle、Android 安装或设备测试。既有 NI 验收记录保持原状态。
- 浏览器预览不模拟真实权限成功、通知采集、数据库迁移 / 竞争、进程恢复、远端 AI 或 BLE。
- 未完成的 Android 设计验收项见方案第 9 节；设计交付完成不代表相应产品能力完成。

`pwsh -NoProfile -File .\doc\harness\checks\check.ps1` 退出码 0：40 份文档、156 个本地链接、35 个 Kotlin 文件、1 份 Room schema；这些是现有检查器实际扫描范围，不代表预览经过 Android 行为验证。结果已在交接记录中登记，未放宽 harness 规则。
