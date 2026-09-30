package com.todoink.app.ui.notifications

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.todoink.app.data.db.NotificationSnapshotEntity
import com.todoink.app.data.repository.NotificationRepository
import com.todoink.app.data.settings.SourceSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class NotificationsUiState(
    val loading: Boolean = true,
    val snapshots: List<NotificationSnapshotEntity> = emptyList(),
    /** 已选择的来源（按显示名排序），供筛选行展示。 */
    val sources: List<SourceFilter> = emptyList(),
    /** 当前筛选的来源；null 表示全部。 */
    val selectedSource: String? = null,
)

data class SourceFilter(
    val packageName: String,
    val label: String,
)

@HiltViewModel
class NotificationsViewModel @Inject constructor(
    repository: NotificationRepository,
    sourceSettings: SourceSettings,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val selectedSource = MutableStateFlow<String?>(null)
    private val labelCache = ConcurrentHashMap<String, String>()

    private fun labelFor(packageName: String): String = labelCache.getOrPut(packageName) {
        runCatching {
            val pm = context.packageManager
            pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString()
        }.getOrDefault(packageName)
    }

    val uiState: StateFlow<NotificationsUiState> = combine(
        repository.observeSnapshots(),
        sourceSettings.whitelist,
        selectedSource,
    ) { snapshots, whitelist, selected ->
        NotificationsUiState(
            loading = false,
            snapshots = if (selected == null) {
                snapshots
            } else {
                snapshots.filter { it.packageName == selected }
            },
            sources = whitelist
                .map { SourceFilter(it, labelFor(it)) }
                .sortedBy { it.label.lowercase() },
            selectedSource = selected,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), NotificationsUiState())

    /** 设计稿 P05：来源筛选只在展示层生效，不写库。 */
    fun selectSource(packageName: String?) {
        selectedSource.value = packageName
    }
}
