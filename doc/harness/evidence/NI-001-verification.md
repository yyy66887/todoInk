# NI-001 验证记录

- 日期 / 执行者：2026-09-20 / Kimi Code（AI 编码助手）。
- 代码基线与本地改动：
  - 新增 `app/src/main/java/com/todoink/app/data/settings/SourceSettings.kt`（来源白名单窄接口）。
  - 新增 `app/src/main/java/com/todoink/app/data/settings/StatusRecorder.kt`（状态记录窄接口）。
  - 新增 `app/src/main/java/com/todoink/app/data/settings/RetentionSettings.kt`（保留期窄接口）。
  - 修改 `SettingsRepository.kt`：实现 SourceSettings / RetentionSettings；白名单改为一次性加载 + 编辑成功后同步刷新缓存（替换原持续 collect，避免旧 emission 回退缓存）。
  - 修改 `ListenerStatusStore.kt`：实现 StatusRecorder。
  - 新增 `app/src/main/java/com/todoink/app/notification/NotificationIntake.kt`：实时与补采唯一入口，先判断来源再调用提取 lambda。
  - 重写 `NotificationRecorder.kt`：入队与来源判断分离（enqueueDraft）；process 在解析 / 保存前复核来源（C02）；依赖改走窄接口。
  - 修改 `TodoInkNotificationListenerService.kt`：实时回调与 onListenerConnected 补采均改走 NotificationIntake；包名取自回调字段，未选来源不执行 extract。
  - 修改 `NotificationRepository.kt`：SettingsRepository 依赖收窄为 RetentionSettings。
  - 修改 `di/AppModule.kt`：新增 SourceSettings / StatusRecorder / RetentionSettings 绑定。
  - 新增 `app/src/test/java/com/todoink/app/notification/NotificationIntakeTest.kt`（5 用例，含受控 Fake 设置 / 状态 / DAO 与 StandardTestDispatcher 调度控制）。
- 环境：与 NI-000 相同（JDK 21 / Gradle 8.14.3 / SDK 36；无真机）。

| 验收 ID | 实际命令 / 操作 | 退出码 / 用例数 | PASS / FAIL / NOT_RUN / BLOCKED | 证据路径 |
| --- | --- | --- | --- | --- |
| V01 非选来源实时进入（提取器调用 0 / 入队 0 / 保存 0） | `NotificationIntakeTest.未选来源不调用提取器不入队不写库` | tests=5 failures=0 | PASS（JVM，生产入口） | `app/build/test-results/testDebugUnitTest/TEST-com.todoink.app.notification.NotificationIntakeTest.xml` |
| V01 来源选中后正常采集 | `来源选中后同一入口正常采集` | 同上 | PASS | 同上 |
| V01 重连补采与实时一致（未选来源不读正文） | `重连补采与实时回调走同一入口` | 同上 | PASS（入口一致性为代码结构 + JVM 行为；Service 级适配未测） | 同上 |
| V02 入队后关闭来源，排队项丢弃 | `入队后关闭来源，未开始持久化的排队项不保存` | 同上 | PASS | 同上 |
| V02 关闭拒绝 / 再开启恢复 | `关闭后新回调被拒绝，重新开启后恢复` | 同上 | PASS | 同上 |
| 受控破坏验证 | 临时把 Intake 改为先 extract 后过滤 | 11 tests / 2 failed（恰好 2 个 V01 相关用例失败），恢复后 11/0/0 | PASS | 会话日志 |
| Static / Test / Verify 门禁 | `check.ps1 -Mode Verify`（JAVA_HOME 已设） | 0；11 JVM 用例 fresh 通过，lint / assembleDebug 通过 | PASS | 会话日志 |
| androidTest 回归编译 | `assembleDebugAndroidTest` | 0（Repository 构造兼容新参数类型） | PASS（编译） | 会话日志 |
| 真机来源开关 | 一款实际来源关闭 / 开启验证 | 未执行 | NOT_RUN（无设备） | — |

回归是否能暴露原问题：能。把 Intake 破坏为“先 extract 后过滤”后，`未选来源不调用提取器…` 与 `关闭后新回调被拒绝…` 两个用例立即失败（extractCalls=1 / accepted=true），其余 9 个仍通过；恢复后全部通过。

未验证范围与原因：
- Service 级（StatusBarNotification → Intake）未在 JVM 覆盖：Service 依赖 Android 运行时；入口签名已把 sbn 隔离在 lambda 之外，结构上无法绕过来源判断调用 extract。真机验证随 NI-007 / 真机来源开关执行。
- 真机来源关闭 / 开启：无设备，NOT_RUN。
- 队列溢出、取消传播、错误分类仍是 NI-004 范围；本任务未改 catch Exception 现状（G05 保留）。
- 来源选择列表仍只列有桌面入口的 App：`com.android.shell` 等 adb 注入通知的来源无法在 UI 勾选白名单（影响模拟器用 `cmd notification post` 测试正向采集，不影响真机路径）。此为产品行为问题，登记为待用户确认项，不在 NI-001 契约（V01/V02）内擅自扩列表。

报告是否来自本次执行、是否对应当前源码、是否已脱敏：是。JUnit XML 为恢复后本次运行的 fresh 结果；样本全为合成正文，无真实通知数据。

审查结论与仍需处理的问题：
- 新接口原因与退出条件：SourceSettings / StatusRecorder / RetentionSettings 是让 V01/V02 可在 JVM 以生产入口验证的最小缝；NI-002 / NI-004 / NI-006 直接复用。若后续引入更完整测试装配可整体替换。
- SettingsRepository 白名单缓存策略：一次性加载 + 编辑成功后同步刷新；`hasLocalEdit` 防止启动加载覆盖编辑。外部进程写同一 DataStore 的场景 Phase 1 不存在，如未来出现需重新评估。
- C02 边界已按契约实现：关闭成功返回时消费侧缓存已一致；已开始提交的保存不追溯取消。

任务是否满足完成契约（说明理由，不只填“通过”）：
- V01 / V02 的软件判据已由调用生产入口的 JVM 回归覆盖并通过，门禁全绿。
- “真机来源开关”必需设备证据缺失，按方案第 5 节保持 VERIFYING，不标 DONE；后续不依赖设备的工作（NI-005 / NI-003）可继续。
