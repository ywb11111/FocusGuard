package com.ywb.focusguard.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.compose.cartesian.data.lineModel
import com.ywb.focusguard.data.repository.FocusRepository
import com.ywb.focusguard.domain.model.FocusSession
import com.ywb.focusguard.domain.model.FocusCategory
import com.ywb.focusguard.ui.state.FocusCategorySummary
import com.ywb.focusguard.ui.state.PeriodSummary
import com.ywb.focusguard.ui.state.ReportPeriod
import com.ywb.focusguard.ui.state.ReportsUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import java.util.Calendar
import javax.inject.Inject

/**
 * 报告页 ViewModel：支持周/月切换，从 Room 按日期范围查询并聚合统计。
 *
 * 使用 flatMapLatest 切换周期时自动取消旧查询、启动新查询。
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ReportsViewModel @Inject constructor(
    private val focusRepository: FocusRepository
) : ViewModel() {

    /** 当前选中的报告周期。 */
    private val _period = MutableStateFlow(ReportPeriod.WEEK)

    /**
     * 评分趋势图的数据容器（Vico 的 CartesianChartModelProducer）。
     *
     * **为什么放在 ViewModel 而不是页面里 remember**：
     * 底部 Tab 切走时报告页会离开组合，页面里 remember 的对象随之丢弃；切回来时重新创建一个空容器，
     * 图表要等数据重新灌进去，再从零"长"一遍入场动画——这正是切到报告页时曲线卡顿的来源之一。
     * ViewModel 跟随导航返回栈存活，Tab 切换不会销毁它，容器里的数据一直在，切回来直接画出最终状态。
     */
    val trendModelProducer = CartesianChartModelProducer()

    /** 上一次写入图表的数据，数据没变就不重复提交（避免无意义的差值动画）。 */
    private var lastTrendValues: List<Float>? = null

    /**
     * 本 ViewModel 生命周期内趋势图是否已经播放过入场动画。
     * 入场动画只在第一次看到报告时播放一次；之后切 Tab 回来数据没变，就不该再从零长一遍。
     */
    var trendIntroPlayed = false
        private set

    /** 报告页完整状态。 */
    val uiState: StateFlow<ReportsUiState> = _period.flatMapLatest { period ->
        val (start, end) = periodRange(period)
        focusRepository.observeSessionsInRange(start, end).map { sessions ->
            buildUiState(period, sessions)
        }
    }.onEach { state ->
        updateTrendModel(state.trendValues)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ReportsUiState()
    )

    /** 切换周/月周期。 */
    fun switchPeriod(period: ReportPeriod) {
        _period.value = period
    }

    /** 页面播放完入场动画后调用，之后再进入报告页时图表直接显示最终状态。 */
    fun markTrendIntroPlayed() {
        trendIntroPlayed = true
    }

    /**
     * 把趋势数据提交给图表容器。runTransaction 在后台线程计算图表模型，不占主线程；
     * 少于 2 个点画不成折线，页面会改为文字提示，这里不提交。
     * 数据变化（切换周/月、新完成一次专注）时 Vico 会在旧曲线和新曲线之间做差值动画。
     *
     * 单独 launch 而不是在 onEach 里直接挂起：runTransaction 会等图表处理完才返回，
     * 直接挂起会把列表、数字等页面状态的更新也一起拖住。新数据到来时取消上一次还没完成的提交。
     */
    private fun updateTrendModel(values: List<Float>) {
        if (values.size < 2 || values == lastTrendValues) return
        lastTrendValues = values
        trendJob?.cancel()
        trendJob = viewModelScope.launch {
            trendModelProducer.runTransaction { lineModel { series(values) } }
        }
    }

    /** 正在进行的图表数据提交。 */
    private var trendJob: Job? = null

    /** 根据周期类型计算起止时间戳。 */
    private fun periodRange(period: ReportPeriod): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        val end = cal.apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.timeInMillis + 1

        val start = when (period) {
            ReportPeriod.WEEK -> {
                cal.apply {
                    add(Calendar.DAY_OF_YEAR, -6)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
            }
            ReportPeriod.MONTH -> {
                cal.apply {
                    add(Calendar.DAY_OF_YEAR, -29)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
            }
        }
        return Pair(start, end)
    }

    /** 从会话列表构建完整的报告页状态。 */
    private fun buildUiState(period: ReportPeriod, sessions: List<FocusSession>): ReportsUiState {
        val totalFocus = sessions.sumOf { it.durationMillis }
        val avgScore = sessions.map { it.score }.average().takeIf { !it.isNaN() }?.toInt() ?: 0
        val avgNoise = sessions.map { it.averageNoiseDb }.average().takeIf { !it.isNaN() }?.toFloat() ?: 0f
        val avgLight = sessions.map { it.averageLightLux }.average().takeIf { !it.isNaN() }?.toFloat() ?: 0f
        val totalMovement = sessions.sumOf { it.movementCount }

        return ReportsUiState(
            period = period,
            sessions = sessions,
            periodSummary = PeriodSummary(
                totalFocusMillis = totalFocus,
                averageScore = avgScore,
                sessionCount = sessions.size,
                averageNoiseDb = avgNoise,
                averageLightLux = avgLight,
                totalMovementCount = totalMovement
            ),
            trendValues = sessions.reversed().map { it.score.toFloat() },
            categorySummaries = buildCategorySummaries(sessions),
            isLoaded = true
        )
    }
}

/** 旧记录没有标准标签时统一归入“其他”，避免历史自由文本把报告切得过碎。 */
internal fun buildCategorySummaries(sessions: List<FocusSession>): List<FocusCategorySummary> =
    sessions
        .groupBy { FocusCategory.fromLabel(it.note).label }
        .map { (label, items) ->
            FocusCategorySummary(
                label = label,
                totalFocusMillis = items.sumOf { it.durationMillis },
                sessionCount = items.size,
                averageScore = items.map { it.score }.average().toInt()
            )
        }
        .sortedByDescending { it.totalFocusMillis }
