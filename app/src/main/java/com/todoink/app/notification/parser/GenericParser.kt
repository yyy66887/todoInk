package com.todoink.app.notification.parser

import com.todoink.app.notification.model.NotificationSnapshotDraft
import com.todoink.app.notification.model.ParsedMessage
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 通用降级解析器（NI-003 步骤 A）：优先 MessagingStyle 消息列表，
 * 再按 textLines → bigText → text 的顺序降级为消息，不拼接多种表示（C06）。
 * 缺发送人 / 时间保持未知，不猜测（C07）。
 */
@Singleton
class GenericParser @Inject constructor() : NotificationParser {

    override val sourcePackage: String? = null

    override val parserVersion: Int = 2

    override fun parse(draft: NotificationSnapshotDraft): ParseResult {
        val hasStyle = draft.messagingStyleMessages.isNotEmpty()
        val hasTextLines = draft.textLines.any { it.isNotBlank() }
        val hasBigText = !draft.bigText.isNullOrBlank()
        val hasText = !draft.text.isNullOrBlank()

        val availability = when {
            hasStyle || hasTextLines || hasBigText -> ContentAvailability.FULL
            hasText -> ContentAvailability.PARTIAL
            draft.isRedacted -> ContentAvailability.HIDDEN
            else -> ContentAvailability.EMPTY
        }

        return ParseResult(
            parserVersion = parserVersion,
            status = if (availability == ContentAvailability.EMPTY || availability == ContentAvailability.HIDDEN) {
                ParseStatus.NO_TEXT
            } else {
                ParseStatus.PARSED
            },
            availability = availability,
            messages = degradedMessages(draft),
        )
    }

    /** 按固定优先级产出 0..n 条消息：style → textLines（逐行）→ bigText → text。 */
    private fun degradedMessages(draft: NotificationSnapshotDraft): List<ParsedMessage> {
        if (draft.messagingStyleMessages.isNotEmpty()) return draft.messagingStyleMessages
        val lines = draft.textLines.filter { it.isNotBlank() }
        return when {
            lines.isNotEmpty() -> lines.map { ParsedMessage(senderName = null, text = it, messageTimestamp = null) }
            !draft.bigText.isNullOrBlank() -> listOf(
                ParsedMessage(senderName = null, text = draft.bigText!!, messageTimestamp = null),
            )
            !draft.text.isNullOrBlank() -> listOf(
                ParsedMessage(senderName = null, text = draft.text!!, messageTimestamp = null),
            )
            else -> emptyList()
        }
    }
}
