# 验证与门禁

## 统一入口

在项目根目录使用 PowerShell 7（`pwsh`）。静态检查无外部依赖、不修改文件；Gradle 模式会写构建缓存和产物，首次运行可能需要联网下载依赖。

| 命令 | 实际范围 | 不能证明 |
| --- | --- | --- |
| `pwsh -NoProfile -File .\doc\harness\checks\check.ps1` | 根 AGENTS.md 存在且非空、文档完整性 / 本地链接、源集与包登记、依赖白名单、精确例外、Room JSON 可解析 | 助手会话是否实际加载、Kotlin 编译、功能正确、迁移兼容 |
| `pwsh -NoProfile -File .\doc\harness\checks\self-test.ps1` | 在临时工程中注入违规，确认检查器能拒绝 | Android 业务行为 |
| `pwsh -NoProfile -File .\doc\harness\checks\check.ps1 -Mode Test` | 静态检查 + `:app:testDebugUnitTest --rerun-tasks` + 本次非空 JUnit 结果 | 真机通知行为 |
| `pwsh -NoProfile -File .\doc\harness\checks\check.ps1 -Mode Build` | 静态检查 + `:app:assembleDebug` | 安装、启动、测试通过 |
| `pwsh -NoProfile -File .\doc\harness\checks\check.ps1 -Mode Verify` | 静态检查 + 单测结果 + `:app:lintDebug :app:assembleDebug` | 真机采集验收 |
| `pwsh -NoProfile -File .\doc\harness\checks\check.ps1 -Mode Device` | 静态检查 + `:app:connectedDebugAndroidTest --rerun-tasks` + 本次非空结果 | 三款真实来源 App 的行为覆盖 |

`Device` 会连接测试设备并执行 instrumentation，可能安装测试 APK；选用专用测试设备。脚本不会自动给通知访问权、发聊天消息、发布产物或调用云端 AI。

当前没有测试基础设施，`Test` / `Verify` / `Device` 应明确失败，不能把 NO-SOURCE 当测试通过。静态模式会给出缺少测试源的提示，但静态通过只用于文档 / 结构任务。

JDK 根据 [当前配置](PROJECT.md) 设置，例如先验证本机路径存在，再设置：

```powershell
$env:JAVA_HOME = 'D:\AndroidDev\tools\jdk-21'
& "$env:JAVA_HOME\bin\java.exe" -version
.\gradlew.bat --version
```

本次未升级依赖、未下载工具链，也未执行上述 Gradle 命令。Android SDK 路径以每台机器的 `local.properties` 为准，不提交本机路径或凭据。

## 选择必需检查

| 改动 | 必需证据 |
| --- | --- |
| 只改文档 | Static；修改检查器时还需 self-test |
| Parser / Repository / 队列 | Static + Test + lint / build；数据库语义需 Room 集成测试 |
| Listener / 权限 / Settings | 上述检查 + Device + 对应真实来源手工场景 |
| UI | 上述相关检查 + 设备可观察结果 / 截图 |
| Room schema / converter | 升级旧库、数据保留、往返与迁移用例；不能只比 JSON |
| 未知目录 / 新模块 / 构建配置 | 先更新边界和任务影响范围，然后全量 Verify 与相关 Device |

未来 CI 直接调用同一脚本；功能变更以 Verify 为最低软件门禁。检查器自测也应成为 CI 必需项，不能使用 continue-on-error。CI 尚未创建，分支保护尚未设置。若未来拆分任务，失败、取消、缺失、意外跳过均不能视为完成。

## Phase 1 行为矩阵

全部场景初始状态均为 **未执行**。表格是验收规格，不是现有测试用例清单。NI-000 建立框架后，测试直接调用生产类，仅替换时钟、系统通知、存储失败等外部边界。

| ID | 输入 / 操作 | 预期可观察结果 | 建议验证层 |
| --- | --- | --- | --- |
| V01 | 非白名单通知分别进入实时回调与重连补采；设置尚未加载 | 不访问正文提取器、不入队、不写库；选中来源正常采集 | Service 适配测试 + 真机 |
| V02 | 制造待消费项，关闭来源并等待设置操作成功，再放行消费；重新开启 | 关闭后未开始持久化的项被丢弃；新通知被拒绝；开启后恢复 | 可控调度器行为测试 |
| V03 | 同 key 同正文，仅 postTime / receivedAt 改变；并发提交重复项 | 只有一个当前内容版本，observeCount 正确；无并发重复版本 | Repository + 真实 Room |
| V04 | 同 key A→B→A；不同 key 同文本；只改 MessagingStyle 中正文 | 内容变化各有递增版本，不跨 key 合并，不遗漏消息变化 | Repository 行为测试 |
| V05 | MessagingStyle / textLines / bigText / text 分别单独存在；组合、空白与重复表示 | 选择优先级一致、输出 0..n、保留消息順序、不机械拼接 | Parser 单测 |
| V06 | 群组普通成员、summary、隐藏 / 无正文、未知发送人 | 不混淆汇总与成员；无凭据不猜发送人；可用性标记有依据 | Extractor + Parser + 真机 |
| V07 | 一个解析器抛异常；下一条正常 | 失败可诊断且不伪装解析成功；后续通知仍能保存 | 管道行为测试 |
| V08 | 已授权但未连接；撤销授权；断连重连；状态恢复迟到 | 权限与连接分别显示，恢复不覆盖新状态；不承诺历史补全 | ViewModel + 真机 |
| V09 | 冻结消费者后填满 128 项再入队；保存失败；处理中取消 | 明确计数被丢项；保存失败可见；取消不吞；无无界重试 | 管道 / 协程测试 |
| V10 | 保存只有 MessagingStyle 正文的通知，杀进程重开；查看详情 | 正文仍可查看；Raw / Normalized 字段可追溯，按 receivedAt 倒序 | Room + UI / 真机 |
| V11 | 时钟置于保留边界两侧；清理后重开 | 仅删除过期快照，不误删有效项；默认 3 天可解释 | 注入时钟 + Room |
| V12 | 读取日志与备份规则；测试设备备份 / 恢复 | 普通日志无正文；数据库与 DataStore 不被恢复；配置存在不是运行证明 | 配置审查 + 设备 |
| V13 | 旧版本数据库升级；列表包含换行、U+001F、空元素和中文 | 迁移保留数据；编码往返一致；schema 与版本同步 | Room 迁移 / converter |

## 微信 / 企微 / QQ 真机矩阵

三款 App 分别执行：私聊、群聊、连续消息、同通知更新、多会话汇总、目标 App 前台、锁屏隐藏预览。记录设备型号、Android / ROM 版本、来源 App 版本、设置、通知是否实际产生、字段可见性、期望与实得结果。

目标 App 没产生系统通知、或系统隐藏内容时，记录为来源限制 / 未观察到；不能补造正文，也不能把“没有通知”误报成采集成功。手工发消息由测试者在已授权的测试会话中完成，harness 不自动向联系人发消息。

使用合成文本作为默认 fixture；真实样本先脱敏，替换姓名 / 群名 / 消息 / key 等可识别内容，同时保留字段结构、空值与通知更新顺序。不要把数据库、完整 extras、设备标识或私人通知写入报告。

## 证据格式

每次验证记录：日期、任务 ID、代码基线 / 哈希、命令、退出码、本次测试数、结果报告路径、设备条件、验收编号与结果。状态只能是 PASS / FAIL / NOT_RUN / BLOCKED；不适用需说明理由。旧构建产物或旧测试报告不算新证据。

文档链接检查只验证文件目标，忽略标题锚点与参考材料中的外链；Room JSON 检查只验证解析与基本结构。业务完成声明仍需上述矩阵证据。
