package com.ywb.focusguard.ui.component

import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.dp
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.compose.cartesian.data.lineModel
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.marker.rememberDefaultCartesianMarker
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoScrollState
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoZoomState
import com.patrykandpatrick.vico.compose.common.ProvideVicoTheme
import com.patrykandpatrick.vico.compose.common.component.rememberTextComponent
import com.patrykandpatrick.vico.compose.m3.common.rememberM3VicoTheme
import kotlinx.coroutines.runBlocking

/** 带坐标和按压数据标记的报告折线图，模型变化时使用 Vico 的差值动画。 */
@Composable
fun InteractiveLineChart(
    values: List<Float>,
    modifier: Modifier = Modifier
) {
    if (values.size < 2) {
        Box(
            modifier = modifier.fillMaxWidth().height(132.dp),
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.material3.Text(
                text = values.firstOrNull()?.let { "本期评分 ${it.toInt()} 分" } ?: "暂无趋势数据",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    val modelProducer = remember { CartesianChartModelProducer() }
    val inspectionMode = LocalInspectionMode.current
    val bottomFormatter = remember {
        CartesianValueFormatter { _, value, _ ->
            (value.toInt() + 1).toString()
        }
    }

    if (inspectionMode) {
        remember(values) {
            runBlocking {
                modelProducer.runTransaction {
                    lineModel { series(values) }
                }
            }
        }
    } else {
        LaunchedEffect(values) {
            modelProducer.runTransaction {
                lineModel { series(values) }
            }
        }
    }

    ProvideVicoTheme(rememberM3VicoTheme()) {
        val marker = rememberDefaultCartesianMarker(label = rememberTextComponent())
        CartesianChartHost(
            chart = rememberCartesianChart(
                rememberLineCartesianLayer(),
                startAxis = VerticalAxis.rememberStart(
                    valueFormatter = CartesianValueFormatter.decimal(decimalCount = 0)
                ),
                bottomAxis = HorizontalAxis.rememberBottom(
                    valueFormatter = bottomFormatter,
                    guideline = null
                ),
                marker = marker
            ),
            modelProducer = modelProducer,
            modifier = modifier.fillMaxWidth().height(156.dp),
            scrollState = rememberVicoScrollState(scrollEnabled = false),
            zoomState = rememberVicoZoomState(zoomEnabled = false),
            animationSpec = if (inspectionMode) null else tween(durationMillis = 320),
            animateIn = !inspectionMode
        )
    }
}
