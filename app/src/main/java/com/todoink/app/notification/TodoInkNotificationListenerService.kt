package com.todoink.app.notification

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * 系统通知监听。回调运行在主线程（Android 7.0+），
 * 这里只做提取与入队，解析和落库都在 [NotificationRecorder] 的协程里（方案 2.4）。
 */
@AndroidEntryPoint
class TodoInkNotificationListenerService : NotificationListenerService() {

    @Inject
    lateinit var intake: NotificationIntake

    @Inject
    lateinit var snapshotExtractor: SnapshotExtractor

    @Inject
    lateinit var listenerStatusStore: com.todoink.app.data.settings.ListenerStatusStore

    override fun onListenerConnected() {
        listenerStatusStore.setListenerConnected(true)
        // 补采当前仍活跃的通知，走同一去重流程；无法恢复已消失的历史通知。
        activeNotifications?.forEach { enqueue(it) }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        enqueue(sbn)
    }

    override fun onListenerDisconnected() {
        listenerStatusStore.setListenerConnected(false)
        // 系统断开监听时请求重连，由系统自行退避，不反复强制拉起。
        requestRebind(
            android.content.ComponentName(this, TodoInkNotificationListenerService::class.java),
        )
    }

    private fun enqueue(sbn: StatusBarNotification) {
        // 包名是回调字段，不属于正文提取；未选来源不会执行 extract（C01）。
        intake.onNotificationArrived(sbn.packageName) { snapshotExtractor.extract(sbn) }
    }
}
