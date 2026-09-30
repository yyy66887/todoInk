package com.todoink.app.ui.settings

import android.content.Context
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.todoink.app.data.settings.ListenerStatusStore
import com.todoink.app.data.settings.RetentionSettings
import com.todoink.app.data.settings.SourceSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    /** null 表示授权状态未知。 */
    val accessGranted: Boolean? = null,
    val listenerConnected: Boolean = false,
    val enabledSourceCount: Int = 0,
    val retentionDays: Int? = null,
)

/** 预览稿 P07 设置页状态：采集 / 数据与隐私分组所需的聚合只读状态。 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    statusStore: ListenerStatusStore,
    sourceSettings: SourceSettings,
    private val retentionSettings: RetentionSettings,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val accessGranted = MutableStateFlow<Boolean?>(null)
    private val retentionDays = MutableStateFlow<Int?>(null)

    init {
        refresh()
        viewModelScope.launch {
            retentionDays.value = retentionSettings.getRetentionDays()
        }
    }

    fun refresh() {
        accessGranted.value = NotificationManagerCompat.getEnabledListenerPackages(context)
            .contains(context.packageName)
    }

    val state: StateFlow<SettingsUiState> = combine(
        statusStore.status,
        sourceSettings.whitelist,
        accessGranted,
        retentionDays,
    ) { status, whitelist, granted, retention ->
        SettingsUiState(
            accessGranted = granted,
            listenerConnected = status.listenerConnected,
            enabledSourceCount = whitelist.size,
            retentionDays = retention,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())
}
