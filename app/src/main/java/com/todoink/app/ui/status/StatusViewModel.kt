package com.todoink.app.ui.status

import android.content.Context
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.todoink.app.data.repository.NotificationRepository
import com.todoink.app.data.settings.ListenerStatusStore
import com.todoink.app.data.settings.SourceSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class StatusUiState(
    /** null 表示尚未读取到系统授权状态（状态未知）。 */
    val accessGranted: Boolean? = null,
    val listenerConnected: Boolean = false,
    val lastCallbackAt: Long? = null,
    val lastSavedAt: Long? = null,
    val lastError: String? = null,
    val lastErrorAt: Long? = null,
    val snapshotCount: Int = 0,
    val enabledSourceCount: Int = 0,
)

@HiltViewModel
class StatusViewModel @Inject constructor(
    statusStore: ListenerStatusStore,
    repository: NotificationRepository,
    sourceSettings: SourceSettings,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val accessGranted = MutableStateFlow<Boolean?>(null)

    init {
        refreshAccessGranted()
    }

    /**
     * NI-005 第 1 步：在系统适配边界独立读取通知访问授权，
     * 不以监听连接状态代替；从系统设置返回时由页面触发刷新。
     */
    fun refreshAccessGranted() {
        accessGranted.value = NotificationManagerCompat.getEnabledListenerPackages(context)
            .contains(context.packageName)
    }

    val state: StateFlow<StatusUiState> = combine(
        statusStore.status,
        repository.observeSnapshotCount(),
        accessGranted,
        sourceSettings.whitelist,
    ) { status, count, granted, whitelist ->
        StatusUiState(
            accessGranted = granted,
            listenerConnected = status.listenerConnected,
            lastCallbackAt = status.lastCallbackAt,
            lastSavedAt = status.lastSavedAt,
            lastError = status.lastError,
            lastErrorAt = status.lastErrorAt,
            snapshotCount = count,
            enabledSourceCount = whitelist.size,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StatusUiState())
}
