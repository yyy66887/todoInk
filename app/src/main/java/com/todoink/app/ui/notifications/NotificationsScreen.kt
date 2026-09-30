package com.todoink.app.ui.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.todoink.app.data.db.NotificationSnapshotEntity
import com.todoink.app.ui.components.EmptyPanel
import com.todoink.app.ui.components.FilterPills
import com.todoink.app.ui.components.PageHeader
import com.todoink.app.ui.components.SourceBadge
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    .withZone(ZoneId.systemDefault())

private val availabilityLabel = mapOf(
    "FULL" to "完整正文",
    "PARTIAL" to "仅摘要",
    "HIDDEN" to "内容不可用",
    "EMPTY" to "无正文",
)

@Composable
fun NotificationsScreen(
    onOpenSnapshot: (Long) -> Unit,
    viewModel: NotificationsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            PageHeader(
                title = "通知",
                subtitle = state.snapshots.maxOfOrNull { it.receivedAt }
                    ?.let { "最近收到 ${timeFormatter.format(Instant.ofEpochMilli(it))}" }
                    ?: "还没有采集到通知",
                sealIcon = Icons.Filled.Notifications,
            )
            if (state.sources.isNotEmpty()) {
                FilterPills(
                    options = listOf("全部") + state.sources.map { it.label },
                    selected = state.selectedSource
                        ?.let { pkg -> state.sources.find { it.packageName == pkg }?.label }
                        ?: "全部",
                    onSelect = { label ->
                        viewModel.selectSource(
                            if (label == "全部") null else state.sources.find { it.label == label }?.packageName,
                        )
                    },
                )
                Spacer(Modifier.height(12.dp))
            }
        }
        when {
            state.loading -> EmptyPanel(
                icon = Icons.Filled.Notifications,
                title = "加载中…",
            )
            state.snapshots.isEmpty() -> if (state.selectedSource == null) {
                EmptyPanel(
                    icon = Icons.Filled.Notifications,
                    title = "还没有通知记录",
                    description = "开启通知访问并选择来源后，收到的新通知会出现在这里。",
                )
            } else {
                EmptyPanel(
                    icon = Icons.Filled.Notifications,
                    title = "该来源暂无记录",
                    description = "切换到“全部”查看其他来源的通知。",
                )
            }
            else -> {
                val today = LocalDate.now()
                val grouped = state.snapshots.groupBy {
                    Instant.ofEpochMilli(it.receivedAt).atZone(ZoneId.systemDefault()).toLocalDate()
                }
                LazyColumn(contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp)) {
                    grouped
                        .toSortedMap(compareByDescending { it })
                        .forEach { (date, items) ->
                            val groupLabel = when (date) {
                                today -> "今天"
                                today.minusDays(1) -> "昨天"
                                else -> date.format(DateTimeFormatter.ofPattern("M月d日"))
                            }
                            item(key = "header-$date") {
                                GroupTitle("$groupLabel · ${date.format(DateTimeFormatter.ofPattern("M月d日"))}", items.size)
                            }
                            items(items, key = { it.snapshotId }) { snapshot ->
                                NotificationRow(snapshot) { onOpenSnapshot(snapshot.snapshotId) }
                            }
                        }
                }
            }
        }
    }
}

@Composable
private fun GroupTitle(label: String, count: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            "$count 条",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** 预览稿 notification 行：无边框、底部细分隔线，来源行 + 标题 + 两行摘要。 */
@Composable
private fun NotificationRow(snapshot: NotificationSnapshotEntity, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SourceBadge(snapshot.packageName)
            Text(
                snapshot.packageName,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                timeFormatter.format(Instant.ofEpochMilli(snapshot.receivedAt)),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            snapshot.title ?: "（无标题）",
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        val body = snapshot.bigText
            ?: snapshot.textLines.firstOrNull { it.isNotBlank() }
            ?: snapshot.text
        if (!body.isNullOrBlank()) {
            Spacer(Modifier.height(4.dp))
            Text(
                body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (snapshot.contentAvailability != "FULL") {
            Spacer(Modifier.height(6.dp))
            Text(
                availabilityLabel[snapshot.contentAvailability] ?: snapshot.contentAvailability,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(10.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}
