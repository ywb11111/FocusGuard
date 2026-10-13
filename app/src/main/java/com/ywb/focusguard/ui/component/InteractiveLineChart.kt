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

/** 图表高度；DeferUntilTransitionEnd 的占位高度也用它，保证图表出现时布局不跳动。 */
val TREND_CHART_HEIGHT = 156.dp

/** 带坐标和按压数据标记的报告折线图，模型变化时使用 Vico 的差值动画。 */
@Composable
fun InteractiveLineChart(
    values: List<Float>,
    modifier: Modifier = Modifier
) {
    if (values.size < 2) {
        TrendChartFallback(values, modifier)
        return
    }

    val inspectionMode = LocalInspectionMode.current
    val modelProducer = if (inspectionMode) {
        remember(values) {
            CartesianChartModelProducer().also { producer ->
                runBlocking {
                    producer.runTransaction {
                        lineModel { series(values) }
                    }
                }
            }
        }
    } else {
        remember { CartesianChartModelProducer() }
    }

    if (!inspectionMode) {
        LaunchedEffect(values) {
            modelProducer.runTransaction {
                lineModel { series(values) }
            }
        }
    }

    TrendChartHost(modelProducer = modelProducer, animateIn = !inspectionMode, modifier = modifier)
}

/**
 * 使用外部（ViewModel）持有的数据容器的版本，报告页使用。
 *
 * 数据由 ViewModel 提前写入容器，页面重新进入时图表能直接画出最终状态；
 * [animateIn] 只在第一次看到报告时为 true，之后切 Tab 回来不再从零重放入场动画。
 *
 * @param values 仅用于少于 2 个点时显示文字提示；真正的图表数据在 [modelProducer] 里。
 */
@Composable
fun InteractiveLineChart(
    values: List<Float>,
    modelProducer: CartesianChartModelProducer,
    animateIn: Boolean,
    modifier: Modifier = Modifier
) {
    if (values.size < 2) {
        TrendChartFallback(values, modifier)
        return
    }
    TrendChartHost(modelProducer = modelProducer, animateIn = animateIn, modifier = modifier)
}

/** 数据不足以画折线时的文字提示。 */
@Composable
private fun TrendChartFallback(values: List<Float>, modifier: Modifier) {
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
}

/** 两个版本共用的 Vico 图表主体。 */
@Composable
private fun TrendChartHost(
    modelProducer: CartesianChartModelProducer,
    animateIn: Boolean,
    modifier: Modifier
) {
    val inspectionMode = LocalInspectionMode.current
    val bottomFormatter = remember {
        CartesianValueFormatter { _, value, _ ->
            (value.toInt() + 1).toString()
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
            modifier = modifier.fillMaxWidth().height(TREND_CHART_HEIGHT),
            scrollState = rememberVicoScrollState(scrollEnabled = false),
            zoomState = rememberVicoZoomState(zoomEnabled = false),
            animationSpec = if (inspectionMode) null else tween(durationMillis = 320),
            animateIn = animateIn && !inspectionMode
        )
    }
}
