# UI-C1：四入口导航与预览稿页面形态

- 状态：VERIFYING（2026-09-21 实现完成，Verify 门禁通过；页面形态与交互待真机 / 模拟器走查）。
- 依据：[todoink-preview.html](../../design/todoink-preview.html)（页面结构 / 交互 / 视觉）、[前端设计方案](../../design/TodoInk_前端设计方案.md) §5 视觉规范。
- 关联：页面形态与导航按预览稿；候选 / 待办数据流仍属 Phase 2（TD-001 / TD-005 / TD-006），本批次不伪造数据。

## 目标与范围

1. 底部导航切换为四入口：待办 / 待确认 / 通知 / 设置（预览稿 nav：软紫胶囊选中态）；"状态"并入设置（采集状态行），通知来源、数据保留、关于为二级页。
2. 共享组件按预览稿：PageHeader（日期副标题 + 印章图标）、SubBar（返回栏）、EmptyPanel（图标圆 + 标题 + 文案 + 可选按钮）、DefinitionRow（标签 / 值行）、SettingCard / SettingRow（chevron / 开关 / 值）、EvidenceBlock（引用块）、FilterPills、SegmentControl、SourceBadge（来源首字圆标）。
3. 页面按预览稿：待办首页（segment 今天 / 全部 / 已完成 + 空态）、待确认（空态）、通知列表（来源筛选 pills + 来源行 + 今天 / 更早分组 + 通知详情证据块 + 原始字段折叠）、设置（分组卡片：采集 / 数据与隐私 / 关于）、通知来源（搜索 + 开关，真实白名单）、采集状态（statepanel + definition 行，真实授权 / 连接 / 计数）、数据保留（真实当前保留期值，只读）、关于。
4. 扩展 token：muted / quote 背景（浅 #efebe6 / #f0ece6，深 #302c32）。

## 范围外（如实记录）

- 候选卡片、确认表单、待办行 / 完成控件、review-nudge、"本地候选识别"开关：数据流不存在（Phase 2 TD 任务），本批次不实现也不伪造；待办 / 待确认显示预览稿定义的真实空态。
- 角标数量（候选数）：无数据来源，不显示假角标。
- 来源开关"保存成功才反馈"的提交状态机属 NI-005 第 3 步。

## 验收

| 条件 | 实际命令 / 结果 | 状态 |
| --- | --- | --- |
| Static / Verify 门禁通过（11 个 JVM 用例不回归） | `check.ps1 -Mode Verify` 退出码 0；11/11 用例通过 | PASS |
| 四入口导航与二级页路由可用 | routes：todo / candidates / notifications / settings + status / sources / retention / about / notification/{id}；编译通过 | PASS（代码）/ 待走查 |
| 来源开关、搜索、筛选操作真实生效 | 来源开关写真实白名单（SourceSettings），搜索过滤本机应用，通知筛选为展示层 | PASS（代码核查）/ 待走查 |
| 状态页展示真实授权 / 连接 / 计数 | statepanel + definition 行，数据来自系统授权查询 / ListenerStatusStore / Repository / 白名单 | PASS（代码核查）/ 待走查 |

## 执行记录（2026-09-21）

- 主题：扩展 token 增加 muted（#efebe6 / #302c32）与 quote（#f0ece6 / #302c32）背景。
- 共享组件 `ui/components/Paper.kt`：PageHeader、SubBar、EmptyPanel、DefinitionRow、SourceBadge、InfoChip、FilterPills、SegmentControl、SettingRow（chevron / 开关 / 值三形态）。
- 待办首页：segment 今天 / 全部 / 已完成 + 日期副标题 + 各 tab 空态（今天空态带"查看全部"）；无候选数据，review-nudge 不显示。
- 待确认页：标题 + 说明 + 空态；识别开关随 TD-005 提供。
- 通知列表：筛选 pills（全部 + 已选来源，显示应用名）、按今天 / 昨天 / 更早分组、来源行（首字圆标 + 时间）、两行摘要、非 FULL 可用性标记；点击进详情。
- 通知详情：SubBar 返回栏 + 整理内容卡片 + 原始字段折叠区（同 UI-B，容器样式对齐预览稿）。
- 设置页：采集（通知来源 N 个应用 / 采集状态值行）、数据与隐私（保留时间真实值 / 清理说明）、关于分组卡片；"候选待办"分组随 TD-005 提供，当前不显示。设置页 ON_RESUME 刷新授权。
- 通知来源页：搜索框（按名称 / 包名过滤）+ 真实白名单开关 + 可启动应用限制说明。
- 采集状态页：statepanel（就绪 / 未授权 / 已授权未连接三态）+ definition 行（通知访问 / 监听服务 / 启用来源 / 最近收到 / 最后保存 / 最近错误 / 快照数）。
- 数据保留页：显示 Repository 当前真实保留天数（只读，修改入口随 NI-006）；清理边界说明。
- 关于页：本地优先说明。
- 删除被替换的 SourceSettingsScreen / SourceSettingsViewModel（逻辑并入 SourcesViewModel）。

## 范围外（再次确认）

候选卡片、确认表单、待办行 / 完成控件、review-nudge、候选角标、识别开关、保留期修改控件均为 Phase 2 / NI-006 数据流，本批次未实现也未伪造。对应前端批次 UI-C 完整版（候选流）与 UI-D（待办生命周期）待 Phase 2 获批。
