package com.todoink.app.ui.status

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.todoink.app.ui.components.DefinitionRow
import com.todoink.app.ui.components.SubBar
import com.todoink.app.ui.theme.todoInkExtendedColors
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val timeFormatter = DateTimeFormatter.ofPattern("M月d日 HH:mm")
    .withZone(ZoneId.systemDefault())

private fun formatTime(value: Long?): String =
    value?.let { timeFormatter.format(Instant.ofEpochMilli(it)) } ?: "—"

/** 预览稿 P09 采集状态页：statepanel + definition 行，授权与连接分别表达。 */
@Composable
fun StatusScreen(onBack: () -> Unit, viewModel: StatusViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val extended = todoInkExtendedColors()

    // 设计稿 P09：从系统授权设置返回时刷新授权状态
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.refreshAccessGranted()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
    ) {
        SubBar(title = "采集状态", onBack = onBack)
        Spacer(Modifier.height(8.dp))
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = when {
                state.accessGranted == false -> extended.warningContainer
                !state.listenerConnected -> extended.warningContainer
                else -> extended.successContainer
            },
            contentColor = when {
                state.accessGranted == false -> extended.onWarningContainer
                !state.listenerConnected -> extended.onWarningContainer
                else -> extended.onSuccessContainer
            },
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(
                    Icons.Filled.Shield,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                )
                Text(
                    text = when {
                        state.accessGranted == false -> "通知访问未开启"
                        !state.listenerConnected -> "已授权，监听服务未连接"
                        else -> "通知采集已就绪"
                    },
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = when {
                        state.accessGranted == false ->
                            "开启后，选定来源的新通知才会保存在本机"
                        !state.listenerConnected ->
                            "系统可能已停止采集；已保存的数据仍可查看"
                        else ->
                            "选定来源的新通知会保存在本机"
                    },
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }

        DefinitionRow(
            label = "通知访问",
            value = when (state.accessGranted) {
                true -> "已授权"
                false -> "未授权"
                null -> "状态未知"
            },
        )
        DefinitionRow(label = "监听服务", value = if (state.listenerConnected) "已连接" else "未连接")
        DefinitionRow(label = "启用来源", value = "${state.enabledSourceCount} 个应用")
        DefinitionRow(label = "最近收到", value = formatTime(state.lastCallbackAt))
        DefinitionRow(label = "最后保存", value = formatTime(state.lastSavedAt))
        DefinitionRow(
            label = "最近错误",
            value = state.lastError?.let { "$it（${formatTime(state.lastErrorAt)}）" } ?: "无",
        )
        DefinitionRow(label = "已保存快照", value = "${state.snapshotCount} 条")

        Button(
            onClick = {
                context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
            },
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 50.dp),
        ) {
            Text("打开通知访问权限设置")
        }

        Spacer(Modifier.height(12.dp))
        Text(
            text = "授权与连接是两个独立状态：授权表示系统允许读取，连接表示监听服务当前在运行。" +
                "被目标 App 与系统设置决定的内容差异（预览关闭、仅摘要、OTP 隐藏等）" +
                "会记录在每条快照的可用性标记中。",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(24.dp))
    }
}
