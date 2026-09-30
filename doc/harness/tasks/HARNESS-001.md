# HARNESS-001：建立 TodoInk 开发 Harness

- 状态：DONE
- 日期：2026-09-20
- 用户目标：借鉴 DBX 开发模式，在项目 doc 目录搭建 TodoInk harness。
- 基线：非 Git 工作目录；[源码与配置哈希](../evidence/baseline-2026-09-20.json)。

## 交付范围

项目事实、架构边界、业务契约、执行循环、验证矩阵、阶段队列、任务 / 验证 / 交接 / 决策模板、NI-001 已填写任务、参考资料副本、静态检查器与故障注入自测。全部新增文件位于 doc。

Android 产品功能、根目录配置、CI 接入与模型执行器不属于本次实现范围。

## 验收

| 条件 | 证据 | 状态 |
| --- | --- | --- |
| 从入口找到当前事实、开发规则与下一任务 | README、PROJECT、STATE、NI-001 | PASS |
| 文档链接与现有包级边界检查通过 | check.ps1，退出码 0 | PASS |
| 错误依赖、未知代码区域、失效例外可阻断 | self-test.ps1，15 个场景通过 | PASS |
| 没有测试不能被包装成测试通过 | 临时工程 Test 模式负向验证 | PASS |
| 应用源码与配置没有变更 | 与扫描哈希逐项比较，49 个文件一致 | PASS |

完整证据与验证限制见 [验证记录](../evidence/HARNESS-001-verification.md)，下一步见 [STATE](../STATE.md)。本次 DONE 仅指 Harness 交付，不代表产品 Phase 1 已完成。
