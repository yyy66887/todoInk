package com.todoink.app.ui.settings

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.todoink.app.data.settings.SourceSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class SourceApp(
    val packageName: String,
    val label: String,
    val isSystemApp: Boolean,
)

data class SourcesUiState(
    val loading: Boolean = true,
    val apps: List<SourceApp> = emptyList(),
    val whitelist: Set<String> = emptySet(),
    val query: String = "",
)

@HiltViewModel
class SourcesViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val sourceSettings: SourceSettings,
) : ViewModel() {

    private val loadedApps = MutableStateFlow<List<SourceApp>>(emptyList())
    private val query = MutableStateFlow("")

    init {
        viewModelScope.launch {
            loadedApps.value = loadLaunchableApps()
        }
    }

    fun setQuery(value: String) {
        query.value = value
    }

    fun setWhitelisted(packageName: String, allowed: Boolean) {
        viewModelScope.launch {
            sourceSettings.setWhitelisted(packageName, allowed)
        }
    }

    val state: StateFlow<SourcesUiState> = combine(
        loadedApps,
        sourceSettings.whitelist,
        query,
    ) { apps, whitelist, q ->
        SourcesUiState(
            loading = apps.isEmpty(),
            apps = apps.filter {
                it.label.contains(q, ignoreCase = true) || it.packageName.contains(q, ignoreCase = true)
            },
            whitelist = whitelist,
            query = q,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SourcesUiState())

    private suspend fun loadLaunchableApps(): List<SourceApp> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val launchIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        pm.queryIntentActivities(launchIntent, 0)
            .asSequence()
            .mapNotNull { it.activityInfo?.applicationInfo }
            .distinctBy { it.packageName }
            .filter { it.packageName != context.packageName }
            .map {
                SourceApp(
                    packageName = it.packageName,
                    label = pm.getApplicationLabel(it).toString(),
                    isSystemApp = (it.flags and ApplicationInfo.FLAG_SYSTEM) != 0,
                )
            }
            .sortedBy { it.label.lowercase() }
            .toList()
    }
}
