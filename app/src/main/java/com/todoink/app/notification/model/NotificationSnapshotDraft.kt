package com.todoink.app.notification.model

/**
 * 在监听回调线程上完成的轻量字段复制结果。
 * 只保留受控、可序列化的字段集合，不持有 Bundle / Parcelable（方案 2.2）。
 */
data class NotificationSnapshotDraft(
    val notificationKey: String,
    val notificationId: Int,
    val tag: String?,
    val packageName: String,
    val groupKey: String?,
    val isGroupSummary: Boolean,
    val category: String?,
    val channelId: String?,
    val title: String?,
    val text: String?,
    val bigText: String?,
    val subText: String?,
    val textLines: List<String>,
    val postTime: Long,
    val receivedAt: Long,
    /** MessagingStyle 中分离出的消息列表（可空表示来源未提供该结构）。 */
    val messagingStyleMessages: List<ParsedMessage>,
    /** 系统检测到内容被隐藏/脱敏（如锁屏预览关闭、OTP 保护）。 */
    val isRedacted: Boolean,
    /** 摘要快照标记：可能代表多条消息，只作诊断，不作为独立消息入库。 */
    val isGroupSummaryMessage: Boolean,
)
