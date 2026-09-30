# NI-001：来源过滤在正文提取前生效

- 状态：VERIFYING（2026-09-20 软件侧完成，V01/V02 JVM 回归与门禁通过；真机来源开关 NOT_RUN，见 [验证记录](../evidence/NI-001-verification.md)）。
- 建立日期：2026-09-20
- 优先级：P0
- 里程碑：M1；依赖：[NI-000 测试底座](NI-000-test-foundation.md)。
- 基线：[扫描记录](../PROJECT.md)、[源码哈希](../evidence/baseline-2026-09-20.json)。

## 当前与目标

源码已观察：Service 的 enqueue 先调用 SnapshotExtractor.extract，再把 draft 传给 Recorder；Recorder 才调用 isWhitelisted。因此未选来源已经被复制正文。process 也不复核排队后变化的来源设置。

目标：用户未选择的来源不进入正文提取；实时回调和重连补采走同一个受控入口。关闭来源操作成功后，尚未开始持久化的排队通知不再保存。

## 范围

主要入口：[Service](../../../app/src/main/java/com/todoink/app/notification/TodoInkNotificationListenerService.kt)、[Recorder](../../../app/src/main/java/com/todoink/app/notification/NotificationRecorder.kt)、[Settings](../../../app/src/main/java/com/todoink/app/data/settings/SettingsRepository.kt) 及必要的注入 / 测试配置。可以引入小型接收门面以复用规则，不在两个回调复制条件。

本任务不修复去重、Parser、详情页、BLE 或 AI，也不删除既有历史通知。

## 必须保留

C01、C02、C03、C09、C11。未加载设置时默认拒绝；回调不能为等待 DataStore 阻塞；设置更新后的内存可见性必须与“关闭成功”一致。清楚界定已经开始提交的事务，不承诺追溯取消已经提交的数据。

设置初始化完成后，对仍活跃且已选择的通知进行受控补采，避免冷启动期间仅因设置尚未就绪而永久遗漏这些活跃项；不承诺恢复已消失的通知历史。补采仍走同一入口与去重规则。

## 验收

| 场景 | 判据 | 状态 |
| --- | --- | --- |
| V01 非选来源实时进入 | 提取器调用数 0，入队 / 保存 0 | PASS（JVM 生产入口回归） |
| V01 非选来源出现在 activeNotifications | 与实时入口一致 | PASS（同一入口 JVM 回归；Service 级待真机） |
| V01 设置尚未加载 / 来源已选 | 前者拒绝，后者可保存 | PASS（空白名单默认拒绝，选中后同入口采集） |
| V01 设置加载完成后仍活跃的已选来源 | 受控补采，不读取未选来源正文 | PASS（补采走同一入口） |
| V02 入队后关闭来源 | 关闭成功后放行消费者，不保存旧排队项 | PASS（可控调度器回归） |
| V02 关闭后新回调 / 再开启 | 关闭时拒绝，开启后正常恢复 | PASS（JVM 回归） |
| 真机来源开关 | 一款实际来源完成关闭 / 开启验证，记录权限与连接状态 | NOT_RUN（无设备） |

## 执行记录（2026-09-20）

实现要点：

1. 唯一入口 `NotificationIntake`：`onNotificationArrived(packageName, extract)`，未选来源不执行 extract（包名来自回调字段，不算正文读取）；Service 的实时回调与 `onListenerConnected` 补采都改走该入口。
2. 消费侧复核：`NotificationRecorder.process` 在解析 / 保存前重新检查白名单，排队期间关闭来源的项直接丢弃；已开始提交的保存不追溯取消（C02 边界）。
3. 可测试缝：`SourceSettings`（同步白名单判断 + 更新）、`StatusRecorder`、`RetentionSettings` 三个窄接口；`SettingsRepository` 白名单改为一次性加载 + 编辑成功后同步刷新缓存，替代原持续 collect（消除旧 emission 回退缓存的竞态，`hasLocalEdit` 防启动覆盖）。
4. 回归：`NotificationIntakeTest` 5 用例（StandardTestDispatcher 控制消费者放行时机），受控破坏“先提取后过滤”后恰好 2 用例失败；`check.ps1 -Mode Verify` 通过（11 JVM 用例、lint、assembleDebug、androidTest 编译）。

遗留登记：来源选择列表仅含可启动 App，`com.android.shell` 等 adb 注入来源无法 UI 勾选（见验证记录，待用户确认是否扩展）；队列溢出 / 取消语义属 NI-004。

先用生产入口建立失败回归：可以注入提取器 / 时钟 / 调度器等可控边界，但不能只用源码字符串检查 if 的位置。若需要 Android Notification 构造，选合适的 JVM Android 测试环境或 instrumentation，并说明环境依赖。

## 执行与结束要求

运行 Static、Test、Verify 和所需设备验证；缺设备时如实保持待验状态。按 [验证模板](../templates/verification.md) 留下本次证据，更新 STATE。当前无实现、测试或真机通过声明。
