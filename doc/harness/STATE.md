# 当前交接

更新时间：2026-09-22（产品 MVP 方案 v0.1；保留核心研判 / 消息盒子 / AI / NI / UI 记录）。

## 当前目标

用户最新要求基于核心思路设计 MVP，已形成 [产品 MVP v0.1](MVP_PRODUCT_PLAN.md)，状态 DRAFT，记录见 [MVP-PLAN](tasks/MVP-PLAN-product-mvp.md)。首版交付手机端“消息盒子 → AI 候选 → 用户确认 → TodoList / 完成”；本地与可配置云端两模式均为最终范围，墨水屏后置。五入口为待办 / 待确认 / 消息盒子 / AI / 设置。候选确认、只处理启用后新消息、三天原文保留、建议质量门槛和试用口径均已写明供审核。

产品交付按 MVP-M0–M4 组织，复用 NI / TD / AI，新增 [MB-001 抽屉](tasks/MB-001-app-drawers.md) 与 [MB-002 原 App 跳转](tasks/MB-002-open-source.md)。[Phase 2](MVP_PHASE2_PLAN.md) 同步为 v0.3，前端方案同步为 v0.3；实施任务仍为 DRAFT。单真实 AI 引擎可作为中间联调版本，不能算双模式最终验收通过。

本轮只写方案并同步开发入口，不修改 Android 源码、数据库、原型或模型。文档任务 MVP-PLAN 已 DONE：Static 退出 0（49 份文档、234 个本地链接、45 个 Kotlin 文件、1 份 schema）；补充检查本轮 16 份文档的 185 个本地链接与 UTF-8 通过；前后重新核对 68 个应用 / 构建文件，无变化。未运行 Android / 模型 / 真机 / 硬件验证；用户的设计请求不视为新增实施批准，Phase 1 既有授权继续有效。

以下为前序研究与实施记录。

用户进一步明确核心目标：整合分散 App 消息，结合 AI 做成 TodoList，先在 App 呈现，后续扩展墨水屏。已形成 [核心闭环可行性与验证路线](../product/TodoInk_核心闭环可行性与验证路线.md)：技术路径可行，优先验证真实通知覆盖、任务提取效果和用户整理负担；以消息盒子 + AI 候选 / 确认 + 清单为主线，墨水屏复用正式任务数据。完整双向聊天 / 用户发出消息采集不是首版成立条件。

用户最新明确否决 TodoInk 内直接回复，改为“消息盒子 → 每个获准 App 一个抽屉 → 点击跳转原 App 回复”，并询问能否收集用户在原 App 发出的消息。[当前方案](../product/TodoInk_消息盒子与发出消息可见性.md) 已记录：抽屉和跳转可行；仅通知权限不能可靠获取外部 App 的发送内容，通知中偶发自发消息回显不等于完整双向同步。查看 / 跳转 / 撤除通知不能推断已回复；AI 不能据此自动完成待办。

本轮仅修订文档，未修改 App、增加权限或读取聊天。前端方案新增当前方向的入口说明；既有原型尚未改版。旧直接回复研究标记为历史资料，相关 R-0–R-4 不再作为当前后续步骤；未开发的发送功能无需回滚代码。无障碍读取 / 平台接口未获选为实施路线。

消息盒子与核心闭环研究交付已完成（仅文档）：`pwsh -NoProfile -File .\doc\harness\checks\check.ps1` 退出 0（45 份文档、204 个本地链接、45 个 Kotlin 文件、1 份 schema）；补充检查 5 份 product / design 文档的 35 个本地链接与 UTF-8，无错误。开始 / 结束重新核对 68 个应用 / 构建文件，与既有哈希清单一致，无新增、删除或变化。未运行 Android / 模型 / 真实来源 / BLE / 墨水屏测试，产品和硬件效果仍待验证。

以下为前序研究记录：[跨 App 回复评估](../product/TodoInk_跨App回复可行性评估.md) 曾建议 RemoteInput 路线，现已被上述产品选择替代，三款 App 的当时兼容性也未实际验证。

本轮源码核对：Listener 未接移除失效处理，Extractor / Draft 未保留回复动作；summary 判断仍有原 NI-003 缺口。未发送任何消息、安装探测程序或读取私人通知；研究记录与验证结果见 [REPLY-RESEARCH](tasks/REPLY-RESEARCH-feasibility.md)。这份研究不扩展 Phase 2 或自动回复实施范围。

研究交付验证：静态门禁退出 0；新增产品文档 7 个本地链接和 UTF-8 检查通过；本轮前后 68 个应用 / 构建文件哈希一致。未运行 Android 构建、实际通知回复或真机兼容测试；R-0–R-4 均 NOT_RUN。

前序请求是把本地模型与可配置代理商 / SaaS 两条路线统一纳入名为 **AI** 的独立模块。已建立 [AI 模块](../product/AI/README.md)、[页面规格](../product/AI/PAGES.md)、[实施任务与验收](../product/AI/IMPLEMENTATION.md)，记录见 [AI-PLAN](tasks/AI-PLAN-module-design.md)。双模式是用户已选方向；具体模型组合、资源与效果门槛仍待实测。

Phase 2 升为 v0.2：消息 → 选定本地 / 云端引擎 → 确定性校验 → 候选 → 用户确认 → 待办。TD 任务继续承担消息与候选业务，新增 AI-001–AI-005 承担配置、两引擎、页面和联合验收；Phase 3 改为后续质量 / 效率优化。新增实施任务保持 DRAFT；默认只生成候选、不自动转云端、不回填历史、云端来源单独允许。独立 AI 是业务模块，保留单 :app。

本轮向用户询问希望仅完成方案还是同时实现 App；未收到补充答复时沿已声明的文档范围推进。新页面为五入口和 P10–P14 规格，现有 App / 可点击预览仍为四入口。未下载模型、发通知、调用付费推理或修改应用功能。静态门禁通过，额外核对 5 份 product / design 文档的 29 个本地链接和 UTF-8；前后 68 个应用 / 构建文件哈希一致，完整记录见 AI-PLAN。未运行 Android 构建或模型 / 真机测试，不能据此宣称 AI 已可用。

前序已完成 [AI 可行性评估](../product/TodoInk_AI消息处理可行性评估.md) v0.1，保留官方模型 / 运行时来源；其中旧页面安排与研究批次由 AI 模块规格替代。

前序研判询问了自动候选 / 直接正式入库等级和手机内存档位，未收到具体选择，当前沿用候选确认流。研判轮 `adb devices -l` 仅有模拟器；真机历史通过记录继续有效，不能据此给推理性能结论。研判轮前后 68 个应用 / 构建相关文件 SHA-256 一致。

2026-09-21 AI 研判验证：`pwsh -NoProfile -File .\doc\harness\checks\check.ps1` 退出码 0（43 份文档、168 个本地链接、45 个 Kotlin 文件、1 份 Room schema）；另核对新增 product 文档的 5 个本地链接均存在、UTF-8 无替换字符。没有运行模型、Gradle 或真机测试。

前序页面设计已完成 [前端设计方案](../design/TodoInk_前端设计方案.md) v0.1、[可点击页面预览](../design/todoink-preview.html) 和 [验证记录](../design/DESIGN_VERIFICATION.md)。设计建议纸白 / 墨黑 / 紫色、Phase 2 四入口“待办 / 待确认 / 通知 / 设置”；后续实际实施范围见以下 UI 记录。预览使用合成数据。

2026-09-20 用户指示按设计稿继续开发；已实施 UI-A 批次（[任务记录](tasks/UI-A-design-tokens.md)，VERIFYING）：完整 §5 token（浅 / 深色 + warning / success 语义色）、文字层级与圆角规范、`EmptyState` / `StatusBanner` 共享组件，三页（通知 / 状态 / 设置）样式对齐设计稿并保留现有导航。`check.ps1 -Mode Verify` 通过，11/11 JVM 用例通过；真机 / 深色走查待设备重新连接。UI-B 及以后批次仍受 NI-003 / NI-005 / NI-006 与 Phase 2 审核约束。

2026-09-20 继续实施 [UI-B 批次](tasks/UI-B-notification-detail.md)（VERIFYING）：通知详情页（整理内容 + 原始字段折叠区）、列表来源筛选、独立授权状态与监听连接分离展示（P05 / P06 / P09）；同步落地 NI-005 第 1 / 4 步（授权独立读取、Repository 计数、移除 DAO 例外）。

2026-09-21 用户明确指示按 todoink-preview.html 设计稿开发；已实施 [UI-C1 批次](tasks/UI-C1-navigation-preview.md)（VERIFYING）：四入口导航（待办 / 待确认 / 通知 / 设置，状态并入设置）、预览稿全部页面形态（PageHeader / SubBar / EmptyPanel / DefinitionRow / SegmentControl / FilterPills / SettingCard 等组件）、通知来源搜索页、真实保留期展示页。待办与待确认显示真实空态（候选 / 待办数据流属 Phase 2 TD 任务，未伪造）。Verify 门禁通过；页面走查待设备。UI-C 完整版（候选确认流）与 UI-D（待办生命周期）待 Phase 2 获批。

2026-09-20 设计轮验证：浏览器 14 组交互 / 布局检查通过，已复核页面总览、深色与窄屏截图；当轮 `check.ps1` 静态门禁通过（40 份文档、156 个本地链接、35 个 Kotlin 文件、1 份 Room schema）。当轮前后核对的 58 个应用 / 构建相关文件哈希一致。该设计轮没有执行 Gradle 或设备验证，后续 NI / UI 执行记录另列。

Phase 1 v0.1 已于 2026-09-20 获用户批准（“同意 Phase 1 v0.1”，无附加修改），批准记录见 [MVP_PHASE1_PLAN](MVP_PHASE1_PLAN.md) 第 9 节。用户指示“先按照这个做，后面我接真机”；2026-09-20 真机已通过无线 adb 接入。

[NI-000](tasks/NI-000-test-foundation.md) 已 DONE：真机（OnePlus PJZ110，Android 16 / API 36，ColorOS PJZ110_16.0.10.501(CN01)，无线 adb 192.168.1.6:35925）上 4 个存储用例全部通过（`connectedDebugAndroidTest`，4 tests on PJZ110 - 16）。[NI-001](tasks/NI-001-source-filter.md) 软件侧完成，VERIFYING（仅剩真机来源开关验证）；NI-002–NI-007 为 READY。下一阶段 [Phase 2 方案](MVP_PHASE2_PLAN.md) 当前为 v0.3（TD-000–TD-008、MB-001 / MB-002、AI-001–AI-005），实施任务仍为 DRAFT。

## NI-001 已完成（软件侧）

- 唯一入口 `NotificationIntake`：实时回调与重连补采均先判断来源再调用提取；未选来源不产生正文复制（C01）。
- 消费侧复核：`NotificationRecorder.process` 在解析 / 保存前复核来源；排队期间关闭来源的项直接丢弃，已提交数据不追溯取消（C02 边界）。
- 可测试缝：`SourceSettings` / `StatusRecorder` / `RetentionSettings` 窄接口；`SettingsRepository` 白名单一次性加载 + 编辑成功后同步刷新缓存（替换持续 collect，消除旧 emission 回退竞态）。
- 回归：`NotificationIntakeTest` 5 用例（V01/V02，StandardTestDispatcher 控制消费时机）；受控破坏“先提取后过滤”后恰好 2 用例失败；恢复后 11/11 JVM 用例通过。
- `check.ps1 -Mode Verify` 通过；`assembleDebugAndroidTest` 通过。PROJECT G01 已标记解决。
- 详细证据：[NI-001 验证记录](evidence/NI-001-verification.md)。

## 未完成 / 阻塞

- NI-001 真机来源开关验证 NOT_RUN：在手机上实际关闭 / 开启一款来源并观察采集行为（App 已装在手机上）。
- 遗留登记：来源选择列表仅含可启动 App，adb 注入通知的 `com.android.shell` 无法 UI 勾选白名单（影响模拟器正向采集测试，不影响真机路径）；是否扩展列表待用户确认。
- 队列溢出 / 取消传播 / 错误分类仍为 NI-004 范围（G05 未动）。

## 已建立

DBX → TodoInk 映射、项目源码基线、架构 / 契约、工作循环、验证矩阵、阶段任务、模板、参考资料副本及首个功能任务。源码观察发现的差距统一登记在 [PROJECT](PROJECT.md)，没有把建议当成已实现能力。

## 产品尚未验证

没有三款来源真机采集。instrumentation 存储用例已于 2026-09-20 在真机（PJZ110）通过；来源真机采集场景仍属 NI-007。当前不是 Git 仓库，无法引用分支与提交；恢复时先核对文件。

## 下一步

先审核 [产品 MVP v0.1](MVP_PRODUCT_PLAN.md) 的范围、默认值与验收建议，再按用户选定的实施批次执行。新增产品链路尚未开工；实施前重新核对源码 / 设备，并冻结样本、模型组合与资源门槛。App 和预览仍是原有四入口，不能用文档状态推断已接入 AI 或消息盒子。

产品依赖顺序：核对真实输入并补齐 NI → TD 消息层 / MB 抽屉与跳转 → 共用 AI 与首个真实引擎 → 候选与正式任务 → 双模式联合验收及试用。发出消息回显只作增强，不等待其完整可用；不继续内联发送或增加无障碍权限。手机闭环通过实际使用验证后再进入硬件阶段。以下 Phase 1 事项仍保留原授权和状态。

1. 按批准顺序继续 NI-005（授权 / 连接分离与状态恢复）或 NI-003（正文结构与解析），均不依赖设备；G06/G07/G03/G04/G08 为输入。
2. 真机（PJZ110）已接入：NI-000 的 4 个存储用例已通过；剩余 NI-001 真机来源开关验证（在手机上关闭 / 开启一款来源并观察采集）。
3. Phase 2 方案仍待审核；不因 Phase 1 推进而自动开工。
