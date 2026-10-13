package com.ywb.focusguard.ui.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.currentStateAsState
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

// 项目通用的轻量动画组件。统一遵循三条规则：
// 1. 只动画透明度、位移、裁剪这类"绘制阶段"的属性，不改尺寸，避免每帧重新测量布局；
// 2. 只在进入页面或数据变化时播放一次，普通重组不重放；
// 3. 截图预览（LocalInspectionMode）里直接显示最终状态，保证截图回归稳定。

/**
 * 数字从起点滚动到目标值，用于报告页的总时长、平均分等关键数字。
 *
 * @param target 目标数值。
 * @param animateFromZero 首次显示时是否从 0 开始滚动；false 时直接显示目标值，之后目标变化仍会平滑过渡。
 * @param format 数值转展示文本，例如把分钟数格式化为"2h 15m"。
 */
@Composable
fun AnimatedCountText(
    target: Int,
    animateFromZero: Boolean,
    style: TextStyle,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    format: (Int) -> String = { it.toString() }
) {
    val inspectionMode = LocalInspectionMode.current
    val value = remember { Animatable(if (animateFromZero && !inspectionMode) 0f else target.toFloat()) }
    LaunchedEffect(target) {
        if (inspectionMode) {
            value.snapTo(target.toFloat())
        } else {
            value.animateTo(target.toFloat(), tween(durationMillis = 700, easing = FastOutSlowInEasing))
        }
    }
    Text(text = format(value.value.roundToInt()), style = style, color = color, modifier = modifier)
}

/**
 * 列表项错峰入场：第 index 项延迟 index × 40ms 后，从下方 16dp 淡入上移到原位。
 *
 * 动画值只在 graphicsLayer 的 lambda 里读取：Compose 会把读取记录在"绘制阶段"，
 * 每一帧只重绘这一层，不触发重组和重新布局，几十行同时动画也很轻。
 *
 * @param index 列表下标，超过 8 的按 8 计算，避免长列表最后一项等太久。
 * @param enabled false 时直接显示（例如切 Tab 回来，数据没变就不该再播一遍）。
 */
@Composable
fun Modifier.staggeredEnter(index: Int, enabled: Boolean): Modifier {
    val inspectionMode = LocalInspectionMode.current
    val progress = remember { Animatable(if (enabled && !inspectionMode) 0f else 1f) }
    LaunchedEffect(Unit) {
        if (progress.value < 1f) {
            delay(index.coerceAtMost(8) * 40L)
            progress.animateTo(1f, tween(durationMillis = 320, easing = FastOutSlowInEasing))
        }
    }
    return this.graphicsLayer {
        alpha = progress.value
        translationY = (1f - progress.value) * 16.dp.toPx()
    }
}

/**
 * 等页面转场结束后再显示重量级内容（图表），转场期间先放一个同样高度的占位。
 *
 * **为什么有效**：Navigation Compose 在转场动画进行时，把目标页的生命周期限制在 STARTED，
 * 转场结束才进入 RESUMED。转场那几帧里主线程既要跑淡入淡出，又要第一次组合整页内容，
 * 再加上图表的测量和入场动画，很容易超出 16ms 一帧的预算——这就是"线条上来时卡一下"。
 * 把图表推迟到 RESUMED 再组合，转场和图表动画错开执行，各自都能流畅播放。
 * 占位高度与图表一致，图表出现时不会把下面的内容顶下去。
 *
 * @param enabled false 时立即显示内容（例如数据已就绪、无需入场动画的场景）。
 * @param placeholderHeight 占位高度，应与内容高度一致。
 */
@Composable
fun DeferUntilTransitionEnd(
    enabled: Boolean,
    placeholderHeight: Dp,
    content: @Composable () -> Unit
) {
    val inspectionMode = LocalInspectionMode.current
    val lifecycleState by LocalLifecycleOwner.current.lifecycle.currentStateAsState()
    // 一旦显示就不再退回占位：页面短暂进入 PAUSED（例如下拉通知栏）时不能把图表卸掉重来
    var shown by remember { mutableStateOf(!enabled || inspectionMode) }
    LaunchedEffect(lifecycleState) {
        if (lifecycleState.isAtLeast(Lifecycle.State.RESUMED)) shown = true
    }
    if (shown) {
        content()
    } else {
        Spacer(Modifier.fillMaxWidth().height(placeholderHeight))
    }
}
