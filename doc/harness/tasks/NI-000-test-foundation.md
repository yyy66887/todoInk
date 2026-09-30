# NI-000：建立可运行的验证底座

- 状态：DONE（2026-09-20 软件门禁与真机 4 个存储用例全部通过，见 [验证记录](../evidence/NI-000-verification.md)）。
- 里程碑：M0；优先级：P0；依赖：无前置功能任务。
- 基线：执行前核对 PROJECT、现有改动、JDK / SDK / Gradle；当前未见测试目录。

## 目标与范围

沿现有 app 构建配置补齐 JVM 测试、协程测试、Room / Android instrumentation 所需配置。让失败回归真正调用生产类，取得本次可复现的构建与测试报告。

涉及 `app/build.gradle.kts`、版本目录、拟新增 `app/src/test` / `app/src/androidTest` 和必要的小型依赖注入点。不进行整批依赖升级，不以复制业务实现的测试替身证明正确性。

## 执行步骤

1. 核对并记录当前工具链、现有编译结果和失败原因；记录已有改动，避免把历史构建产物当本次证据。
2. 建立可控时钟 / 调度与存储边界的测试方式，配置必要的 runner；fixture 使用合成通知。
3. 选取一个现有正常行为建立非空基线，例如 GenericParser 保留有效 MessagingStyle 消息；测试调用真实生产类。
4. 为 NI-001 选择可验证提取器调用次数和来源切换竞态的环境。缺陷回归在对应任务中补齐，不为了 M0 通过提前弱化断言。

## 执行记录（2026-09-20）

按执行步骤逐项核对：

1. 工具链核对：JDK Temurin 21.0.12.1+1（`D:\AndroidDev\tools\jdk-21`，不在 PATH，需设 `JAVA_HOME`）、Gradle 8.14.3 wrapper、AGP 8.13.2 / Kotlin 2.2.21、SDK `D:\AndroidDev\sdk`。基线 `assembleDebug` 通过但全部 UP-TO-DATE（历史产物，仅证明工具链可用）。测试设备信息用户尚未提供。
2. 测试底座：`app/build.gradle.kts` 增加 `testInstrumentationRunner` 与 JUnit 4 / coroutines-test / androidx.test（ext-junit、runner、core）依赖（新依赖原因与退出条件见验证记录）；新增 `app/src/test` 与 `app/src/androidTest` 源集。可控时钟以 `com.todoink.app.time.AppClock` 注入 `NotificationRepository.cleanupExpiredSnapshots`，DI 绑定在 AppModule，`rules.json` 已登记新包。
3. JVM 基线：`GenericParserTest` 6 用例直接调用生产 GenericParser（消息保留、不拼接、textLines FULL、text PARTIAL、脱敏 HIDDEN、空 EMPTY）。`testDebugUnitTest` 6/0/0 通过；受控破坏 `messages = emptyList()` 后恰好 2 个用例失败、恢复后全部通过，回归有效。
4. NI-001 环境：`coroutines-test` 已就绪，可驱动有界队列；Recorder 依赖的 SettingsRepository 尚无接口缝，来源切换竞态的 JVM 验证需要 NI-001 在该任务内建立（已登记验证记录）。

真实存储 instrumentation 用例（`NotificationRepositoryStorageTest`，4 用例：新键保存 v1、同内容累加 observeCount、内容变化 v2、可控时钟保留期清理边界）已编译进 androidTest APK（`assembleDebugAndroidTest` 通过）；`connectedDebugAndroidTest` 因无设备 NOT_RUN，设备到位后执行。

## 验收

| 条件 | 证据 / 当前状态 |
| --- | --- |
| Static、真实 JVM 用例、lint、assembleDebug 成功 | `check.ps1 -Mode Verify` 退出码 0（Static 29 docs / 118 links / 31 Kotlin / 1 schema，fresh JUnit 非空，lint + assembleDebug 通过） | PASS | 会话日志 |
| 首个生产行为测试能够在故意破坏对应行为时失败 | 受控验证 2 failed → 恢复 6 passed，PASS |
| instrumentation runner 与至少一个真实存储行为测试可运行 | 已编译可运行；设备报告 NOT_RUN（无设备） |
| JDK / SDK / 设备条件明确 | JDK / SDK 已记录；设备待补，BLOCKED（仅设备部分） |

没有测试或 NO-SOURCE 必须失败。设备未就绪时完成软件准备，但不把设备测试写为通过。任务结束后按验证模板记录，并将后续动作交给 NI-001。
