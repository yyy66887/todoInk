# NI-000 验证记录

- 日期 / 执行者：2026-09-20 / Kimi Code（AI 编码助手）。
- 代码基线与本地改动：49 个既有源码 / 配置文件哈希基线之外，本次新增 / 修改：
  - 修改 `gradle/libs.versions.toml`（新增 junit 4.13.2、kotlinx-coroutines-test 1.10.2、androidx.test ext-junit 1.2.1 / runner 1.6.2 / core 1.6.1）。
  - 修改 `app/build.gradle.kts`（testInstrumentationRunner、testImplementation / androidTestImplementation 依赖）。
  - 新增 `app/src/main/java/com/todoink/app/time/AppClock.kt`（AppClock 接口 + SystemAppClock）。
  - 修改 `app/src/main/java/com/todoink/app/data/repository/NotificationRepository.kt`（cleanupExpiredSnapshots 改用注入的 AppClock）。
  - 修改 `app/src/main/java/com/todoink/app/di/AppModule.kt`（AppClock 绑定）。
  - 修改 `doc/harness/checks/rules.json`（登记 com.todoink.app.time 包；repository 允许引用 AppClock）。
  - 新增 `app/src/test/java/com/todoink/app/notification/parser/GenericParserTest.kt`（6 用例）。
  - 新增 `app/src/androidTest/java/com/todoink/app/data/repository/NotificationRepositoryStorageTest.kt`（4 用例，未上设备）。
- 环境：Windows；JDK Temurin 21.0.12.1+1（`D:\AndroidDev\tools\jdk-21`，不在 PATH，需设 JAVA_HOME）；Gradle 8.14.3 wrapper；AGP 8.13.2 / Kotlin 2.2.21；SDK `D:\AndroidDev\sdk`（min 29 / target 36）；无测试设备连接。

| 验收 ID | 实际命令 / 操作 | 退出码 / 用例数 | PASS / FAIL / NOT_RUN / BLOCKED | 证据路径 |
| --- | --- | --- | --- | --- |
| 工具链可构建（基线） | `./gradlew :app:assembleDebug`（JAVA_HOME=jdk-21） | 0；42 任务 UP-TO-DATE（历史产物，仅证明工具链可用） | PASS（有限证明） | 会话日志 |
| 检查器规则改动自检 | `pwsh -NoProfile -File .\doc\harness\checks\self-test.ps1` | 0；17 场景通过 | PASS | 会话日志 |
| Static + Verify 门禁 | `pwsh -NoProfile -File .\doc\harness\checks\check.ps1 -Mode Verify`（JAVA_HOME=jdk-21） | 0；Static 29 docs / 118 links / 31 Kotlin / 1 schema；fresh JUnit 非空；lint + assembleDebug 通过 | PASS | 会话日志（首次因 pwsh 内缺 JAVA_HOME 失败，设置后重跑通过） |
| 首个生产行为测试非空通过 | `./gradlew :app:testDebugUnitTest` | 0；6 tests / 0 skipped / 0 failures | PASS | `app/build/test-results/testDebugUnitTest/TEST-com.todoink.app.notification.parser.GenericParserTest.xml` |
| 受控破坏验证（测试能失败） | 临时将 GenericParser 返回 `messages = emptyList()` 后复跑 | 非 0；6 tests / 2 failed（恰好 2 个消息保留用例失败） | PASS | 会话日志 |
| 受控恢复后回归通过 | 恢复 GenericParser 后复跑 | 0；6 tests / 0 skipped / 0 failures | PASS | 同上 XML |
| instrumentation runner + 真实存储测试可编译 | `./gradlew :app:assembleDebugAndroidTest` | 0；55 任务 | PASS（编译） | 会话日志 |
| instrumentation 真实存储行为测试运行 | `connectedDebugAndroidTest`（ANDROID_SERIAL=192.168.1.6:35925，无线 adb） | 0；4 tests on PJZ110 - 16，Finished 4 tests | PASS | 会话日志；Gradle 任务 `:app:connectedDebugAndroidTest` |
| JDK / SDK / 设备条件明确 | 设备：OnePlus PJZ110，Android 16（API 36），ColorOS PJZ110_16.0.10.501(CN01)，无线 adb 192.168.1.6:35925 | — | PASS | 本记录 |

回归是否能暴露原问题：能。受控破坏 `messages = emptyList()` 后，`保留有效的 MessagingStyle 消息列表` 与 `有 MessagingStyle 消息时不同其他正文表示拼接` 两个用例失败，其余 4 个仍通过，说明失败与被破坏行为精确对应；恢复后全部通过。

未验证范围与原因：
- `connectedDebugAndroidTest` 未运行：没有可用测试设备 / 模拟器，用户尚未提供测试手机信息。androidTest 代码已编译为 APK（assembleDebugAndroidTest 通过），但存储 / 清理 / 去重行为未在设备上观察。
- lint 报告中存在两条与本次改动无关的既有警告（SourceSettingsViewModel 注解 target、hiltViewModel 迁移提示），未在本任务处理。

报告是否来自本次执行、是否对应当前源码、是否已脱敏：是。JUnit XML 生成于本次 `--rerun` 后的恢复运行，对应恢复后的源码；无通知正文进入日志，样本全部为合成文本。

审查结论与仍需处理的问题：
- 新依赖原因与退出条件：junit / coroutines-test 是 JVM 回归与后续队列竞态测试（NI-001 / NI-004）必需；androidx.test 三件套是 instrumentation runner 与上下文获取必需。若后续引入测试库整合方案（如 Kotest）可整体替换，属独立决策。
- AppClock 为最小注入点：仅 Repository.cleanupExpiredSnapshots 使用；NI-002（哈希不含 postTime 后的时间语义）与 NI-006（保留期清理触发）可直接复用，不需要扩大接口。
- 已知未处理：Repository 的 contentHash 仍包含 postTime（G02，NI-002 范围）；SettingsRepository 无接口缝，JVM 层无法测试 Recorder（NI-001 建立）。

任务是否满足完成契约（说明理由，不只填“通过”）：
- 软件侧验收（Static / 真实 JVM 用例 / lint / assembleDebug / 受控失败验证 / androidTest 编译 / 环境记录）满足后可记 DONE 软件部分。
- “instrumentation runner 与至少一个真实存储行为测试可运行”的设备报告仍 NOT_RUN，按方案第 5 节依赖规则，本任务在设备验收项保持 VERIFYING，不因软件门禁通过而整体 DONE；NI-001 在方案允许下可继续（其不依赖设备）。
