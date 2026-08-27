package com.ywb.focusguard.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ywb.focusguard.ui.theme.MintAccent
import com.ywb.focusguard.ui.theme.MintPrimary
import com.ywb.focusguard.ui.theme.MintPrimaryDark
import com.ywb.focusguard.ui.theme.MintWarning

/**
 * 统一展示"指标名称 + 主要数值 + 可选说明"的紧凑卡片。
 *
 * @param label 指标名称。
 * @param value 最醒目的指标值。
 * @param modifier 外部布局修饰符。
 * @param supportingText 可选的补充说明。
 */
@Composable
fun MetricCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    supportingText: String? = null
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
            if (supportingText != null) {
                Text(
                    text = supportingText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/** 将多个名称/数值对平均排列为一行指标卡。 */
@Composable
fun MetricRow(
    metrics: List<Pair<String, String>>,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        metrics.forEach { (label, value) ->
            MetricCard(
                label = label,
                value = value,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * 带状态指示的指标卡片 - 原型设计风格
 * 显示图标、标签、数值和状态指示
 */
@Composable
fun MetricCardWithStatus(
    icon: String,
    label: String,
    value: String,
    status: String,
    modifier: Modifier = Modifier
) {
    val statusColor = when (status) {
        "good" -> MintPrimaryDark
        "normal" -> MintAccent
        "warning" -> MintWarning
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    val statusText = when (status) {
        "good" -> "✓ 适中"
        "normal" -> "○ 一般"
        "warning" -> "⚠ 偏高"
        else -> ""
    }

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = icon,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = statusText,
                style = MaterialTheme.typography.labelSmall,
                color = statusColor,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
