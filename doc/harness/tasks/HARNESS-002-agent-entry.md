# HARNESS-002：接入根 AGENTS.md

- 状态：DONE
- 日期：2026-09-20
- 用户目标：补齐 AI 开发应有的根目录规则入口。
- 原因：首版把全部交付放入 doc，缺少助手可自动发现的项目指令文件。

## 改动

- 添加根 [AGENTS.md](../../../AGENTS.md)：读取顺序、关键工程约束、验证命令与交接要求；详细规范仍在 harness 维护。
- 同步入口说明、决策与工作流程；保留 HARNESS-001 作为历史交付记录。
- 静态检查要求 AGENTS.md 存在、非空并检查其本地链接；自测添加缺失、空文件和断链三个场景。

## 验证

| 验收 | 实际命令 / 方法 | 结果 |
| --- | --- | --- |
| 根入口完整且链接可达 | `pwsh -NoProfile -File .\doc\harness\checks\check.ps1` | PASS，退出码 0 |
| 新增三个错误场景可阻断，原有场景保留 | `pwsh -NoProfile -File .\doc\harness\checks\self-test.ps1` | PASS，18 个场景通过，退出码 0 |
| 应用源码与配置未变化 | 与 HARNESS-001 基线逐项比较 SHA-256 | PASS，49 个文件一致 |

遵循 [官方 AGENTS.md 发现约定](https://learn.chatgpt.com/docs/agent-configuration/agents-md)。静态检查不证明新会话已实际加载文件；本会话直接读取采用，新会话加载来源可另行核对。没有修改应用功能或运行 Android 构建。
