package com.todoink.app.ui.notifications

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.todoink.app.data.db.NotificationSnapshotEntity
import com.todoink.app.data.repository.NotificationRepository
import com.todoink.app.ui.components.EmptyPanel
import com.todoink.app.ui.components.SubBar
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val detailTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    .withZone(ZoneId.systemDefault())

private fun formatTime(value: Long): String =
    detailTimeFormatter.format(Instant.ofEpochMilli(value))

private val availabilityLabel = mapOf(
    "FULL" to "完整正文",
    "PARTIAL" to "仅摘要（正文不完整）",
    "HIDDEN" to "内容不可用（系统隐藏或脱敏）",
    "EMPTY" to "无正文",
)

private val parseStatusLabel = mapOf(
    "PARSED" to "已解析",
    "NO_TEXT" to "无正文可解析",
    "UNSUPPORTED" to "解析不受支持（原始字段仍保存）",
)

sealed interface DetailState {
    data object Loading : DetailState
    data object NotFound : DetailState
    data class Content(val snapshot: NotificationSnapshotEntity) : DetailState
}

@HiltViewModel
class NotificationDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    repository: NotificationRepository,
) : ViewModel() {

    private val snapshotId: Long = checkNotNull(savedStateHandle["snapshotId"])

    val state: StateFlow<DetailState> = repository.observeSnapshot(snapshotId)
        .map { entity ->
            if (entity == null) DetailState.NotFound else DetailState.Content(entity)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DetailState.Loading)
}

@Composable
fun NotificationDetailScreen(
    onBack: () -> Unit,
    viewModel: NotificationDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        SubBar(title = "通知详情", onBack = onBack)
        when (val current = state) {
            DetailState.Loading -> EmptyPanel(
                icon = Icons.Filled.Notifications,
                title = "加载中…",
            )
            DetailState.NotFound -> EmptyPanel(
                icon = Icons.Filled.Notifications,
                title = "通知不存在或已被清理",
                description = "该记录可能因保留期到期被删除。",
            )
            is DetailState.Content -> DetailContent(current.snapshot)
        }
    }
}

/**
 * 设计稿 P06：默认“整理内容”，受控原始字段放在折叠区，
 * packageName / notificationKey / hash 等不默认暴露。
 */
@Composable
private fun DetailContent(snapshot: NotificationSnapshotEntity) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(snapshot.title ?: "（无标题）", style = MaterialTheme.typography.titleMedium)
                Text(
                    text = snapshot.packageName,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                DetailRow("内容可用性", availabilityLabel[snapshot.contentAvailability] ?: snapshot.contentAvailability)
                DetailRow("解析状态", parseStatusLabel[snapshot.parseStatus] ?: snapshot.parseStatus)
                DetailRow("已解析消息", "${snapshot.parsedMessageCount} 条")
                DetailRow("内容版本", "第 ${snapshot.contentVersion} 版 · 观察 ${snapshot.observeCount} 次")
                DetailRow("接收时间", formatTime(snapshot.receivedAt))
                DetailRow("通知 postTime", formatTime(snapshot.postTime))

                val body = snapshot.bigText
                if (!body.isNullOrBlank()) {
                    Text("展开正文（bigText）", style = MaterialTheme.typography.labelLarge)
                    Text(body, style = MaterialTheme.typography.bodyMedium)
                }
                if (snapshot.textLines.any { it.isNotBlank() }) {
                    Text("收件箱行（textLines）", style = MaterialTheme.typography.labelLarge)
                    snapshot.textLines.filter { it.isNotBlank() }.forEach { line ->
                        Text(line, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                val text = snapshot.text
                if (!text.isNullOrBlank()) {
                    Text("普通正文（text）", style = MaterialTheme.typography.labelLarge)
                    Text(text, style = MaterialTheme.typography.bodyMedium)
                }
                if (snapshot.subText?.isNotBlank() == true) {
                    Text("副文本（subText）", style = MaterialTheme.typography.labelLarge)
                    Text(snapshot.subText, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        RawFieldsCard(snapshot)
    }
}

@Composable
private fun RawFieldsCard(snapshot: NotificationSnapshotEntity) {
    var expanded by rememberSaveable { mutableStateOf(false) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "原始字段",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = if (expanded) "收起" else "展开",
                )
            }
            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier.padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    DetailRow("notificationKey", snapshot.notificationKey)
                    DetailRow("snapshotId", snapshot.snapshotId.toString())
                    DetailRow("notificationId", snapshot.notificationId.toString())
                    DetailRow("tag", snapshot.tag ?: "null")
                    DetailRow("groupKey", snapshot.groupKey ?: "null")
                    DetailRow("packageName", snapshot.packageName)
                    DetailRow("channelId", snapshot.channelId ?: "null")
                    DetailRow("category", snapshot.category ?: "null")
                    DetailRow("isGroupSummary", snapshot.isGroupSummary.toString())
                    DetailRow("postTime", snapshot.postTime.toString())
                    DetailRow("receivedAt", snapshot.receivedAt.toString())
                    DetailRow("contentHash", snapshot.contentHash)
                    DetailRow("contentVersion", snapshot.contentVersion.toString())
                    DetailRow("observeCount", snapshot.observeCount.toString())
                    DetailRow("parserVersion", snapshot.parserVersion.toString())
                    DetailRow("parseStatus", snapshot.parseStatus)
                    DetailRow("contentAvailability", snapshot.contentAvailability)
                    DetailRow("parsedMessageCount", snapshot.parsedMessageCount.toString())
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
    }
}
