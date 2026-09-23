package com.ywb.focusguard.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ywb.focusguard.ui.component.FocusPageHeader
import com.ywb.focusguard.ui.component.FocusStat
import com.ywb.focusguard.ui.component.SectionHeader
import com.ywb.focusguard.ui.component.SimpleLineChart
import com.ywb.focusguard.ui.state.SessionDetailUiState
import com.ywb.focusguard.ui.theme.FocusBlueSoft
import com.ywb.focusguard.ui.theme.FocusTealSoft
import com.ywb.focusguard.ui.viewmodel.SessionDetailViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** 详情页路由：读取带 sessionId 的 ViewModel，并保留返回操作。 */
@Composable
fun SessionDetailRoute(
    onBack: () -> Unit = {},
    viewModel: SessionDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    SessionDetailScreen(uiState, onBack)
}

/** 详情页状态分发器，确保 Loading、Empty、Content 三种状态互斥。 */
@Composable
fun SessionDetailScreen(uiState: SessionDetailUiState, onBack: () -> Unit = {}) {
    when (uiState) {
        SessionDetailUiState.Loading -> SessionDetailMessage("正在读取专注详情", onBack)
        is SessionDetailUiState.Empty -> SessionDetailMessage(uiState.message, onBack)
        is SessionDetailUiState.Content -> SessionDetailContent(uiState, onBack)
    }
}

/** 把结论、真实环境曲线和改进建议组织成一份可解释记录。 */
@Composable
private fun SessionDetailContent(uiState: SessionDetailUiState.Content, onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp)
    ) {
        FocusPageHeader(
            title = "专注详情",
            subtitle = detailDateText(uiState.startedAt),
            leading = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "返回")
                }
            }
        )

        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("综合评分", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(uiState.scoreText, style = MaterialTheme.typography.displayMedium)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("有效专注", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(uiState.durationText, style = MaterialTheme.typography.headlineSmall)
                }
            }
        }

        Row(modifier = Modifier.fillMaxWidth()) {
            FocusStat(uiState.averageNoiseText, "平均噪声", Modifier.weight(1f), icon = Icons.Outlined.GraphicEq)
            FocusStat(uiState.averageLightText, "平均光照", Modifier.weight(1f), icon = Icons.Outlined.LightMode, tint = MaterialTheme.colorScheme.secondary)
            FocusStat("${uiState.motionCount} 次", "手机移动", Modifier.weight(1f), icon = Icons.Outlined.PhoneAndroid)
        }

        DetailChart("噪声变化", "相对 dB，仅保存分析结果", uiState.noiseValues, Icons.Outlined.GraphicEq)
        DetailChart("光照变化", "lux，记录环境亮度变化", uiState.lightValues, Icons.Outlined.LightMode)

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SectionHeader(title = "评分拆解")
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Text(
                    uiState.scoreBreakdownText,
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Row(modifier = Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.Top) {
                Surface(shape = CircleShape, color = FocusTealSoft, modifier = Modifier.size(44.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.Analytics, null, tint = MaterialTheme.colorScheme.primary)
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("下一次建议", style = MaterialTheme.typography.titleMedium)
                    Text(uiState.suggestionText, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        Spacer(Modifier.size(4.dp))
    }
}

@Composable
private fun DetailChart(
    title: String,
    supporting: String,
    values: List<Float>,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionHeader(title = title)
        Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = CircleShape, color = FocusBlueSoft, modifier = Modifier.size(38.dp)) {
                        Box(contentAlignment = Alignment.Center) { Icon(icon, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(19.dp)) }
                    }
                    Spacer(Modifier.width(10.dp))
                    Text(supporting, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (values.size >= 2) {
                    SimpleLineChart(values)
                } else {
                    Text("本次采样不足，暂时无法形成曲线。", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun SessionDetailMessage(message: String, onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        FocusPageHeader(
            title = "专注详情",
            leading = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "返回")
                }
            }
        )
        Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun detailDateText(timestamp: Long): String = if (timestamp <= 0L) {
    "已完成的专注记录"
} else {
    SimpleDateFormat("yyyy年M月d日 HH:mm", Locale.getDefault()).format(Date(timestamp))
}
