# TodoInk 开发 Harness

版本：0.2 · 建立日期：2026-09-20 · 当前产品阶段：Phase 1 通知采集。

这套 harness 为人和编码助手提供同一套工作支撑：**读取真实上下文 → 明确任务契约 → 在边界内修改 → 执行检查 → 核验行为证据 → 留下可恢复的交接**。

本版包含可运行的静态检查与检查器自测，以及待实现的 Android 行为验收方案。它没有实现自动编码执行器，也没有在 App 内引入模型、AgentLoop 或云端 AI。

已批准的实施范围是 [Phase 1 通知采集](MVP_PHASE1_PLAN.md)，软件与设备证据见 STATE。当前新增工作的待审核入口为 [产品 MVP v0.1](MVP_PRODUCT_PLAN.md)，串联 Phase 1 收口与 Phase 2；九个 TD、两个 MB 和五个 AI 任务为 DRAFT。已有授权不因此失效。

## 立即使用

1. 读 [STATE](STATE.md) 和 [PROJECT](PROJECT.md)，核对当前文件与历史基线。
2. MVP 工作先核对 STATE 对应阶段的审核范围，再从 [ROADMAP](ROADMAP.md) 选任务或复制 [任务模板](templates/task.md)。复用已有 NI-000 测试底座；Phase 2 的新增任务与 Phase 1 的收口任务分别记录。
3. 按 [WORKFLOW](WORKFLOW.md) 执行，涉及行为时查 [CONTRACTS](CONTRACTS.md) 和 [验证矩阵](VERIFICATION.md)。
4. 从项目根目录运行以下命令。检查失败必须说明原因，不能把未运行的检查填成通过。

```powershell
pwsh -NoProfile -File .\doc\harness\checks\check.ps1
pwsh -NoProfile -File .\doc\harness\checks\self-test.ps1
```

5. 用 [验证模板](templates/verification.md) 保存证据，更新任务与 [STATE](STATE.md)。

可直接给编码助手这样的任务入口：

```text
请先读 doc/harness/README.md、STATE.md 和 tasks/NI-001-source-filter.md，
核对现有代码后完成 NI-001。遵守已选任务的契约，运行对应检查，
记录实际结果、未验证项和交接；参考文档中的其他计划不自动成为本次任务。
```

AI 开发从根目录 [AGENTS.md](../../AGENTS.md) 接入：它规定读取顺序、关键约束、验证命令与交接要求，详细材料继续集中在 `doc`。无需每次手工粘贴整套 harness。

Codex 的默认发现文件名是 `AGENTS.md`（复数），会在启动时建立指令链；普通 Markdown 链接不代表被链接内容自动注入，所以根入口明确要求助手读取相关文件。当前没有 Git 根，使用本项目根目录作为工作目录。新会话可核对实际加载来源；其他助手是否自动读取须以其工具约定为准。见 [官方说明](https://learn.chatgpt.com/docs/agent-configuration/agents-md)。

## DBX 模式如何落地

| DBX 材料中的模式 | TodoInk 首版落点 | 状态 |
| --- | --- | --- |
| 项目规则与版本化上下文（§13.2–13.3） | 根 AGENTS.md、本入口、PROJECT、STATE、任务模板、源码哈希基线 | 已建立 |
| 单一业务入口（§4.2） | 实时通知与重连补采共用采集链，边界见 ARCHITECTURE | 现有代码有入口，行为待验证 |
| 自动维护模块边界（§4.1、§13.4） | Kotlin 引用规则、精确历史例外、未知源集失败 | 已建立轻量检查 |
| 显式契约与能力（§4.3–4.4） | 以 Kotlin 接口和 Room schema 为事实来源；专属解析器通过注册接入 | 契约文档已建立；不新增重复枚举 |
| 错误 / 结果 / 恢复分离（§4.5） | 保存、重复、丢弃、失败、取消分别验收 | 已定义目标，部分待实现 |
| 行为与竞态测试（§4.6–4.7、§13.5） | 生产入口测试矩阵、模拟外部边界、三款 App 真机证据 | 验收方案已建立，测试基础设施待补 |
| 统一命令与严格门禁（§4.8–4.9） | 静态检查脚本 + Gradle 命令；缺少证据不能完成任务 | 本地入口可用，CI 未接入 |
| 任务完成契约与终态（§14.8–14.9） | WORKFLOW 中的状态转换和完成条件 | 流程约束，无后台执行器 |
| 发布与交接（§4.11） | 验证模板、交接模板、STATE | 已建立 |

先保持单 `:app` 模块、全量检查。插件平台、跨进程协议、智能 CI 路由与独立框架库暂不引入。

## 导航

| 文档 | 负责的信息 |
| --- | --- |
| [产品 MVP](MVP_PRODUCT_PLAN.md) | 手机端闭环范围、消息盒子、五里程碑、双模式交付、验收和审核记录 |
| [PROJECT](PROJECT.md) | 实际代码、环境、差距与证据强度 |
| [MVP Phase 1 方案](MVP_PHASE1_PLAN.md) | 用户审核范围、默认策略、里程碑、八任务与批准记录 |
| [MVP Phase 2 方案](MVP_PHASE2_PLAN.md) | 消息盒子、AI 提取、候选确认、正式待办、TD / MB / AI 任务及前置条件 |
| [AI 模块](../product/AI/README.md) | 本地 / 云端双模式、独立页面、统一处理契约、实施任务与验收 |
| [App 前端设计](../design/TodoInk_前端设计方案.md) | 官方页面参考、页面结构、交互、视觉规范及可点击预览；待审核设计 |
| [ARCHITECTURE](ARCHITECTURE.md) | 当前调用链、允许依赖、例外退出条件 |
| [CONTRACTS](CONTRACTS.md) | 可引用的业务不变量编号 |
| [WORKFLOW](WORKFLOW.md) | 执行阶段、失败与恢复、审查、完成判据 |
| [VERIFICATION](VERIFICATION.md) | 统一命令、门禁范围、自动与真机验收 |
| [ROADMAP](ROADMAP.md) | 按阶段排序的真实任务 |
| [STATE](STATE.md) | 当前状态与下一步 |
| [决策 0001](decisions/0001-harness-scope.md) | 本版设计取舍 |
| [HARNESS-001](tasks/HARNESS-001.md) | 本次交付记录 |
| [本次验证证据](evidence/HARNESS-001-verification.md) | 静态检查、15 个检查器场景与未验证范围 |
| [HARNESS-002](tasks/HARNESS-002-agent-entry.md) | 根 AGENTS.md 接入与验证 |
