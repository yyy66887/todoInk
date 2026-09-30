package com.todoink.app.notification

import com.todoink.app.data.settings.SourceSettings
import com.todoink.app.data.settings.StatusRecorder
import com.todoink.app.notification.model.NotificationSnapshotDraft
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 通知唯一接收入口：实时回调与重连补采都必须经过这里（C01）。
 * 在正文提取之前完成来源判断：未选择的来源不调用 extract，不产生任何正文复制。
 */
@Singleton
class NotificationIntake @Inject constructor(
    private val settings: SourceSettings,
    private val statusStore: StatusRecorder,
    private val recorder: NotificationRecorder,
) {

    /**
     * 在监听回调线程调用。仅在来源已选择时才执行 [extract]（回调只做允许判断与轻量复制，C03）。
     * 返回 false 表示来源未选择（未入队），或来源已选择但队列拒绝。
     */
    fun onNotificationArrived(
        packageName: String,
        extract: () -> NotificationSnapshotDraft,
    ): Boolean {
        statusStore.recordCallback()
        if (!settings.isWhitelisted(packageName)) return false
        return recorder.enqueueDraft(extract())
    }
}
