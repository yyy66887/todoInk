package com.todoink.app.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.todoink.app.data.settings.RetentionSettings
import com.todoink.app.ui.components.DefinitionRow
import com.todoink.app.ui.components.SubBar
import com.todoink.app.ui.theme.todoInkExtendedColors
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class RetentionUiState(
    val loading: Boolean = true,
    val retentionDays: Int = 3,
)

@HiltViewModel
class RetentionViewModel @Inject constructor(
    retentionSettings: RetentionSettings,
) : ViewModel() {

    private val days = MutableStateFlow<Int?>(null)

    init {
        viewModelScope.launch {
            days.value = retentionSettings.getRetentionDays()
        }
    }

    /** 当前 Repository 实际使用的保留天数；修改入口随 NI-006 提供。 */
    val state: StateFlow<RetentionUiState> = days
        .map { value -> RetentionUiState(loading = value == null, retentionDays = value ?: 3) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RetentionUiState())
}

/** 预览稿 retention 页：真实保留期值，只读；修改入口随 NI-006 提供。 */
@Composable
fun RetentionScreen(
    onBack: () -> Unit,
    viewModel: RetentionViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val extended = todoInkExtendedColors()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
    ) {
        SubBar(title = "数据保留与清理", onBack = onBack)
        DefinitionRow(
            label = "当前保留时间",
            value = if (state.loading) "读取中…" else "${state.retentionDays} 天",
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = "原通知会清理，\n确认的事情会留下。",
            style = MaterialTheme.typography.headlineSmall,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = "原始通知与未确认内容按保留期清理；已确认、已完成的待办保留你确认后的内容。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(16.dp))
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            color = extended.warningContainer,
            contentColor = extended.onWarningContainer,
        ) {
            Text(
                text = "来源过期后，相关通知详情会显示“原通知已清理”；正式待办不会因原文过期而被一同删除。",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(12.dp),
            )
        }
        Spacer(Modifier.height(16.dp))
        Text(
            text = "保留时间的调整入口将随保留期设置任务提供。",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(24.dp))
    }
}
