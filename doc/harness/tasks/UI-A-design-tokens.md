# UI-A：设计 token 与现有三页风格统一

- 状态：VERIFYING（2026-09-20 实现完成，Verify 门禁通过；真机浅 / 深色走查待设备重新连接，见下）。
- 依据：[前端设计方案](../../design/TodoInk_前端设计方案.md) §3（现状衔接）、§5（视觉规范）、§7（落地约定）、§8（实施批次 UI-A）。
- 里程碑：前端批次；不改变 Phase 1 NI 任务状态与验收边界。

## 目标与范围

按设计稿 §5 的完整 token 表实现 `MaterialTheme.colorScheme` 浅 / 深色映射，扩充 warning / success 语义色；按 §5 建立文字层级（页面标题 28sp、任务标题 16sp、正文 14–16sp、来源时间 12–14sp）与形状 / 间距；提取并使用 `EmptyState`、`StatusBanner` 共享组件；将「通知 / 状态 / 设置」三页样式对齐设计稿（页面左右 20dp、卡片 16dp 圆角 / 16dp 内边距、来源时间次级色）。

## 范围外

- 不切换四入口导航（UI-C，需 Phase 2 获批）；保留“通知 / 状态 / 设置”。
- 不实现独立授权状态检测、通知详情页、保留期设置（NI-005 / NI-006 功能项）；状态页仅把当前实为监听连接的行改标“监听服务”，消除误导（G06 功能仍归 NI-005）。
- 来源列表仍只含可启动 App，按设计稿 P08 在页面上如实说明该限制。
- 不新增 FAB、完成率、插画、联网字体。

## 不变量

- C10 / C11：不改变任何数据读取与日志行为；仅视觉层。
- Compose 材料三现有约定：Route / Screen 职责不变，ViewModel 不改数据契约（NotificationsViewModel 直接用 Entity 的例外仍按 rules.json 登记，归 NI-006 清理）。
- 深色跟随系统；品牌强调色固定紫色，移除动态取色（与设计稿“品牌强调色保持紫色”冲突时以设计稿为准）。

## 验收

| 条件 | 实际命令 / 结果 | 状态 |
| --- | --- | --- |
| Static / Verify 门禁通过（含既有 11 个 JVM 用例不回归） | `check.ps1 -Mode Verify` 退出码 0；11/11 用例通过；lint / assembleDebug 通过 | PASS |
| 三页使用统一 token，无散落硬编码颜色 | 三页与导航仅引用 MaterialTheme.colorScheme / todoInkExtendedColors；token 全部定义于 theme 包 | PASS（代码核查） |
| 真机 / 模拟器安装后浅 / 深色走查 | 设备无线连接中断（adb connect 被拒绝，mDNS 无服务），待重新连接后安装走查 | NOT_RUN |

## 执行记录（2026-09-20）

- `theme/Color.kt`：按设计稿 §5 实现浅 / 深两套 token 与 `TodoInkExtendedColors`（warning / success 语义色）。
- `theme/Theme.kt`：固定品牌紫方案（移除动态取色），扩展色经 `LocalTodoInkExtendedColors` / `todoInkExtendedColors()` 提供。
- `theme/Type.kt`：文字层级（页面标题 28sp / 任务标题 16sp / 正文 14–16sp / 来源时间 12–14sp）与 16dp / 14dp 圆角形状。
- 新增 `ui/components/EmptyState.kt`（通知空态 / 设置加载态复用）与 `ui/components/StatusBanner.kt`（状态页未连接警示，warning / success / error 三色调）。
- 三页重构：通知页（标题、卡片 16dp 内边距、来源 / 元数据次级色、去 chip 化）、状态页（监听服务行更名消除误导 + 警示条 + 48dp 按钮）、设置页（标题 / 说明含可启动应用限制、Switch 开关、整行 56dp 可点击、行分隔线）。
- 导航：底部导航选中态使用 primaryContainer 指示器。

已验证：编译 / JVM 11 用例 / lint / Verify 门禁通过。待验证：真机与深色走查、200% 字号与 TalkBack（UI-E 范围）。
