package com.todoink.app.data.repository

import android.content.ContentValues
import android.content.Context
import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.todoink.app.data.db.NotificationSnapshotDao
import com.todoink.app.data.db.TodoInkDatabase
import com.todoink.app.data.settings.RetentionSettings
import com.todoink.app.notification.model.NotificationSnapshotDraft
import com.todoink.app.notification.model.ParsedMessage
import com.todoink.app.notification.parser.GenericParser
import com.todoink.app.time.AppClock
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * NI-003 步骤 B 回归：真实 Room（文件库重开与 v1→v2 迁移）验证消息结构
 * 持久化、受控字符无损往返与旧数据保留。
 */
@RunWith(AndroidJUnit4::class)
class NotificationContentStorageTest {

    private val dbName = "ni003-content-test.db"
    private val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        TodoInkDatabase::class.java,
    )

    private class FakeClock : AppClock {
        override fun nowMillis(): Long = 1_700_000_000_000L
    }

    private class FakeRetention : RetentionSettings {
        override suspend fun getRetentionDays(): Int = 3
    }

    @After
    fun tearDown() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.deleteDatabase(dbName)
    }

    private fun draft(
        key: String,
        messages: List<ParsedMessage> = emptyList(),
        textLines: List<String> = emptyList(),
        text: String? = null,
        bigText: String? = null,
    ) = NotificationSnapshotDraft(
        notificationKey = key,
        notificationId = 7,
        tag = null,
        packageName = "com.example.app",
        groupKey = null,
        isGroupSummary = false,
        category = "msg",
        channelId = "chats",
        title = "标题：中文",
        text = text,
        bigText = bigText,
        subText = null,
        textLines = textLines,
        postTime = 1_000L,
        receivedAt = 2_000L,
        messagingStyleMessages = messages,
        isRedacted = false,
        isGroupSummaryMessage = false,
    )

    private suspend fun buildRepository(db: TodoInkDatabase): NotificationRepository =
        NotificationRepository(
            db.notificationSnapshotDao(),
            FakeRetention(),
            FakeClock(),
        )

    @Test
    fun messagingStyle消息保存后重开数据库仍可恢复() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val messages = listOf(
            ParsedMessage(senderName = "张三", text = "第一行\n换行·U+001F≈分隔·中文", messageTimestamp = 1_234L),
            ParsedMessage(senderName = null, text = "", messageTimestamp = null),
        )
        val parser = GenericParser()
        val draft = draft("key-style", messages = messages)

        TodoInkDatabase.MIGRATION_1_2.let {
            val first = Room.databaseBuilder(context, TodoInkDatabase::class.java, dbName)
                .addMigrations(it)
                .build()
            val repository = buildRepository(first)
            val outcome = repository.saveSnapshot(draft, parser.parse(draft))
            assertEquals(NotificationRepository.Outcome.SAVED, outcome)
            first.close()
        }

        val reopened = Room.databaseBuilder(context, TodoInkDatabase::class.java, dbName)
            .addMigrations(TodoInkDatabase.MIGRATION_1_2)
            .build()
        try {
            val restored = reopened.notificationSnapshotDao().findLatestByKey("key-style")
            assertTrue(restored != null)
            assertEquals(messages, restored!!.parsedMessages)
            assertEquals(2, restored.parsedMessageCount)
            assertEquals("PARSED", restored.parseStatus)
            assertEquals("FULL", restored.contentAvailability)
        } finally {
            reopened.close()
        }
    }

    @Test
    fun 降级正文消息与特殊字符往返无损() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, TodoInkDatabase::class.java).build()
        try {
            val repository = buildRepository(db)
            val parser = GenericParser()
            val trickyLines = listOf(
                "带换行\n正文",
                "单元分隔符与|混合",
                "中文·🚀 emoji",
                "  前后空格  ",
            )
            val draft = draft(
                "key-degraded",
                textLines = trickyLines,
                text = "仅text\n含U+001F字符",
            )
            repository.saveSnapshot(draft, parser.parse(draft))

            val saved = db.notificationSnapshotDao().findLatestByKey("key-degraded")
            assertTrue(saved != null)
            // textLines 逐行降级为消息（含空白过滤前的原行文本）
            assertEquals(trickyLines, saved!!.textLines)
            assertEquals(trickyLines, saved.parsedMessages.map { it.text })
            assertEquals("仅text\n含U+001F字符", saved.text)
            assertTrue(saved.parsedMessages.all { it.senderName == null && it.messageTimestamp == null })
        } finally {
            db.close()
        }
    }

    @Test
    fun v1数据库迁移到v2保留旧数据并给出默认值() = runTest {
        helper.createDatabase(dbName, 1).use { db ->
            val values = ContentValues().apply {
                put("snapshotId", 1L)
                put("notificationKey", "v1-key")
                put("notificationId", 3)
                put("tag", null as String?)
                put("packageName", "com.example.old")
                put("groupKey", null as String?)
                put("isGroupSummary", 0)
                put("category", null as String?)
                put("channelId", null as String?)
                put("title", "旧标题")
                put("text", "旧正文")
                put("bigText", null as String?)
                put("subText", null as String?)
                put("textLines", "行A行B")
                put("postTime", 111L)
                put("receivedAt", 222L)
                put("contentHash", "hash-v1")
                put("contentVersion", 1)
                put("observeCount", 2)
                put("parserVersion", 1)
                put("parseStatus", "PARSED")
                put("contentAvailability", "FULL")
                put("parsedMessageCount", 0)
            }
            db.insert("notification_snapshots", 0, values)
        }

        val migrated = helper.runMigrationsAndValidate(dbName, 2, true, TodoInkDatabase.MIGRATION_1_2)
        migrated.query("SELECT notificationKey, text, textLines, isGroupSummaryMessage, parsedMessages, parsedMessageCount FROM notification_snapshots").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("v1-key", cursor.getString(0))
            assertEquals("旧正文", cursor.getString(1))
            assertEquals("行A行B", cursor.getString(2))
            assertEquals(0L, cursor.getLong(3))
            assertEquals("", cursor.getString(4))
            assertEquals(0, cursor.getInt(5))
        }
        migrated.close()
    }
}
