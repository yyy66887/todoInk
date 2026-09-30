package com.todoink.app.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.todoink.app.notification.model.ParsedMessage

/**
 * 一次通知回调观察到的快照（方案 2.2 的三层模型中的 NotificationSnapshot 层）。
 *
 * 同一个通知 key 的内容变化会以递增 contentVersion 保留多份；
 * 内容未变化的重复回调只累加 observeCount，不产生新记录。
 */
@Entity(
    tableName = "notification_snapshots",
    indices = [
        Index("notificationKey"),
        Index("packageName"),
        Index("receivedAt"),
    ],
)
data class NotificationSnapshotEntity(
    @PrimaryKey(autoGenerate = true)
    val snapshotId: Long = 0,
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
    /** 系统标记的分组汇总通知（FLAG_GROUP_SUMMARY）；isGroupSummary 仅表示属于分组。 */
    @ColumnInfo(defaultValue = "0")
    val isGroupSummaryMessage: Boolean = false,
    /** 对结构化正文字段的稳定哈希，用于快照层去重。 */
    val contentHash: String,
    /** 同一 notificationKey 下的内容版本号，从 1 开始。 */
    val contentVersion: Int,
    /** 相同内容被观察到的次数。 */
    val observeCount: Int,
    val parserVersion: Int,
    /** PARSED / NO_TEXT / UNSUPPORTED */
    val parseStatus: String,
    /** FULL / PARTIAL / HIDDEN / EMPTY */
    val contentAvailability: String,
    /** 本次解析分离出的消息条数。 */
    val parsedMessageCount: Int,
    /** 解析得到的 0..n 条受控消息（NI-003 步骤 B），重开后可恢复 Normalized 详情。 */
    @ColumnInfo(defaultValue = "''")
    val parsedMessages: List<ParsedMessage> = emptyList(),
)
