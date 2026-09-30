# UI-B：通知列表 / 详情、来源筛选与独立授权状态

- 状态：VERIFYING（2026-09-20 实现完成，Verify 门禁通过；三态授权展示与详情页待真机 / 模拟器走查）。
- 依据：[前端设计方案](../../design/TodoInk_前端设计方案.md) §4 P05 / P06 / P09、§6、§8（实施批次 UI-B）。
- 关联：落地 [NI-005](NI-005-listener-status.md) 第 1 步（独立授权读取与分离展示）与第 4 步（Repository 计数、移除 DAO 历史例外）；通知详情的"整理内容"受 NI-003 限制，消息列表完整展示待其完成。

## 目标与范围

1. 通知列表按设计稿 P05：来源筛选（全部 / 已选择的来源，横向可滚动）、行内来源 / 时间 / 摘要、"正文不完整 / 仅摘要 / 内容不可用"次级标记；点击进入详情。
2. 通知详情页按 P06：默认"整理内容"（标题、可用性、解析状态、各正文表示、消息条数、时间）；"原始字段"折叠区展示受控字段（key / hash / 版本等），满足 Phase 1 原始字段可核查验收。
3. 状态页按 P09：`通知访问：已授权 / 未授权 / 状态未知` 与 `监听服务：已连接 / 未连接` 分行展示；未授权与已授权未连接分别给行动入口；从系统设置返回（ON_RESUME）刷新授权状态。
4. 记录数改经 Repository 查询；`rules.json` 移除 StatusViewModel→DAO 例外（NI-005 第 4 步）。

## 范围外

- ListenerStatusStore 启动恢复竞态与节流持久化（NI-005 第 2 步）、来源开关提交状态机（第 3 步）不在本批次。
- 详情页不实现跳回原 App 聊天；不添加下拉刷新；不做消息列表完整展示（NI-003）与候选 / 任务关联（Phase 2）。

## 不变量

- C09：授权与连接分别表达；不把"已授权但未连接"显示为正常采集。
- C10 / C11：详情仅读取已保存字段；正文仍不进日志。
- 授权判定使用系统事实（`NotificationManagerCompat.getEnabledListenerPackages`），不用"监听已连接"代替。

## 验收

| 条件 | 实际命令 / 结果 | 状态 |
| --- | --- | --- |
| Static / Verify 门禁通过（11 个既有 JVM 用例不回归） | `check.ps1 -Mode Verify` 退出码 0；11/11 通过；lint / assembleDebug 通过；rules.json 例外更新后静态检查通过 | PASS |
| 授权关闭、已授权未连接、已连接三态可区分展示 | 代码已实现（getEnabledListenerPackages + ON_RESUME 刷新 + 分行 / 警示条） | 待真机走查 |
| 详情页整理内容与原始字段可查看 | 详情路由 + 整理内容卡片 + 原始字段折叠区已实现 | 待真机走查 |
| 来源筛选改变列表且不写库 | 筛选为展示层状态（VM combine），未触碰存储 | PASS（代码核查）/ 待走查 |

## 执行记录（2026-09-20）

- DAO / Repository：新增 `observeSnapshot(id)`、`observeSnapshotCount()`。
- `StatusViewModel`：注入 ApplicationContext，`refreshAccessGranted()` 用 `NotificationManagerCompat.getEnabledListenerPackages` 独立读取授权（`Boolean?` 含"状态未知"）；计数改走 Repository；`rules.json` 移除 StatusViewModel→DAO 例外。
- `StatusScreen`：通知访问 / 监听服务分行；未授权与已授权未连接两种警示条（带系统设置入口）；ON_RESUME 刷新；授权与连接语义说明文案。
- `NotificationsViewModel`：`selectedSource` 展示层筛选 + 已选来源列表；列表页筛选 chips（全部 / 各来源）、可用性中文标记、卡片点击进详情。
- 新增 `NotificationDetailScreen` + ViewModel：整理内容（标题、可用性、解析状态、消息条数、版本 / 观察次数、bigText / textLines / text / subText、时间）+ 原始字段折叠区（key / hash / 版本等受控字段）；Entity 引用登记例外，归 NI-006 迁移 UI 模型。
- `TodoInkApp`：新增 `notification/{snapshotId}` 路由（Long 参数），详情页隐藏底部导航，返回键出栈。

已知边界：详情"消息列表"完整展示依赖 NI-003（当前仅显示条数）；授权读取在 Android 13+ 的"已授权"包含各用户配置差异（如锁屏隐藏），其内容级差异仍由快照可用性标记表达。
