# MVP-PLAN：消息盒子到 TodoList 的产品 MVP 方案

- 日期：2026-09-22；状态：DONE（仅文档任务，产品实施未开始）。
- 用户目标：基于消息整合 → AI → TodoList → 后续墨水屏的核心思路，设计 MVP。
- 范围：可审核产品计划、任务映射与缺失的消息盒子任务；不实施 Android、AI 或硬件。
- 基线：无 Git；本轮重新计算 68 个应用 / 构建文件，与 [已有哈希清单](../evidence/REPLY-RESEARCH-baseline-2026-09-22.json) 一致，实施时仍需再核对。

## 交付

[产品 MVP](../MVP_PRODUCT_PLAN.md) 明确 P0、页面和默认值、五个里程碑、质量口径、验证证据与审核范围。复用原 NI / TD / AI；新增 [MB-001](MB-001-app-drawers.md) 和 [MB-002](MB-002-open-source.md)。同步入口、Phase 2、路线和交接，避免直接回复或旧通知结构被误当现行计划。

关键约束：两种 AI 模式均保留为最终 MVP；首版手机闭环，硬件后置；用户确认任务、手动完成；消息盒子只跳转原 App；无完整发出消息前置。本轮不调整现有 NI / UI 已验证结论，不修改原 PRD 快照。

## 验收与记录

| 项目 | 当前 |
| --- | --- |
| 范围、依赖、页面、验收可追溯 | PASS；MVP-M0–M4 对应 NI / TD / MB / AI，MVP-V01–V08 对应原验收；已区分中间单引擎与最终双模式 |
| Static | PASS；退出 0，49 份文档、234 个本地链接、45 个 Kotlin 文件、1 份 Room schema |
| 修改文档补充检查 | PASS；16 份文档、185 个本地链接均存在；严格 UTF-8 解码与替换字符检查无错误 |
| 应用文件前后基线 | PASS；开始 / 结束均重新核对 68 个文件，与已有清单一致，新增 / 删除 / 修改均为 0 |
| Android / 模型 / 真机 / 硬件 | 本轮不执行，不能作为产品已完成证据 |

执行记录：`pwsh -NoProfile -File .\doc\harness\checks\check.ps1`；补充 PowerShell 内联检查使用严格 `UTF8Encoding(false, true)` 解码与 `Test-Path` 核对相对链接，使用 `rg --files app/src app/schemas gradle` 连同项目构建文件清单重新枚举，并以 `Get-FileHash -Algorithm SHA256` 对比既有基线。补充两项退出码均为 0。没有运行 Verify / Gradle，检查器和规则未修改。

## 交接

方案已完成并供用户审核；产品计划和新增实施任务保持 DRAFT。待验证项包括来源覆盖、真实引擎兼容 / 质量 / 资源、跳转行为与用户试用；不能从本任务 DONE 推断产品完成。本轮不顺带启动 M0–M4；Phase 1 原有授权继续有效。
