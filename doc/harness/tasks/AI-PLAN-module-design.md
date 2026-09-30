# AI-PLAN：将双模式 AI 纳入 TodoInk 建设方案

- 状态：DONE（文档交付）；日期：2026-09-21；AI 功能实施任务仍为 DRAFT。
- 用户目标：用一个名为 AI 的独立模块说明并整合手机本地模型和可配置 API 两种方式。
- 本轮范围：产品 / 页面 / 技术方案、阶段和任务衔接；不修改 Android 源码或调用真实推理。
- 基线：无 Git；前后核对 68 个应用 / 构建相关文件的 SHA-256，均一致；完整 [基线与比较结果](../evidence/AI-PLAN-baseline-2026-09-21.json) 已留档。

## 交付与不变量

[AI 模块](../../product/AI/README.md)、[页面](../../product/AI/PAGES.md)、[实施任务](../../product/AI/IMPLEMENTATION.md)。同步 Phase 2、TD-003 / TD-005 / TD-008、架构与入口，取消旧规划中“Phase 2 排除 AI”的冲突。

独立业务模块保留单 :app；本地不自动转云端；云端配置与来源需显式启用；AI 只生成候选，用户确认后进入正式待办；所有未执行实现 / 性能验收保留 NOT_RUN。保留原 PRD 快照和 NI / UI 实施历史。

## 验收与执行记录

| 项目 | 当前结果 |
| --- | --- |
| 双模式、页面、接口与任务可追溯 | PASS；产品主文档、P10–P14、AI-001–AI-005 及 AI-V01–V10 已建立，Phase 2 / TD / 导航入口同步；均区分规划与已实现 |
| Static / 文档链接 | PASS；PowerShell 静态门禁退出 0；额外验证 product / design 的 5 份文档、29 个本地链接与严格 UTF-8 解码，无错误 |
| 应用文件基线比较 | PASS；68 → 68 文件，changed=[]、removed=[]；本轮未改应用 / 构建文件 |
| 模型 / API / Android 构建或真机测试 | 本轮文档范围不执行，不作为 AI 功能已验证 |

## 交接

产品方向已按用户要求纳入方案；新增实施任务保持 DRAFT。页面原型仍为四入口旧版，AI 页面是新增规格，尚未渲染到原型或 App。下一步按实施清单确定开工范围，优先输入 / 校验与配置；本地组合和真实服务验证分别积累证据。

实际验证命令为 `pwsh -NoProfile -File .\doc\harness\checks\check.ps1`，本轮执行通过；没有改检查器或规则，无需 self-test。补充链接检查用 PowerShell 的严格 UTF-8 解码和相对路径解析，覆盖静态门禁未递归扫描的 AI / 前端文件。源码基线使用 `rg --files app/src app/schemas gradle` 加根构建文件清单、`Get-FileHash -Algorithm SHA256` 前后比较。

范围审查时纠正了旧 Phase 2 排除 AI、旧设置页开关、Phase 3 首次接入 AI 和旧样本规模等冲突。没有重写原始 PRD、已批准 Phase 1 的范围或 NI / UI 历史验证结论。
