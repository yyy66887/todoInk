# TodoInk 开发文档

AI 开发入口为根目录 [AGENTS.md](../AGENTS.md)，详细内容从 [开发 Harness](harness/README.md) 开始。它把 DBX 的任务契约、共享业务入口、自动检查、证据验收和交接方式适配到当前 Kotlin / Android 项目。

**当前审核入口：[产品 MVP v0.1](harness/MVP_PRODUCT_PLAN.md)**。首版交付消息盒子 → 本地 / 云端 AI → 候选确认 → 手机 TodoList，拆为五个里程碑；墨水屏后置。研究依据见 [核心闭环可行性](product/TodoInk_核心闭环可行性与验证路线.md)。直接回复已取消，完整发出消息采集不作为首版前置。

[Phase 1 通知采集方案](harness/MVP_PHASE1_PLAN.md) 已获批，当前实施与验收进度见 STATE。[Phase 2 AI 消息处理与候选待办方案](harness/MVP_PHASE2_PLAN.md) v0.3 衔接 9 个 TD、2 个 MB 和 5 个 AI 任务；新增实施范围待审核，保留 Phase 1 未完成前置条件，按产品 MVP 安排交付批次。

**[AI 模块](product/AI/README.md)** 是本地模型与自定义云端 API 的统一产品入口，配套 [页面设计](product/AI/PAGES.md) 和 [实施任务与验收](product/AI/IMPLEMENTATION.md)。两种方式共用“消息 → 提取与校验 → 待确认 → 正式待办”流程，目前是建设方案，尚未实现推理能力。

- [当前代码基线与缺口](harness/PROJECT.md)：哪些已经存在，哪些还没有证明。
- [架构与依赖边界](harness/ARCHITECTURE.md)、[业务契约](harness/CONTRACTS.md)：改动应落在哪里。
- [开发循环](harness/WORKFLOW.md)、[验证入口](harness/VERIFICATION.md)：如何执行与结束任务。
- [阶段计划](harness/ROADMAP.md)、[当前交接](harness/STATE.md)：从哪里继续。
- [参考资料说明](reference/README.md)：两份原始材料的本地快照及其使用边界。
- [App 前端设计方案](design/TodoInk_前端设计方案.md)：同类页面研究、信息架构、页面交互、视觉规范与实施批次；配套 [可点击预览](design/todoink-preview.html)，设计待审核。
- [AI 消息处理可行性评估](product/TodoInk_AI消息处理可行性评估.md)：模型 / 运行时的官方研究依据；集成范围和执行顺序以 AI 模块为准。
- [消息盒子与发出消息可见性](product/TodoInk_消息盒子与发出消息可见性.md)：当前方向为 App 抽屉收录通知、点击跳转原 App；说明为何通知权限不能可靠获取用户在原 App 的回复。
- [旧跨 App 回复研究](product/TodoInk_跨App回复可行性评估.md)：用户已否决直接回复模式，保留为技术参考，不作为实施入口。

详细规范、脚本与证据保存在 `doc`，根 AGENTS.md 负责接入；Android 功能修复按任务清单另行推进。
