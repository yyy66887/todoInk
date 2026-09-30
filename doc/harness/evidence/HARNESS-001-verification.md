# HARNESS-001 验证记录

- 日期：2026-09-20
- 环境：Windows，PowerShell 7.6.5。
- 代码基线：非 Git 工作目录；[49 个源码与配置文件的 SHA-256](baseline-2026-09-20.json)。
- 范围：本次新增的开发 Harness，不是 Android 产品验收。

## 已运行

| 检查 | 实际命令 / 操作 | 结果 |
| --- | --- | --- |
| 静态门禁 | `pwsh -NoProfile -File .\doc\harness\checks\check.ps1` | PASS，退出码 0；文档 / 本地链接、30 个 Kotlin 文件、1 个 Room schema；使用 3 项明确登记的历史例外 |
| 检查器故障注入 | `pwsh -NoProfile -File .\doc\harness\checks\self-test.ps1` | PASS，退出码 0，15 个场景全部通过 |
| 应用未改动 | 读取 baseline JSON，逐文件运行 `Get-FileHash -Algorithm SHA256` 比较 | PASS，49 个文件哈希未变 |
| 参考副本完整 | 对桌面原始文件与 doc/reference 副本分别计算 SHA-256 | PASS，两份均一致 |

自测在独立临时工程中运行，结束后清理；没有向实际 App 源文件注入错误。

## 15 个检查器场景

1. 当前基线通过。
2. 底层反向引用 UI 被拒绝。
3. 全限定名绕开 import 仍被拒绝。
4. 通配符非法依赖被拒绝。
5. model 中 Android 类型被拒绝。
6. 未登记包被拒绝。
7. 未登记 Java 源码被拒绝。
8. 未登记生产源集被拒绝。
9. 新增未登记 Gradle 模块被拒绝。
10. 断开的文档链接被拒绝。
11. 非法 Room JSON 被拒绝。
12. 已不再需要的历史例外被拒绝。
13. 注释与字符串中的包名不误判为引用。
14. 无单测源时 Test 返回失败，不运行 Gradle、不接受 NO-SOURCE。
15. 恢复后的基线通过。

## 审查与限制

- 文档明确区分源码事实、设计目标、待验证项；引用实际文件；NI-001 可独立接手。
- 本地静态门禁和检查器自测已执行。Gradle 模式仅验证了“缺少测试提前拒绝”路径；真实 Gradle 执行与报告消费路径尚未在此项目验证。
- 未执行 Android 编译、lint、业务单测、Room 迁移、instrumentation、真机来源采集、备份恢复或硬件验证。
- 包引用扫描不等于编译器或完整依赖图；JSON 合法不等于 schema 兼容；本地门禁尚未接入 CI。
- 参考文档内的建议没有作为本次操作指令；没有访问 DBX 远程仓库、添加 AI SDK、调用云端模型或发送用户通知内容。

结论：满足本次“在 doc 建立开发 harness”的完成条件。产品 Phase 1 仍未验收，后续从 [NI-001](../tasks/NI-001-source-filter.md) 开始。
