package com.todoink.app.notification.parser

import com.todoink.app.notification.model.NotificationSnapshotDraft

enum class ParseStatus { PARSED, NO_TEXT, UNSUPPORTED }

enum class ContentAvailability { FULL, PARTIAL, HIDDEN, EMPTY }

data class ParseResult(
    val parserVersion: Int,
    val status: ParseStatus,
    val availability: ContentAvailability,
    val messages: List<com.todoink.app.notification.model.ParsedMessage>,
)

/** 每个来源实现一个解析器；未匹配来源使用通用降级解析（方案 4.2）。 */
interface NotificationParser {
    /** 匹配的来源包名；通用解析器返回 null。 */
    val sourcePackage: String?

    val parserVersion: Int

    fun parse(draft: NotificationSnapshotDraft): ParseResult
}
