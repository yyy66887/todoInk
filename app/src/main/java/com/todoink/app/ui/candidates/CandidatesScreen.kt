package com.todoink.app.ui.candidates

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.todoink.app.ui.components.EmptyPanel
import com.todoink.app.ui.components.PageHeader

/**
 * 预览稿 P02 待确认收件箱。候选数据与规则引擎属 Phase 2（TD-003 / TD-005 / TD-006），
 * 当前显示真实空态；识别开关随候选处理服务（TD-005）一并提供。
 */
@Composable
fun CandidatesScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
    ) {
        PageHeader(
            title = "待确认",
            subtitle = "0 条通知建议",
            sealIcon = Icons.Filled.Inbox,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "来自通知的建议，由你决定是否加入清单。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        EmptyPanel(
            icon = Icons.Filled.Inbox,
            title = "暂时没有待确认",
            description = "新通知产生建议后，会出现在这里。",
        )
    }
}
