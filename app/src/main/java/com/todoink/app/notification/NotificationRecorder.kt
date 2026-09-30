package com.todoink.app.notification

import com.todoink.app.data.repository.NotificationRepository
import com.todoink.app.data.settings.SourceSettings
import com.todoink.app.data.settings.StatusRecorder
import com.todoink.app.di.ApplicationScope
import com.todoink.app.notification.model.NotificationSnapshotDraft
import com.todoink.app.notification.parser.ParserRegistry
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * 采集管道：入队后有界队列异步完成解析、去重和落库。
 * 队列满与落库失败都会记录到状态页（方案 2.4）。
 */
@Singleton
class NotificationRecorder @Inject constructor(
    private val settings: SourceSettings,
    private val statusStore: StatusRecorder,
    private val repository: NotificationRepository,
    private val parserRegistry: ParserRegistry,
    @ApplicationScope private val appScope: CoroutineScope,
) {
    private val queue = Channel<NotificationSnapshotDraft>(
        capacity = QUEUE_CAPACITY,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    private val saveMutex = Mutex()

    init {
        appScope.launch {
            for (draft in queue) {
                process(draft)
            }
        }
    }

    /**
     * 来源已由 [NotificationIntake] 判断通过后入队。
     * 返回 false 表示队列已满，该项被拒绝。
     */
    fun enqueueDraft(draft: NotificationSnapshotDraft): Boolean {
        val offered = queue.trySend(draft).isSuccess
        if (!offered) {
            statusStore.recordError(ERROR_QUEUE_FULL)
        }
        return offered
    }

    private suspend fun process(draft: NotificationSnapshotDraft) {
        // C02：排队期间来源可能被关闭。尚未开始持久化的项在此复核后直接丢弃；
        // 已开始提交的保存不追溯取消。
        if (!settings.isWhitelisted(draft.packageName)) return
        try {
            val parseResult = parserRegistry.parse(draft)
            val outcome = saveMutex.withLock {
                repository.saveSnapshot(draft, parseResult)
            }
            when (outcome) {
                NotificationRepository.Outcome.SAVED -> statusStore.recordSaved()
                NotificationRepository.Outcome.DUPLICATE_CONTENT -> Unit
            }
        } catch (e: Exception) {
            statusStore.recordError(ERROR_SAVE_FAILED)
        }
    }

    private companion object {
        const val QUEUE_CAPACITY = 128
        const val ERROR_QUEUE_FULL = "queue_overflow"
        const val ERROR_SAVE_FAILED = "save_failed"
    }
}
