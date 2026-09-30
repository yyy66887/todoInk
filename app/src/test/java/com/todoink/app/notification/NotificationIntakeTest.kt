package com.todoink.app.notification

import com.todoink.app.data.db.NotificationSnapshotDao
import com.todoink.app.data.db.NotificationSnapshotEntity
import com.todoink.app.data.repository.NotificationRepository
import com.todoink.app.data.settings.RetentionSettings
import com.todoink.app.data.settings.SourceSettings
import com.todoink.app.data.settings.StatusRecorder
import com.todoink.app.notification.model.NotificationSnapshotDraft
import com.todoink.app.notification.parser.GenericParser
import com.todoink.app.notification.parser.ParserRegistry
import com.todoink.app.time.AppClock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * NI-001 V01 / V02 回归：直接调用生产 [NotificationIntake] 与 [NotificationRecorder]，
 * 通过可控设置缝与可控调度器验证“提取前过滤”和“关闭来源后排队项不保存”。
 */
class NotificationIntakeTest {

    private class FakeSourceSettings : SourceSettings {
        private val allowed = mutableSetOf<String>()
        private val flow = MutableStateFlow(allowed.toSet())
        override val whitelist: StateFlow<Set<String>> = flow

        override fun isWhitelisted(packageName: String): Boolean =
            allowed.contains(packageName)

        override suspend fun setWhitelisted(packageName: String, allowed: Boolean) {
            if (allowed) this.allowed.add(packageName) else this.allowed.remove(packageName)
            flow.value = this.allowed.toSet()
        }
    }

    private class FakeStatusRecorder : StatusRecorder {
        var callbacks = 0
        var saves = 0
        val errors = mutableListOf<String>()

        override fun recordCallback() {
            callbacks++
        }

        override fun recordSaved() {
            saves++
        }

        override fun recordError(message: String) {
            errors.add(message)
        }
    }

    private class FakeDao : NotificationSnapshotDao {
        val inserted = mutableListOf<NotificationSnapshotEntity>()

        override fun observeAll(): Flow<List<NotificationSnapshotEntity>> =
            flowOf(emptyList())

        override fun observeCount(): Flow<Int> = flowOf(0)

        override suspend fun findLatestByKey(key: String): NotificationSnapshotEntity? = null

        override fun observeSnapshot(id: Long): Flow<NotificationSnapshotEntity?> =
            flowOf(null)

        override suspend fun insert(entity: NotificationSnapshotEntity): Long {
            inserted.add(entity)
            return inserted.size.toLong()
        }

        override suspend fun incrementObserveCount(id: Long) = Unit

        override suspend fun deleteOlderThan(cutoff: Long): Int = 0
    }

    private class FakeAppClock : AppClock {
        override fun nowMillis(): Long = 1_700_000_000_000L
    }

    private class FakeRetentionSettings : RetentionSettings {
        override suspend fun getRetentionDays(): Int = 3
    }

    private class Env(scope: TestScope) {
        val scheduler = scope.testScheduler
        val dispatcher = StandardTestDispatcher(scope.testScheduler)
        val settings = FakeSourceSettings()
        val status = FakeStatusRecorder()
        val dao = FakeDao()
        val repository = NotificationRepository(dao, FakeRetentionSettings(), FakeAppClock())
        val recorder = NotificationRecorder(
            settings,
            status,
            repository,
            ParserRegistry(GenericParser()),
            CoroutineScope(dispatcher + Job()),
        )
        val intake = NotificationIntake(settings, status, recorder)

        fun draft(packageName: String) = NotificationSnapshotDraft(
            notificationKey = "$packageName|1|tag",
            notificationId = 1,
            tag = "tag",
            packageName = packageName,
            groupKey = null,
            isGroupSummary = false,
            category = "msg",
            channelId = "chats",
            title = "标题",
            text = "正文",
            bigText = null,
            subText = null,
            textLines = emptyList(),
            postTime = 1_000L,
            receivedAt = 2_000L,
            messagingStyleMessages = emptyList(),
            isRedacted = false,
            isGroupSummaryMessage = false,
        )
    }

    @Test
    fun `V01 未选来源不调用提取器不入队不写库`() = runTest {
        val env = Env(this)
        var extractCalls = 0

        val accepted = env.intake.onNotificationArrived("com.other.app") {
            extractCalls++
            env.draft("com.other.app")
        }

        assertFalse(accepted)
        assertEquals(0, extractCalls)
        env.scheduler.runCurrent()
        assertEquals(0, env.dao.inserted.size)
        assertEquals(1, env.status.callbacks)
        assertEquals(0, env.status.saves)
    }

    @Test
    fun `V01 来源选中后同一入口正常采集`() = runTest {
        val env = Env(this)
        var extractCalls = 0
        env.settings.setWhitelisted("com.a", true)

        val accepted = env.intake.onNotificationArrived("com.a") {
            extractCalls++
            env.draft("com.a")
        }

        assertTrue(accepted)
        assertEquals(1, extractCalls)
        env.scheduler.runCurrent()
        assertEquals(1, env.dao.inserted.size)
        assertEquals("com.a", env.dao.inserted.single().packageName)
        assertEquals(1, env.status.saves)
    }

    @Test
    fun `V01 重连补采与实时回调走同一入口`() = runTest {
        val env = Env(this)
        env.settings.setWhitelisted("com.a", true)

        // onListenerConnected 的补采：对当前活跃通知逐个调用同一入口
        env.intake.onNotificationArrived("com.a") { env.draft("com.a") }
        env.intake.onNotificationArrived("com.b") { env.draft("com.b") }
        env.scheduler.runCurrent()

        assertEquals(1, env.dao.inserted.size)
        assertEquals("com.a", env.dao.inserted.single().packageName)
    }

    @Test
    fun `V02 入队后关闭来源，未开始持久化的排队项不保存`() = runTest {
        val env = Env(this)
        env.settings.setWhitelisted("com.a", true)

        // 入队但未放行消费者
        assertTrue(env.intake.onNotificationArrived("com.a") { env.draft("com.a") })
        assertEquals(0, env.dao.inserted.size)

        // 关闭来源成功（缓存同步更新），再放行消费者
        env.settings.setWhitelisted("com.a", false)
        env.scheduler.runCurrent()

        assertEquals(0, env.dao.inserted.size)
        assertEquals(0, env.status.saves)
    }

    @Test
    fun `V02 关闭后新回调被拒绝，重新开启后恢复`() = runTest {
        val env = Env(this)
        var extractCalls = 0

        env.settings.setWhitelisted("com.a", true)
        assertTrue(env.intake.onNotificationArrived("com.a") { env.draft("com.a") })
        env.scheduler.runCurrent()
        assertEquals(1, env.dao.inserted.size)

        env.settings.setWhitelisted("com.a", false)
        assertFalse(env.intake.onNotificationArrived("com.a") { extractCalls++; env.draft("com.a") })
        env.scheduler.runCurrent()
        assertEquals(0, extractCalls)
        assertEquals(1, env.dao.inserted.size)

        env.settings.setWhitelisted("com.a", true)
        assertTrue(env.intake.onNotificationArrived("com.a") { env.draft("com.a") })
        env.scheduler.runCurrent()
        assertEquals(2, env.dao.inserted.size)
    }
}
