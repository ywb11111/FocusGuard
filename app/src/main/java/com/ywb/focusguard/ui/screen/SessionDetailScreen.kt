package com.ywb.focusguard.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ywb.focusguard.ui.component.MetricCard
import com.ywb.focusguard.ui.component.SectionHeader
import com.ywb.focusguard.ui.component.SimpleLineChart

@Composable
fun SessionDetailScreen(
    sessionId: Long
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "专注详情 #$sessionId",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            MetricCard("时长", "45m", Modifier.weight(1f))
            MetricCard("评分", "88", Modifier.weight(1f))
        }
        SectionHeader(title = "评分拆解")
        Text("噪声 -8 · 光照 -0 · 移动 -3 · 分心 -5")
        SectionHeader(title = "噪声曲线")
        SimpleLineChart(values = listOf(42f, 40f, 45f, 58f, 52f, 43f, 41f))
        SectionHeader(title = "光照曲线")
        SimpleLineChart(values = listOf(160f, 172f, 180f, 188f, 176f, 184f, 190f))
        SectionHeader(title = "建议")
        Text(
            text = "整体环境稳定。后续接入真实采样后，这里会显示由规则评分生成的可解释建议。",
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
