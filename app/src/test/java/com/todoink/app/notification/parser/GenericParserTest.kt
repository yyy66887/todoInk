package com.todoink.app.notification.parser

import com.todoink.app.notification.model.NotificationSnapshotDraft
import com.todoink.app.notification.model.ParsedMessage
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * NI-003 步骤 A 回归：直接调用生产 [GenericParser]，验证正文降级优先级
 * （MessagingStyle → textLines → bigText → text）、空白过滤与不拼接表示（C06）。
 */
class GenericParserTest {

    private val parser = GenericParser()

    private fun draft(
        text: String? = null,
        bigText: String? = null,
        textLines: List<String> = emptyList(),
        messages: List<ParsedMessage> = emptyList(),
        isRedacted: Boolean = false,
    ) = NotificationSnapshotDraft(
        notificationKey = "key|0|tag",
        notificationId = 1,
        tag = "tag",
        packageName = "com.example.app",
        groupKey = null,
        isGroupSummary = false,
        category = "msg",
        channelId = "chats",
        title = "标题",
        text = text,
        bigText = bigText,
        subText = null,
        textLines = textLines,
        postTime = 1_000L,
        receivedAt = 2_000L,
        messagingStyleMessages = messages,
        isRedacted = isRedacted,
        isGroupSummaryMessage = false,
    )

    @Test
    fun `保留有效的 MessagingStyle 消息列表`() {
        val messages = listOf(
            ParsedMessage(senderName = "张三", text = "晚上一起吃饭？", messageTimestamp = 1_234L),
            ParsedMessage(senderName = null, text = "好啊", messageTimestamp = null),
        )

        val result = parser.parse(draft(messages = messages))

        assertEquals(ParseStatus.PARSED, result.status)
        assertEquals(ContentAvailability.FULL, result.availability)
        assertEquals(messages, result.messages)
        assertEquals(2, result.parserVersion)
    }

    @Test
    fun `有 MessagingStyle 消息时优先使用且不拼接其他表示`() {
        val messages = listOf(ParsedMessage(senderName = "张三", text = "正文A", messageTimestamp = 1L))

        val result = parser.parse(
            draft(text = "文本正文", bigText = "大文本正文", textLines = listOf("第一行"), messages = messages),
        )

        assertEquals(ParseStatus.PARSED, result.status)
        assertEquals(ContentAvailability.FULL, result.availability)
        assertEquals(messages, result.messages)
    }

    @Test
    fun `无 MessagingStyle 时 textLines 逐行降级为消息`() {
        val result = parser.parse(draft(textLines = listOf("第一行", "第二行")))

        assertEquals(ParseStatus.PARSED, result.status)
        assertEquals(ContentAvailability.FULL, result.availability)
        assertEquals(
            listOf(
                ParsedMessage(senderName = null, text = "第一行", messageTimestamp = null),
                ParsedMessage(senderName = null, text = "第二行", messageTimestamp = null),
            ),
            result.messages,
        )
    }

    @Test
    fun `textLines 中空白元素被过滤`() {
        val result = parser.parse(draft(textLines = listOf("  ", "", "有效行", " ")))

        assertEquals(
            listOf(ParsedMessage(senderName = null, text = "有效行", messageTimestamp = null)),
            result.messages,
        )
        assertEquals(ParseStatus.PARSED, result.status)
    }

    @Test
    fun `无 textLines 时 bigText 降级为单条消息`() {
        val result = parser.parse(draft(bigText = "长文本正文"))

        assertEquals(ParseStatus.PARSED, result.status)
        assertEquals(ContentAvailability.FULL, result.availability)
        assertEquals(
            listOf(ParsedMessage(senderName = null, text = "长文本正文", messageTimestamp = null)),
            result.messages,
        )
    }

    @Test
    fun `bigText 与 text 同时存在时只取 bigText 不拼接`() {
        val result = parser.parse(draft(text = "普通文本", bigText = "长文本"))

        assertEquals(ContentAvailability.FULL, result.availability)
        assertEquals(
            listOf(ParsedMessage(senderName = null, text = "长文本", messageTimestamp = null)),
            result.messages,
        )
    }

    @Test
    fun `仅 text 时可用性降级为 PARTIAL`() {
        val result = parser.parse(draft(text = "只有普通文本"))

        assertEquals(ParseStatus.PARSED, result.status)
        assertEquals(ContentAvailability.PARTIAL, result.availability)
        assertEquals(
            listOf(ParsedMessage(senderName = null, text = "只有普通文本", messageTimestamp = null)),
            result.messages,
        )
    }

    @Test
    fun `无正文且标记脱敏时为 HIDDEN`() {
        val result = parser.parse(draft(isRedacted = true))

        assertEquals(ParseStatus.NO_TEXT, result.status)
        assertEquals(ContentAvailability.HIDDEN, result.availability)
        assertEquals(emptyList<ParsedMessage>(), result.messages)
    }

    @Test
    fun `完全无正文时为 EMPTY 且 NO_TEXT`() {
        val result = parser.parse(draft())

        assertEquals(ParseStatus.NO_TEXT, result.status)
        assertEquals(ContentAvailability.EMPTY, result.availability)
    }
}
