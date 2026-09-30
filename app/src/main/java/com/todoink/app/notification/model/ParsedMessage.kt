package com.todoink.app.notification.model

/**
 * 从快照中分离出的消息。Phase 2 将落入 ObservedMessage 表；
 * 解析接口从一开始就允许返回 0 到多条消息（方案 2.2）。
 */
data class ParsedMessage(
    val senderName: String?,
    val text: String,
    val messageTimestamp: Long?,
)
