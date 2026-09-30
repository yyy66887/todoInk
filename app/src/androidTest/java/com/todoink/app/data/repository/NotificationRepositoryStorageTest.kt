package com.todoink.app.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.todoink.app.data.db.NotificationSnapshotDao
import com.todoink.app.data.db.TodoInkDatabase
import com.todoink.app.data.settings.SettingsRepository
import com.todoink.app.notification.model.NotificationSnapshotDraft
import com.todoink.app.notification.parser.GenericParser
import com.todoink.app.notification.parser.ParseResult
import com.todoink.app.time.AppClock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * M0 真实存储行为回归：内存 Room + 真实 SettingsRepository + 可控时钟，
 * 全部调用生产 [NotificationRepository]，不使用复制业务实现的测试替身。
 */
@RunWith(AndroidJUnit4::class)
class NotificationRepositoryStorageTest {

    /** 可控墙钟：测试中显式推进时间，验证保留期边界。 */
    private class FakeAppClock(var now: Long) : AppClock {
        override fun nowMillis(): Long = now
    }

    private lateinit var db: TodoInkDatabase
    private lateinit var dao: NotificationSnapshotDao
    private lateinit var settings: SettingsRepository
    private lateinit var repository: NotificationRepository
    private val clock = FakeAppClock(now = 1_700_000_000_000L)
    private val settingsScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(context, TodoInkDatabase::class.java).build()
        dao = db.notificationSnapshotDao()
        settings = SettingsRepository(context, settingsScope)
        repository = NotificationRepository(dao, settings, clock)
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun draft(
        key: String,
        title: String?,
        receivedAt: Long,
    ) = NotificationSnapshotDraft(
        notificationKey = key,
        notificationId = 1,
        tag = null,
        packageName = "com.example.app",
        groupKey = null,
        isGroupSummary = false,
        category = "msg",
        channelId = "chats",
        title = title,
        text = "正文内容",
        bigText = null,
        subText = null,
        textLines = emptyList(),
        postTime = 1_000L,
        receivedAt = receivedAt,
        messagingStyleMessages = emptyList(),
        isRedacted = false,
        isGroupSummaryMessage = false,
    )

    private suspend fun save(vararg drafts: NotificationSnapshotDraft) {
        val parser = GenericParser()
        drafts.forEach { draft ->
            val parseResult: ParseResult = parser.parse(draft)
            repository.saveSnapshot(draft, parseResult)
        }
    }

    @Test
    fun saveSnapshot_newKey_savesVersion1WithObserveCount1() = runTest {
        val outcome = repository.saveSnapshot(draft("key-a", "标题A", clock.now), GenericParser().parse(draft("key-a", "标题A", clock.now)))

        assertEquals(NotificationRepository.Outcome.SAVED, outcome)
        val saved = dao.findLatestByKey("key-a")
        assertNotNull(saved)
        assertEquals(1, saved!!.contentVersion)
        assertEquals(1, saved.observeCount)
        assertEquals("标题A", saved.title)
    }

    @Test
    fun saveSnapshot_sameContent_accumulatesObserveCountWithoutNewVersion() = runTest {
        val draft = draft("key-b", "标题B", clock.now)
        save(draft, draft)

        val saved = dao.findLatestByKey("key-b")
        assertNotNull(saved)
        assertEquals(2, saved!!.observeCount)
        assertEquals(1, saved.contentVersion)
    }

    @Test
    fun saveSnapshot_contentChange_createsVersion2() = runTest {
        save(draft("key-c", "旧标题", clock.now))
        save(draft("key-c", "新标题", clock.now + 5_000))

        val saved = dao.findLatestByKey("key-c")
        assertNotNull(saved)
        assertEquals(2, saved!!.contentVersion)
        assertEquals("新标题", saved.title)
    }

    @Test
    fun cleanupExpiredSnapshots_deletesOnlyRowsOlderThanRetention() = runTest {
        settings.setRetentionDays(3)
        val now = clock.now
        val recent = draft("key-recent", "保留", now - 1L * 24 * 60 * 60 * 1000)
        val boundary = draft("key-boundary", "边界保留", now - 3L * 24 * 60 * 60 * 1000)
        val expired = draft("key-expired", "过期删除", now - 4L * 24 * 60 * 60 * 1000)
        save(recent, boundary, expired)

        val deleted = repository.cleanupExpiredSnapshots()

        assertEquals(1, deleted)
        assertNotNull(dao.findLatestByKey("key-recent"))
        // 清理使用 receivedAt < cutoff，恰好等于边界不删除
        assertNotNull(dao.findLatestByKey("key-boundary"))
        assertNull(dao.findLatestByKey("key-expired"))
    }
}
