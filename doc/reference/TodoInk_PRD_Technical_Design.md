# TodoInk 产品需求与技术方案

> 版本：v0.1  
> 当前阶段：MVP Phase 1  
> 目标平台：Android + ESP32 + E-Paper  
> 当前优先级：Android 通知采集与验证

---

## 1. 项目概述

### 1.1 项目名称

**TodoInk**

TodoInk 是一个将手机通知中的碎片化信息自动整理为待办事项，并通过 ESP32 墨水屏进行低干扰展示的个人任务管理系统。

### 1.2 项目背景

日常工作和生活中，大量行动项散落在微信、企业微信、QQ 等即时通讯工具中，例如：

- “明天下午把报价单发我一下”
- “周五之前确认一下合同”
- “记得把新版方案同步给客户”
- “下午三点开项目会”

这类信息通常需要用户手动记录到 TODO 软件中，否则很容易遗漏。

TodoInk 希望通过 Android 的系统通知能力，在用户明确授权后获取指定 App 的通知内容，并逐步完成：

```text
通知采集
    ↓
通知标准化
    ↓
待办识别
    ↓
用户确认
    ↓
TODO List
    ↓
BLE 同步
    ↓
ESP32
    ↓
墨水屏展示
```

---

# 2. 产品目标

TodoInk 的核心目标是：

> 从用户授权的应用通知中发现潜在行动项，并将确认后的 TODO 通过实体墨水屏持续展示。

最终完整链路：

```text
微信 / 企业微信 / QQ / 其他 App
                │
                ▼
         Android 系统通知
                │
                ▼
        TodoInk Android App
                │
       通知采集 / 过滤 / 去重
                │
                ▼
          TODO 提取引擎
                │
                ▼
         Candidate TODO
                │
            用户确认
                │
                ▼
            TODO List
                │
                ▼
               BLE
                │
                ▼
             ESP32
                │
                ▼
             E-Paper
```

---

# 3. 产品设计原则

## 3.1 Android 负责业务逻辑

Android App 负责：

- 通知权限管理
- 通知采集
- App 来源过滤
- 通知标准化
- 通知去重
- 本地数据存储
- TODO 识别
- TODO 管理
- AI 分析
- 每日汇总
- 墨水屏页面渲染
- BLE 数据同步

---

## 3.2 ESP32 尽量简单

ESP32 主要负责：

- BLE 连接
- 接收手机端数据
- 保存最后一次显示内容
- 驱动墨水屏
- 刷新显示
- 可选物理按键交互
- 低功耗运行

ESP32 不负责：

- 通知解析
- AI 分析
- NLP
- TODO 判断
- 中文文本复杂排版
- 微信 / 企业微信协议处理

---

## 3.3 用户拥有最终控制权

系统自动识别出的任务默认进入：

```text
CANDIDATE
```

只有经过用户确认后才进入正式 TODO。

例如：

```text
张三：
明天下午三点之前把新版报价单发给我。
```

可以生成：

```text
Candidate TODO：
15:00 前发送新版报价单给张三
```

而：

```text
张三：
我明天下午把新版报价单发给你。
```

不应该成为用户自己的 TODO。

---

# 4. MVP 总体规划

TodoInk 采用分阶段开发。

## Phase 1：Android 通知采集

目标：

> 验证 Android 能否稳定获取微信、企业微信、QQ 等指定 App 的通知，以及实际能够获取哪些字段。

本阶段不开发 TODO、AI、BLE、ESP32。

---

## Phase 2：通知转 Candidate TODO

目标：

> 基于真实通知数据，通过规则判断通知是否包含待办事项。

主要内容：

- Todo 数据模型
- Candidate 状态
- Rule-Based Todo Extractor
- 时间解析
- Candidate Inbox
- 用户确认 / 忽略

---

## Phase 3：AI TODO 提取

目标：

> 对规则无法准确判断的通知使用 AI 进行任务、时间和行动项识别。

---

## Phase 4：Android BLE + ESP32

目标：

> 打通 Android App 到 ESP32 的 BLE 通信链路。

---

## Phase 5：墨水屏展示

目标：

> Android 渲染 TODO 页面，通过 BLE 发送给 ESP32，并在墨水屏显示。

---

## Phase 6：自动汇总与低功耗优化

目标：

- 每日自动汇总
- 自动同步
- 过期任务处理
- ESP32 低功耗
- 自动刷新策略

---

# 5. MVP Phase 1：Android 通知采集

## 5.1 阶段目标

MVP 第一阶段只做一件事情：

> 在 Android 手机上获得用户授权后，稳定监听并采集指定 App 的系统通知，将通知结构化保存到本地，并在 App 中展示。

完整流程：

```text
目标 App
   ↓
Android Notification
   ↓
NotificationListenerService
   ↓
通知解析
   ↓
通知过滤
   ↓
通知标准化
   ↓
通知去重
   ↓
Room Database
   ↓
Notification Inbox
```

---

## 5.2 第一阶段不包含的功能

Phase 1 暂时不实现：

- TODO 自动识别
- AI 分析
- 每日汇总
- BLE
- ESP32
- 墨水屏
- 云同步
- 自动回复消息
- 微信数据库读取
- Hook / 逆向微信

---

# 6. 第一阶段重点验证 App

第一阶段优先验证：

```text
微信
企业微信
QQ
```

后续可扩展：

```text
飞书
钉钉
短信
Gmail
Outlook
其他 App
```

---

# 7. 通知采集目标字段

需要尽可能采集以下原始信息：

```text
packageName
appName

notificationKey
notificationId

title
text
subText
bigText
textLines

category
postTime
receivedAt
```

在标准化之后，重点希望得到：

```text
App 来源
发送人
会话 / 群名称
消息正文
通知时间
```

Phase 1 的核心目的之一就是确认不同 App 实际能稳定提供哪些信息。

---

# 8. Android 通知监听

## 8.1 NotificationListenerService

使用 Android 原生：

```text
NotificationListenerService
```

监听系统通知。

基本结构：

```kotlin
class AppNotificationListener :
    NotificationListenerService() {

    override fun onNotificationPosted(
        sbn: StatusBarNotification
    ) {
        val notification = sbn.notification
        val extras = notification.extras

        val title =
            extras.getCharSequence(
                Notification.EXTRA_TITLE
            )?.toString()

        val text =
            extras.getCharSequence(
                Notification.EXTRA_TEXT
            )?.toString()

        val bigText =
            extras.getCharSequence(
                Notification.EXTRA_BIG_TEXT
            )?.toString()

        val textLines =
            extras.getCharSequenceArray(
                Notification.EXTRA_TEXT_LINES
            )?.map {
                it.toString()
            }

        // 交给 NotificationProcessor
    }
}
```

---

## 8.2 Service 职责边界

`NotificationListenerService` 只负责：

```text
接收系统通知
+
提取原始 Notification 信息
+
将数据交给 NotificationProcessor
```

不要在 Service 中直接处理：

- AI 请求
- TODO 判断
- BLE
- 页面逻辑
- 大量数据库业务
- 复杂文本解析

---

# 9. 通知权限

App 首次启动需要检测：

```text
Notification Listener Access
```

是否已经授权。

授权流程：

```text
启动 App
   ↓
检测通知访问权限
   ↓
未授权
   ↓
显示权限说明
   ↓
跳转系统通知使用权页面
   ↓
用户授权 TodoInk
   ↓
返回 App
   ↓
开始监听通知
```

权限说明建议：

> TodoInk 需要通知访问权限，用于读取您选择的应用产生的通知，并在后续将其中可能存在的行动项整理为待办事项。通知数据默认仅保存在本机。

---

# 10. App 白名单

用户可以选择允许 TodoInk 处理哪些 App。

示例：

```text
通知来源

[x] 微信
[x] 企业微信
[x] QQ

[ ] 飞书
[ ] 钉钉
[ ] Gmail
[ ] Outlook
```

NotificationListener 收到通知后首先判断：

```kotlin
if (!notificationFilter.isAllowed(
        sbn.packageName
    )
) {
    return
}
```

MVP 可以优先支持：

```text
微信
com.tencent.mm

企业微信
com.tencent.wework

QQ
com.tencent.mobileqq
```

内部架构不能写死为只支持这三个 App。

---

# 11. 原始通知模型

Phase 1 必须保存足够多的 Raw Notification 数据，以便后续分析不同 App 的通知结构。

建议数据结构：

```kotlin
data class RawNotification(
    val key: String,

    val packageName: String,

    val notificationId: Int,

    val title: String?,

    val text: String?,

    val bigText: String?,

    val subText: String?,

    val textLines: List<String>,

    val category: String?,

    val postTime: Long
)
```

---

# 12. NotificationNormalizer

业务层不应该直接依赖 Android 的 `Notification` 对象。

增加统一标准化层：

```text
Android Notification
        │
        ▼
RawNotification
        │
        ▼
NotificationNormalizer
        │
        ▼
NormalizedNotification
```

建议内部模型：

```kotlin
data class NormalizedNotification(
    val id: String,

    val packageName: String,

    val appName: String,

    val sender: String?,

    val conversation: String?,

    val title: String?,

    val content: String,

    val timestamp: Long
)
```

示例：

原始企业微信通知：

```text
title:
张三

text:
明天下午三点之前把报价发给我
```

标准化结果：

```json
{
  "id": "notification_xxx",
  "packageName": "com.tencent.wework",
  "appName": "企业微信",
  "sender": "张三",
  "conversation": null,
  "title": "张三",
  "content": "明天下午三点之前把报价发给我",
  "timestamp": 1789722000000
}
```

---

# 13. NotificationProcessor

增加统一入口：

```text
NotificationProcessor
```

负责串联：

```text
Raw Notification
       ↓
NotificationFilter
       ↓
NotificationNormalizer
       ↓
NotificationDeduplicator
       ↓
NotificationRepository
       ↓
Room
```

这样 `NotificationListenerService` 可以保持轻量。

---

# 14. 通知去重

聊天 App 可能不断更新同一个系统通知。

例如：

```text
14:00
张三：你好
```

一分钟后可能变为：

```text
14:01
张三：你好
张三：报价发你了
```

如果每次更新都当成独立通知处理，会导致后续数据重复。

因此 Phase 1 就应该实现：

```text
NotificationDeduplicator
```

可参考：

```text
Notification Key
Package Name
Title
Content
Post Time
Content Hash
```

判断：

- 是否为完全重复通知
- 是否为同一 Notification 的内容更新
- 是否为新的消息事件

---

# 15. 本地数据库

使用：

```text
Room
```

保存通知数据。

Phase 1 至少建立：

```text
notifications
```

表。

建议字段：

```text
id

notification_key

package_name
app_name

notification_id

title
text
big_text
sub_text
text_lines

sender
conversation
content

content_hash

category

post_time
received_at
```

Entity 示例：

```kotlin
@Entity(
    tableName = "notifications"
)
data class NotificationEntity(

    @PrimaryKey
    val id: String,

    val notificationKey: String,

    val notificationId: Int,

    val packageName: String,

    val appName: String,

    val title: String?,

    val text: String?,

    val bigText: String?,

    val subText: String?,

    val sender: String?,

    val conversation: String?,

    val content: String,

    val contentHash: String,

    val category: String?,

    val postTime: Long,

    val receivedAt: Long
)
```

---

# 16. Notification Inbox

Phase 1 不需要开发正式 TODO 首页。

首先开发：

```text
Notification Inbox
```

用于查看采集到的通知。

示例：

```text
┌──────────────────────────────────┐
│ Notification Inbox               │
├──────────────────────────────────┤
│ 企业微信                  14:32   │
│ 张三                             │
│ 明天下午三点之前把报价发给我       │
│                                  │
├──────────────────────────────────┤
│ 微信                      14:25   │
│ 项目讨论群                        │
│ 李四：新版设计已经上传了           │
│                                  │
├──────────────────────────────────┤
│ QQ                        14:11   │
│ 王五                             │
│ 晚上记得看一下文档                 │
└──────────────────────────────────┘
```

---

# 17. Notification Detail

点击通知后进入详情页。

需要显示：

```text
App
Package Name

Notification Key
Notification ID

Title
Text
Big Text
Sub Text
Text Lines

Category

Normalized Sender
Normalized Conversation
Normalized Content

Post Time
Received Time
```

---

# 18. Raw / Normalized 双视图

通知详情建议同时提供两个 Tab：

```text
Raw
```

和：

```text
Normalized
```

例如：

## Raw

```text
package:
com.tencent.wework

title:
张三

text:
明天下午三点之前把报价发给我

bigText:
null

textLines:
[]
```

## Normalized

```text
App:
企业微信

Sender:
张三

Conversation:
-

Content:
明天下午三点之前把报价发给我

Timestamp:
2026-09-18 14:32
```

该功能对于后续排查非常重要。

它可以帮助快速判断：

```text
Android 没有提供数据
```

还是：

```text
NotificationNormalizer 解析错误
```

---

# 19. Phase 1 页面结构

第一阶段只需要三个核心页面：

```text
TodoInk
│
├── 通知
│
├── 应用
│
└── 设置
```

## 通知

展示采集到的通知。

## 应用

设置允许监听哪些 App。

## 设置

包括：

```text
通知访问权限状态

通知保留时间

清空本地通知

调试模式

App 版本
```

---

# 20. Phase 1 技术选型

第一阶段采用纯原生 Android。

## 开发语言

```text
Kotlin
```

## UI

```text
Jetpack Compose
Material 3
```

## 架构

```text
MVVM
+
UDF
```

## 状态管理

```text
ViewModel
+
StateFlow
+
Flow
```

## 异步

```text
Kotlin Coroutines
```

## 数据库

```text
Room
```

## App 配置

```text
DataStore
```

## Dependency Injection

```text
Hilt
```

## 通知

```text
NotificationListenerService
```

Phase 1 暂时不引入：

```text
BLE Library
Retrofit
AI SDK
ESP32
WorkManager 复杂任务
```

保持第一阶段尽可能简单。

---

# 21. Phase 1 工程结构

MVP 初期采用单 Module。

```text
app
│
├── notification
│   │
│   ├── AppNotificationListener.kt
│   ├── NotificationProcessor.kt
│   ├── NotificationNormalizer.kt
│   ├── NotificationFilter.kt
│   └── NotificationDeduplicator.kt
│
├── database
│   │
│   ├── AppDatabase.kt
│   ├── NotificationDao.kt
│   └── NotificationEntity.kt
│
├── repository
│   │
│   └── NotificationRepository.kt
│
├── settings
│   │
│   └── NotificationSettingsRepository.kt
│
└── ui
    │
    ├── inbox
    │   ├── NotificationInboxScreen.kt
    │   └── NotificationInboxViewModel.kt
    │
    ├── detail
    │   ├── NotificationDetailScreen.kt
    │   └── NotificationDetailViewModel.kt
    │
    ├── apps
    │   └── AppFilterScreen.kt
    │
    └── settings
        └── SettingsScreen.kt
```

---

# 22. Phase 1 核心数据流

```text
                Android
┌──────────────────────────────────────┐
│                                      │
│ NotificationListenerService          │
│              │                       │
│              ▼                       │
│       RawNotification                │
│              │                       │
│              ▼                       │
│     NotificationProcessor            │
│              │                       │
│       ┌──────┴──────┐                │
│       ▼             ▼                │
│ NotificationFilter  Ignore           │
│       │                              │
│       ▼                              │
│ NotificationNormalizer              │
│       │                              │
│       ▼                              │
│ NotificationDeduplicator            │
│       │                              │
│       ▼                              │
│ NotificationRepository              │
│       │                              │
│       ▼                              │
│      Room                            │
│       │                              │
│       ▼                              │
│ Notification Inbox                  │
│                                      │
└──────────────────────────────────────┘
```

---

# 23. Phase 1 测试场景

## 23.1 微信私聊

测试：

```text
微信联系人：
明天下午把报价单发我一下
```

检查：

- 是否收到 Notification
- Title 是什么
- Text 是什么
- BigText 是否存在
- 是否能识别联系人名称
- 是否能读取完整消息

---

## 23.2 微信群聊

测试：

```text
项目群
张三：明天下午把报价单发我一下
```

重点确认：

- `title` 是群名还是发送人
- `text` 中是否包含发送人
- 能否区分群名和发送人

---

## 23.3 企业微信私聊

测试：

```text
张三：
明天下午三点之前把报价单发给我
```

验证：

- 发送人
- 正文
- 时间
- Notification 更新方式

---

## 23.4 企业微信群聊

验证：

- 群名称
- 发送人
- 消息正文
- 多条消息合并方式

---

## 23.5 QQ 私聊 / 群聊

执行同样测试。

---

## 23.6 连续消息

快速发送：

```text
消息 1
消息 2
消息 3
```

观察系统通知是否：

- 产生 3 个独立 Notification
- 更新同一个 Notification
- 产生 `textLines`
- 覆盖上一条内容

---

## 23.7 App 在前台

验证：

```text
目标 App 正在前台
```

时是否仍会产生系统通知。

因为某些 App 在用户正在查看会话时可能不产生通知。

---

## 23.8 通知摘要

测试同一个 App 多个会话同时收到消息时：

```text
张三
李四
项目群
```

观察是否存在：

```text
summary notification
```

避免把汇总通知误判为一条真实消息。

---

# 24. Phase 1 成功标准

第一阶段完成不以 TODO 是否生成作为标准。

必须满足以下条件。

## 24.1 权限

用户能够正常授予：

```text
Notification Listener Access
```

App 能够正确检测权限状态。

---

## 24.2 微信

收到微信通知后：

```text
TodoInk
```

能够捕获通知并保存。

---

## 24.3 企业微信

收到企业微信通知后能够捕获并保存。

---

## 24.4 QQ

收到 QQ 通知后能够捕获并保存。

---

## 24.5 字段解析

至少能够获取：

```text
App
标题 / 发送人
消息正文
通知时间
```

对于无法获取的字段，可以通过 Raw Notification 页面分析原因。

---

## 24.6 本地保存

通知正常写入 Room。

关闭 App 后重新打开：

```text
历史通知仍存在
```

---

## 24.7 Notification Inbox

App 可以按照时间倒序显示采集到的通知。

---

## 24.8 App Filter

关闭某个 App 的监听后：

```text
该 App 后续通知不再进入数据库
```

---

## 24.9 去重

同一 Notification 被系统更新时，不应无条件生成大量完全重复记录。

---

# 25. Phase 2：通知转 Candidate TODO

完成 Phase 1 后，开始实现：

```text
Notification
      ↓
Todo Extraction
      ↓
Candidate TODO
```

Phase 2 的核心问题：

> 这条通知是不是一个需要“我”执行的任务？

例如：

```text
张三：
明天下午三点之前把报价发给我
```

转换为：

```text
Candidate TODO

15:00 前发送报价给张三
```

---

# 26. TODO 数据状态

建议定义：

```text
CANDIDATE
CONFIRMED
COMPLETED
IGNORED
```

状态流：

```text
Notification
    │
    ▼
CANDIDATE
    │
    ├──────► IGNORED
    │
    ▼
CONFIRMED
    │
    ▼
COMPLETED
```

---

# 27. TODO 数据模型

建议：

```kotlin
data class TodoItem(
    val id: String,

    val title: String,

    val description: String?,

    val dueAt: Long?,

    val status: TodoStatus,

    val sourceApp: String?,

    val sourceSender: String?,

    val sourceNotificationId: String?,

    val confidence: Float?,

    val createdAt: Long,

    val updatedAt: Long
)
```

---

# 28. Rule-Based Todo Extractor

Phase 2 先不用 AI。

定义统一接口：

```kotlin
interface TodoExtractor {

    suspend fun extract(
        notification: NormalizedNotification
    ): TodoExtractionResult
}
```

实现：

```text
RuleBasedTodoExtractor
```

初始关键词可以包括：

```text
明天
后天
上午
下午
晚上
之前
记得
需要
请
帮我
别忘了
周一
周二
周三
周四
周五
周六
周日
```

---

# 29. Phase 3：AI TODO Extraction

当规则无法稳定判断时，再加入：

```text
AiTodoExtractor
```

最终可以形成：

```text
Notification
      ↓
Rule Engine
      │
      ├── 明确不是任务 → Ignore
      │
      ├── 明确是任务 → Candidate
      │
      └── 不确定
             ↓
             AI
             ↓
         Candidate
```

AI 负责：

- 判断是否为用户待办
- 提取行动项
- 提取截止时间
- 判断执行对象
- 规范化标题
- 辅助任务去重

---

# 30. AI 输出格式

AI 不直接返回自由文本。

要求结构化输出：

```json
{
  "isTodo": true,
  "title": "发送新版报价单给张三",
  "dueAt": "2026-09-19T15:00:00",
  "confidence": 0.94,
  "reason": "对方要求用户在指定时间前执行发送动作"
}
```

---

# 31. Phase 4：BLE + ESP32

Android：

```text
BLE Central
GATT Client
```

ESP32：

```text
BLE Peripheral
GATT Server
```

设备名称可以设计为：

```text
TodoInk-XXXX
```

例如：

```text
TodoInk-83A2
```

---

# 32. BLE GATT 设计

自定义：

```text
TodoInk Service
```

可以包含：

```text
TodoInk Service
│
├── DeviceInfo Characteristic
│
├── SyncControl Characteristic
│
├── TodoData Characteristic
│
├── DisplayData Characteristic
│
└── DeviceStatus Characteristic
```

---

# 33. BLE 同步流程

```text
Android
   │
   │ Connect
   ▼
ESP32
   │
   │ Connected
   ▼
Android
   │
   │ SYNC_BEGIN
   ▼
ESP32
   │
   │ READY
   ▼
Android
   │
   │ DATA
   ▼
ESP32
   │
   │ ACK
   ▼
Android
   │
   │ SYNC_END
   ▼
ESP32
   │
   │ CRC_OK
   ▼
Refresh E-Paper
```

---

# 34. 墨水屏显示策略

建议 Android 直接负责页面渲染。

```text
TODO
   │
   ▼
Android Renderer
   │
   ▼
Black / White Bitmap
   │
   ▼
BLE
   │
   ▼
ESP32
   │
   ▼
E-Paper
```

ESP32 不需要处理：

- 中文字体
- Unicode
- Emoji
- 自动换行
- 字号
- 粗体
- 页面复杂布局

ESP32 只负责将 framebuffer 写入墨水屏。

---

# 35. 墨水屏页面示例

```text
┌──────────────────────────────┐
│ 明日 TODO        09 / 19 周六 │
├──────────────────────────────┤
│                              │
│ □ 09:30                      │
│   项目周会                    │
│                              │
│ □ 15:00 前                    │
│   发送报价单给张三             │
│                              │
│ □ 回复新版方案                │
│                              │
├──────────────────────────────┤
│ 3 Tasks            23:42 ↻   │
└──────────────────────────────┘
```

---

# 36. ESP32 技术选型

建议：

```text
ESP32-S3
```

开发框架：

```text
ESP-IDF
```

BLE：

```text
NimBLE
```

墨水屏：

```text
SPI E-Paper
```

本地存储：

```text
NVS / Flash
```

---

# 37. ESP32 本地缓存

ESP32 保存最后一次成功显示的数据。

即使：

```text
手机离开
ESP32 重启
蓝牙断开
```

仍然可以恢复最后一次 TODO 页面。

---

# 38. ESP32 功耗策略

理想流程：

```text
BLE 连接 / 唤醒
      ↓
接收数据
      ↓
刷新墨水屏
      ↓
保存状态
      ↓
进入低功耗
```

后续可加入：

```text
Deep Sleep
```

---

# 39. 每日 TODO 汇总

后期增加每日汇总能力。

任务来源：

- 今天产生的新 Candidate
- 已确认但未完成的 TODO
- 明天到期的任务
- 用户手动创建的任务
- 逾期但仍未完成的任务

每日处理：

```text
去重
+
排序
+
时间归一化
+
过期任务处理
+
生成 Tomorrow TODO
```

---

# 40. 隐私原则

TodoInk 会处理通知内容，因此隐私是核心设计要求。

基本原则：

```text
用户明确授权
```

```text
只处理用户选择的 App
```

```text
默认数据仅保存在本地
```

如果未来启用云端 AI：

```text
只发送任务分析所需文本
```

并明确提示用户。

建议未来提供：

```text
仅本地模式
```

和：

```text
AI 模式
```

---

# 41. 数据生命周期

原始通知没有必要永久保存。

可以提供：

```text
通知保留时间

1 天
3 天
7 天
30 天
```

TODO 可以长期保存。

---

# 42. 明确不做的事情

TodoInk 不采用：

```text
读取微信数据库
```

```text
Hook 微信
```

```text
逆向微信协议
```

```text
模拟登录微信
```

```text
自动获取聊天历史
```

```text
自动回复微信
```

系统只处理：

> Android 系统已经展示给用户、并且用户明确授权 TodoInk 访问的通知。

---

# 43. 推荐开发顺序

## Stage 1

```text
NotificationListenerService
```

先打印所有通知原始字段。

---

## Stage 2

```text
Notification Inbox
```

让通知能够直接在 App 页面中看到。

---

## Stage 3

```text
Room
```

保存通知历史。

---

## Stage 4

```text
App Filter
```

只处理微信、企微、QQ。

---

## Stage 5

```text
NotificationNormalizer
```

分别适配：

```text
微信
企业微信
QQ
```

---

## Stage 6

```text
NotificationDeduplicator
```

解决重复和更新通知问题。

完成以上内容，即认为 **MVP Phase 1 完成**。

---

# 44. 当前第一优先级

现阶段不需要考虑：

```text
AI
BLE
ESP32
墨水屏
每日汇总
```

第一优先级只需要回答一个问题：

> Android 到底能够从微信、企业微信、QQ 的通知中稳定获得什么信息？

当前第一个完整闭环：

```text
收到微信 / 企微 / QQ 消息
          ↓
Android 产生 Notification
          ↓
TodoInk 捕获 Notification
          ↓
提取 Raw Notification
          ↓
Normalize
          ↓
保存到 Room
          ↓
Notification Inbox
          ↓
用户看到刚刚收到的通知
```

只要这个闭环能够稳定运行，MVP 第一阶段即完成。

---

# 45. 项目最终架构

```text
                         Android
┌────────────────────────────────────────────────────┐
│                                                    │
│ NotificationListenerService                        │
│              │                                     │
│              ▼                                     │
│        RawNotification                             │
│              │                                     │
│              ▼                                     │
│      NotificationProcessor                         │
│              │                                     │
│       ┌──────┴─────────┐                           │
│       ▼                ▼                           │
│ NotificationFilter   Ignore                        │
│       │                                            │
│       ▼                                            │
│ NotificationNormalizer                             │
│       │                                            │
│       ▼                                            │
│ NotificationDeduplicator                           │
│       │                                            │
│       ▼                                            │
│ NotificationRepository                             │
│       │                                            │
│      Room                                          │
│       │                                            │
│       ├──────────────► Notification Inbox          │
│       │                                            │
│       ▼                                            │
│     TodoEngine                                     │
│    ┌─────┴─────┐                                   │
│    ▼           ▼                                   │
│  Rules         AI                                  │
│    │           │                                   │
│    └─────┬─────┘                                   │
│          ▼                                         │
│     TodoRepository                                 │
│          │                                         │
│         Room                                       │
│      ┌───┴─────┐                                   │
│      ▼         ▼                                   │
│ Compose UI   Renderer                              │
│                │                                   │
│                ▼                                   │
│              Bitmap                                │
│                │                                   │
│                ▼                                   │
│           BLE Manager                              │
│                                                    │
└──────────────────────┬─────────────────────────────┘
                       │
                       │ BLE
                       ▼
                ┌──────────────┐
                │    ESP32     │
                │              │
                │ NimBLE       │
                │ Framebuffer  │
                │ Flash Cache  │
                └──────┬───────┘
                       │
                       │ SPI
                       ▼
                  ┌─────────┐
                  │ E-Paper │
                  └─────────┘
```

---

# 46. 产品定位

TodoInk 最终并不只是一个：

```text
墨水屏 TODO
```

而是：

> 一个把碎片化通知信息逐步转化为行动项，并通过低干扰实体终端持续呈现的个人任务管理系统。

核心价值链：

```text
Information
     ↓
Understanding
     ↓
Task
     ↓
Action
```

但当前阶段只解决第一步：

```text
Information
```

也就是：

> **稳定、准确、可调试地获取 Android 通知。**

只有真实通知数据采集稳定以后，再进入 TODO 提取、AI、BLE 和墨水屏阶段。
