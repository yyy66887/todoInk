package com.todoink.app.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.todoink.app.ui.components.DefinitionRow
import com.todoink.app.ui.components.PageHeader
import com.todoink.app.ui.components.SubBar

/** 预览稿 about 页：本地优先说明，不含账号 / 云服务占位。 */
@Composable
fun AboutScreen(onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
    ) {
        SubBar(title = "关于 TodoInk", onBack = onBack)
        PageHeader(
            title = "TodoInk",
            subtitle = "从通知，到真正要做的事情",
            sealIcon = Icons.Filled.Checklist,
        )
        DefinitionRow(label = "定位", value = "本地通知转待办")
        DefinitionRow(label = "数据", value = "全部在本机处理")
        DefinitionRow(label = "设计版本", value = "v0.1 · 待审核")
        Spacer(Modifier.height(16.dp))
        Text(
            text = "只有你确认过的行动项才会成为待办；本阶段没有账号、云服务或设备连接。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(24.dp))
    }
}
