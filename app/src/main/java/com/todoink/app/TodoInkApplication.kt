package com.todoink.app

import android.app.Application
import com.todoink.app.data.repository.NotificationRepository
import com.todoink.app.di.ApplicationScope
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@HiltAndroidApp
class TodoInkApplication : Application() {

    @Inject
    lateinit var notificationRepository: NotificationRepository

    @Inject
    @ApplicationScope
    lateinit var appScope: CoroutineScope

    override fun onCreate() {
        super.onCreate()
        // 启动时按保留期清理过期快照；不级联删除任何用户数据。
        // 周期性清理后续迁移到 WorkManager（方案 4.2）。
        appScope.launch {
            runCatching { notificationRepository.cleanupExpiredSnapshots() }
        }
    }
}
