package com.todoink.app.ui.todo

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.todoink.app.ui.components.EmptyPanel
import com.todoink.app.ui.components.PageHeader
import com.todoink.app.ui.components.SegmentControl
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val dateFormatter = DateTimeFormatter.ofPattern("M月d日 · EEEE", Locale.CHINESE)

/**
 * 预览稿 P01 待办首页。候选 / 正式任务数据流属 Phase 2（TD-001 / TD-005 / TD-006），
 * 当前只呈现真实状态：还没有任何正式待办，显示设计稿定义的空态；不伪造任务数据。
 */
@Composable
fun TodoHomeScreen() {
    var tab by rememberSaveable { mutableStateOf("today") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
    ) {
        PageHeader(
            title = "待办",
            subtitle = dateFormatter.format(LocalDate.now()),
            sealIcon = Icons.Filled.WbSunny,
        )
        Spacer(Modifier.height(12.dp))
        SegmentControl(
            options = listOf("today" to "今天", "all" to "全部", "completed" to "已完成"),
            selected = tab,
            onSelect = { tab = it },
        )
        Spacer(Modifier.height(20.dp))
        when (tab) {
            "today" -> EmptyPanel(
                icon = Icons.Filled.WbSunny,
                title = "今天没有到期任务",
                description = "其他日期的任务，可以在“全部”中查看。",
                actionLabel = "查看全部",
                onAction = { tab = "all" },
            )
            "completed" -> EmptyPanel(
                icon = Icons.Filled.Checklist,
                title = "还没有已完成任务",
                description = "确认后的事情会留在这里。",
            )
            else -> EmptyPanel(
                icon = Icons.Filled.Inbox,
                title = "清单暂时是空的",
                description = "通知里的行动项经你确认后，才会加入待办。",
            )
        }
        Spacer(Modifier.height(24.dp))
        Text(
            text = "通知里的建议会先进入“待确认”，由你决定是否加入清单。",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(24.dp))
    }
}
