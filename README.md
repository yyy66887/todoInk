# TodoInk

墨水屏 TODO 伴侣 App 的 Android 端基础框架（Phase 1：可观测的通知采集闭环）。

技术选型依据 `TodoInk_方案审核与前后端选型.md`：Kotlin + Compose (Material 3) 单 Activity、
ViewModel + StateFlow 单向数据流、Room（导出 schema）+ Preferences DataStore、Hilt、
NotificationListenerService + 按来源解析器。Phase 1–2 无服务器、无网络依赖。

## 环境

- JDK 21（`D:\AndroidDev\tools\jdk-21`）
- Gradle 8.14.3（wrapper 同版本）
- AGP 8.13.2 / Kotlin 2.2.21 / compileSdk 36 / minSdk 29

命令行构建：

```bash
JAVA_HOME=/d/AndroidDev/tools/jdk-21 ./gradlew assembleDebug
```

或直接用 Android Studio 打开工程目录。

## 包结构（单 Module）

```
com.todoink.app
├── notification/     系统通知采集
│   ├── TodoInkNotificationListenerService   监听服务：连接/断连状态、活跃通知补采
│   ├── SnapshotExtractor                    回调线程上的轻量字段复制（不持有 Bundle）
│   ├── NotificationRecorder                 有界队列 → 解析 → 去重 → 落库
│   ├── model/                               NotificationSnapshotDraft / ParsedMessage
│   └── parser/                              NotificationParser 接口 + GenericParser + ParserRegistry
├── data/
│   ├── db/           Room：notification_snapshots（快照层，含 contentHash/contentVersion/observeCount）
│   ├── repository/   NotificationRepository：快照层去重、保留期清理
│   └── settings/     DataStore：来源白名单（内存缓存副本）、监听状态持久化
├── device/           DeviceTransport 接口占位（硬件阶段接 Nordic BLE Library）
├── ui/               通知列表 / 状态 / 设置 三页 + 主题
└── di/               Hilt 模块
```

## 已落实的方案要点

- **三层数据模型的第 1 层**：只落 `NotificationSnapshot`（自有 ID、通知 key、内容版本、
  受控字段集合）；`ObservedMessage` 表与任务表按方案在 Phase 2 引入，解析接口从现在起
  就返回 0..n 条消息。
- **快照层去重**：同一 notificationKey 内容哈希相同只累加观察次数，内容变化保留新版本；
  不用 postTime 参与唯一键。
- **权限与连接状态分离**：状态页显示监听是否连接、最近回调/保存时间、最近错误；
  `onListenerConnected` 只补采当前活跃通知。
- **隐私默认行为**：白名单外的来源在读取正文与入库前即排除；原始快照默认保留 3 天；
  数据库与 DataStore 已排除 Android Auto Backup（`data_extraction_rules` / `backup_rules`）。
- ** MessagingStyle 优先**：先取 `NotificationCompat.MessagingStyle` 消息列表，
  再降级 textLines / bigText / text，不做机械拼接；记录正文可用性（FULL/PARTIAL/HIDDEN/EMPTY）。

## 尚未包含（按开发顺序后置）

- 消息表（ObservedMessage）、本地规则与候选任务（Todo）——第 3 步「候选收件箱」
- BLE / 墨水屏传输协议——第 4 步硬件独立验证
- 云端 AI（FastAPI）与 Retrofit 网络——第 5 步
- 周期性清理迁移到 WorkManager、App 专属解析器（微信/企微/QQ）
