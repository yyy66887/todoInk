package com.todoink.app.data.repository

import com.todoink.app.data.db.NotificationSnapshotDao
import com.todoink.app.data.db.NotificationSnapshotEntity
import com.todoink.app.data.settings.RetentionSettings
import com.todoink.app.notification.parser.ParseResult
import com.todoink.app.time.AppClock
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

/**
 * 快照持久化入口。
 *
 * 去重分层（方案 2.3）当前实现快照层：同一 notificationKey 内
 * 内容哈希相同只累加 observeCount，内容变化则保留新版本。
 * 消息层与任务层去重在对应数据层引入后补充。
 */
@Singleton
class NotificationRepository @Inject constructor(
    private val snapshotDao: NotificationSnapshotDao,
    private val settings: RetentionSettings,
    private val clock: AppClock,
) {

    suspend fun saveSnapshot(
        draft: com.todoink.app.notification.model.NotificationSnapshotDraft,
        parseResult: ParseResult,
    ): Outcome {
        val contentHash = draft.contentHash()
        val latest = snapshotDao.findLatestByKey(draft.notificationKey)
        if (latest != null && latest.contentHash == contentHash) {
            snapshotDao.incrementObserveCount(latest.snapshotId)
            return Outcome.DUPLICATE_CONTENT
        }
        snapshotDao.insert(
            NotificationSnapshotEntity(
                notificationKey = draft.notificationKey,
                notificationId = draft.notificationId,
                tag = draft.tag,
                packageName = draft.packageName,
                groupKey = draft.groupKey,
                isGroupSummary = draft.isGroupSummary,
                category = draft.category,
                channelId = draft.channelId,
                title = draft.title,
                text = draft.text,
                bigText = draft.bigText,
                subText = draft.subText,
                textLines = draft.textLines,
                postTime = draft.postTime,
                receivedAt = draft.receivedAt,
                contentHash = contentHash,
                contentVersion = (latest?.contentVersion ?: 0) + 1,
                observeCount = 1,
                parserVersion = parseResult.parserVersion,
                parseStatus = parseResult.status.name,
                contentAvailability = parseResult.availability.name,
                parsedMessageCount = parseResult.messages.size,
                parsedMessages = parseResult.messages,
                isGroupSummaryMessage = draft.isGroupSummaryMessage,
            ),
        )
        return Outcome.SAVED
    }

    suspend fun cleanupExpiredSnapshots(): Int {
        val retentionDays = settings.getRetentionDays()
        val cutoff = clock.nowMillis() - retentionDays * 24L * 60 * 60 * 1000
        return snapshotDao.deleteOlderThan(cutoff)
    }

    fun observeSnapshots(): Flow<List<NotificationSnapshotEntity>> =
        snapshotDao.observeAll()

    fun observeSnapshot(id: Long): Flow<NotificationSnapshotEntity?> =
        snapshotDao.observeSnapshot(id)

    fun observeSnapshotCount(): Flow<Int> = snapshotDao.observeCount()

    enum class Outcome { SAVED, DUPLICATE_CONTENT }

    private fun com.todoink.app.notification.model.NotificationSnapshotDraft.contentHash(): String {
        val meaningful = listOf(
            notificationKey,
            title.orEmpty(),
            text.orEmpty(),
            bigText.orEmpty(),
            subText.orEmpty(),
            textLines.joinToString("\n"),
            postTime.toString(),
        ).joinToString('\u001F'.toString())
        val digest = MessageDigest.getInstance("SHA-256").digest(meaningful.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }
}
