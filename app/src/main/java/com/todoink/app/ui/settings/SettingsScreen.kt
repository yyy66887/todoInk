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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.todoink.app.ui.components.DefinitionRow
import com.todoink.app.ui.components.PageHeader
import com.todoink.app.ui.components.SettingRow

/**
 * 预览稿 P07 设置页：采集 / 数据与隐私 / 关于 分组卡片；
 * 候选待办分组随 Phase 2 候选处理服务（TD-005）提供，当前不显示。
 */
@Composable
fun SettingsScreen(
    onOpenSources: () -> Unit,
    onOpenStatus: () -> Unit,
    onOpenRetention: () -> Unit,
    onOpenAbout: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.refresh()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
    ) {
        PageHeader(
            title = "设置",
            subtitle = "让 TodoInk 按你的方式工作",
            sealIcon = Icons.Filled.Settings,
        )
        SettingGroup("采集") {
            SettingRow(
                title = "通知来源",
                value = "${state.enabledSourceCount} 个应用",
                onClick = onOpenSources,
            )
            SettingRow(
                title = "采集状态",
                value = when {
                    state.accessGranted == false -> "未授权"
                    state.accessGranted == true && state.listenerConnected -> "已授权 · 已连接"
                    state.accessGranted == true -> "已授权 · 未连接"
                    else -> "状态未知"
                },
                onClick = onOpenStatus,
            )
        }
        SettingGroup("数据与隐私") {
            SettingRow(
                title = "通知保留时间",
                value = state.retentionDays?.let { "$it 天" } ?: "—",
                onClick = onOpenRetention,
            )
            SettingRow(
                title = "数据清理说明",
                onClick = onOpenRetention,
            )
        }
        SettingGroup("关于") {
            SettingRow(
                title = "关于 TodoInk",
                value = "设计 v0.1",
                onClick = onOpenAbout,
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = "通知内容在本机处理。\n正式待办只有在你确认后才会创建。",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(24.dp))
    }
}

/** 预览稿 setting-group + setting-card。 */
@Composable
private fun SettingGroup(
    title: String,
    content: @Composable () -> Unit,
) {
    Column(modifier = Modifier.padding(vertical = 12.dp)) {
        Text(
            title,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 2.dp, bottom = 8.dp),
        )
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(15.dp),
            color = MaterialTheme.colorScheme.surface,
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        ) {
            Column { content() }
        }
    }
}
