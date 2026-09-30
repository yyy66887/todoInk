package com.todoink.app.notification

import android.app.Notification
import android.os.Bundle
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat
import com.todoink.app.notification.model.NotificationSnapshotDraft
import com.todoink.app.notification.model.ParsedMessage
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 回调线程上的轻量字段复制：白名单判断之后立即调用，
 * 之后不再触碰 StatusBarNotification / Bundle（方案 2.4）。
 */
@Singleton
class SnapshotExtractor @Inject constructor() {

    fun extract(sbn: StatusBarNotification): NotificationSnapshotDraft {
        val notification = sbn.notification
        val extras = notification.extras
        val receivedAt = System.currentTimeMillis()
        return NotificationSnapshotDraft(
            notificationKey = sbn.key,
            notificationId = sbn.id,
            tag = sbn.tag,
            packageName = sbn.packageName,
            groupKey = sbn.groupKey,
            isGroupSummary = sbn.isGroup,
            category = notification.category,
            channelId = try {
                notification.channelId
            } catch (_: Exception) {
                null
            },
            title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString(),
            text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString(),
            bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString(),
            subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString(),
            textLines = extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)
                ?.map { it?.toString().orEmpty() }
                .orEmpty(),
            postTime = sbn.postTime,
            receivedAt = receivedAt,
            messagingStyleMessages = extractMessagingStyle(notification),
            isRedacted = detectRedacted(extras),
            // G04（NI-003）：isGroupSummary 表示通知属于某个分组；
            // isGroupSummaryMessage 只在系统标记 FLAG_GROUP_SUMMARY 时为真（真正的汇总通知）。
            isGroupSummaryMessage = (notification.flags and Notification.FLAG_GROUP_SUMMARY) != 0,
        )
    }

    private fun extractMessagingStyle(notification: Notification): List<ParsedMessage> {
        val style = NotificationCompat.MessagingStyle
            .extractMessagingStyleFromNotification(notification) ?: return emptyList()
        return style.messages.mapNotNull { msg ->
            val text = msg.text?.toString()?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            ParsedMessage(
                senderName = msg.person?.name?.toString(),
                text = text,
                messageTimestamp = if (msg.timestamp > 0) msg.timestamp else null,
            )
        }
    }

    private fun detectRedacted(extras: Bundle): Boolean =
        extras.containsKey(EXTRA_HIDDEN_CONVERSATION_TITLE)

    private companion object {
        const val EXTRA_HIDDEN_CONVERSATION_TITLE = "android.hiddenConversationTitle"
    }
}
