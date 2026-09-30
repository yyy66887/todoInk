# 项目事实与差距

扫描日期：2026-09-20。证据级别：**源码静态观察**；本次没有运行 Android 构建、设备交互或功能测试。

## 基线

- 工作目录为 `todoINK`，`git status` 返回“不是 Git 仓库”。没有可引用的分支或提交，不能生成虚构的 diff / PR 基线。
- [源码与配置哈希清单](evidence/baseline-2026-09-20.json) 是本次扫描快照，供恢复任务时比较，不要求以后修改代码仍保持相同哈希。
- 单 Gradle 模块 `:app`，包名 `com.todoink.app`，源码位于 [app/src/main/java](../../app/src/main/java)。
- 已见本地构建输出目录，但历史产物不能作为本次构建或功能通过的证据。
- 扫描时未见 `src/test`、`src/androidTest`、测试依赖或 instrumentation runner；NI-000（2026-09-20）已补齐测试源集与依赖，见 [任务记录](tasks/NI-000-test-foundation.md)。未见 CI 配置。

## 当前配置（取自文件，不表示已验证环境）

| 项 | 当前值 / 来源 |
| --- | --- |
| Gradle | 8.14.3，[wrapper 配置](../../gradle/wrapper/gradle-wrapper.properties) |
| AGP / Kotlin / KSP | 8.13.2 / 2.2.21 / 2.2.21-2.0.5，[版本目录](../../gradle/libs.versions.toml) |
| SDK | min 29 / compile 36 / target 36，[app 构建配置](../../app/build.gradle.kts) |
| 字节码目标 | Java / Kotlin 17 |
| README 声明的构建 JDK | JDK 21，`D:\AndroidDev\tools\jdk-21`，每台机器自行验证 |
| 组件 | Compose Material 3、Hilt、Room、Preferences DataStore、Coroutines |
| 当前数据库 | Room v1，`notification_snapshots`，已导出 [schema](../../app/schemas/com.todoink.app.data.db.TodoInkDatabase/1.json) |
| 网络 / 设备 | 主 Manifest 无 INTERNET；DeviceTransport 仅接口，无 BLE 实现 |

## 已有骨架

实时通知、重连时的当前活跃通知补采、字段复制、有界队列、通用解析接口、快照保存与版本计数、来源设置、通知列表、状态页均有代码。每一项都还需要行为或设备证据。

产品当前目标仍是：获得用户授权后，采集选定来源的通知，结构化保存在本地并在 Inbox 中查看。Phase 1 的验收不能用“自动生成 TODO”替代。

## 源码与文档的具体差距

以下是开发任务的输入，不是本次顺带实施的修复。

| ID | 静态观察与路径 | 与目标的差距 / 需要验证 | 后续任务 |
| --- | --- | --- | --- |
| ~~G01~~ | 已解决（2026-09-20，NI-001）：来源判断前移至 NotificationIntake（先判包名再 extract），Recorder 消费侧复核来源；真机来源开关验证待设备 | — | 真机项并入 NI-007 |
| G02 | [Repository](../../app/src/main/java/com/todoink/app/data/repository/NotificationRepository.kt) 的 contentHash 包含 postTime，未包含 MessagingStyle 消息 | 时间变化可能产生内容新版本；仅结构化消息变化可能遗漏；需要行为回归确认 | NI-002 |
| G03 | [GenericParser](../../app/src/main/java/com/todoink/app/notification/parser/GenericParser.kt) 只把 MessagingStyle 列表写入 messages | textLines / bigText / text 参与状态判断但未转为消息；空 textLines 元素也可能影响 FULL 判断 | NI-003 |
| G04 | [Extractor](../../app/src/main/java/com/todoink/app/notification/SnapshotExtractor.kt) 两个摘要字段均来自 `sbn.isGroup` | 分组与汇总通知的区分需要核对并分别测试，不能把普通群组成员都视为汇总 | NI-003 |
| G05 | Recorder 使用 DROP_OLDEST，但只在 trySend 失败时记录溢出；捕获通用 Exception | 丢弃旧项是否可见、取消是否被吞掉、保存失败后是否继续，需要确定性队列测试 | NI-004 |
| G06 | [StatusScreen](../../app/src/main/java/com/todoink/app/ui/status/StatusScreen.kt) “通知访问权限”行实际显示 listenerConnected | 尚无独立授权状态检测；权限与连接语义仍混合 | NI-005 |
| G07 | [ListenerStatusStore](../../app/src/main/java/com/todoink/app/data/settings/ListenerStatusStore.kt) 启动恢复会重新写整个状态；recordCallback 仅改内存 | 连接状态恢复竞态、最近回调跨进程保存需验证 | NI-005 |
| G08 | [Entity](../../app/src/main/java/com/todoink/app/data/db/NotificationSnapshotEntity.kt) 保存 parsedMessageCount，未保存解析消息列表；当前 UI 只有列表卡片 | 仅 MessagingStyle 正文的持久化 / 重开可见性不足；Raw / Normalized 详情未实现 | NI-003、NI-006 |
| G09 | Repository 查询版本与插入未组成数据库事务，Mutex 位于 Recorder | 新增其他写入口前需建立事务边界及并发用例，不能依赖调用者都持有锁 | NI-002 |
| G10 | [Converters](../../app/src/main/java/com/todoink/app/data/db/Converters.kt) 用 U+001F 拼接列表；已配置备份规则与启动清理 | 任意文本往返、迁移、保留期与实际备份行为尚无验证 | NI-006 |
| G11 | [StatusViewModel](../../app/src/main/java/com/todoink/app/ui/status/StatusViewModel.kt) 直接使用 DAO；两个通知 UI 文件直接用 Entity | 为当前依赖登记精确例外，后续迁移到 Repository / UI 模型 | NI-005、NI-006 |

根 README 还引用未提供的 `TodoInk_方案审核与前后端选型.md`。本次不推测其内容。阶段名称以已提供 PRD 为产品参考；若后续调整 AI 与硬件验证先后，在决策记录中说明。
