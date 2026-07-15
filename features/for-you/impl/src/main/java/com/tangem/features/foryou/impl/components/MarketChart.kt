package com.tangem.features.foryou.impl.components

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tangem.core.ui.components.SpacerH8
import com.tangem.core.ui.components.haze.hazeSourceTangem
import com.tangem.core.ui.ds2.surface.TangemSurface
import com.tangem.core.ui.extensions.*
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.core.ui.format.bigdecimal.percent
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.features.foryou.impl.R
import com.tangem.features.foryou.impl.components.state.*
import com.tangem.features.foryou.impl.ui.components.AiInsightContent
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.math.BigDecimal

@Composable
internal fun MarketChart(marketChart: MarketChartUM, modifier: Modifier = Modifier) {
    var cardBoundsInWindow by remember { mutableStateOf(Rect.Zero) }

    TangemSurface(
        modifier = modifier
            .hazeSourceTangem()
            .onGloballyPositioned { cardBoundsInWindow = it.boundsInWindow() },
        color = TangemTheme.colors3.bg.secondary,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            DonutChartBlock(marketChart.donutChart, cardBoundsInWindow)
            Spacer(modifier = Modifier.height(16.dp))
            when (marketChart) {
                is MarketChartUM.Loaded -> {
                    TopHoldingBlock(
                        assetCount = marketChart.assetCount,
                        topHoldingPercent = marketChart.topHoldingPercent,
                    )
                }
                is MarketChartUM.NoData -> {
                    CantLoadDataBlock(text = marketChart.title)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (marketChart.aiInsight is AiInsightUM.Displayed) SpacerH8()

            AiInsightContent(
                aiInsightUM = marketChart.aiInsight,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
            )
        }
    }
}

@Suppress("LongMethod")
@Composable
private fun ColumnScope.DonutChartBlock(donutChartUM: DonutChartUM, cardBoundsInWindow: Rect) {
    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    val segments = donutChartUM.donutSegmentList
    val scope = rememberCoroutineScope()
    var dismissJob by remember { mutableStateOf<Job?>(null) }
    var chartSize by remember { mutableStateOf(IntSize.Zero) }
    var chartWindowOffset by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = Modifier
            .padding(32.dp)
            .align(Alignment.CenterHorizontally)
            .size(200.dp),
        contentAlignment = Alignment.Center,
    ) {
        DonutChart(
            modifier = Modifier
                .fillMaxSize()
                .onGloballyPositioned { coordinates ->
                    chartSize = coordinates.size
                    chartWindowOffset = coordinates.localToWindow(Offset.Zero)
                }
                // A press on the chart means this tap is "on the chart", not "outside" — veto the pending
                // outside-dismiss before it commits.
                .pointerInput(Unit) {
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false)
                        dismissJob?.cancel()
                    }
                },
            selectedIndex = selectedIndex,
            strokeWidth = DonutStrokeWidth,
            startAngle = DonutStartAngle,
            // Tap a slice → select; tap it again or miss (null) → deselect; tap another slice → switch.
            onSegmentClick = { index ->
                selectedIndex = index?.takeIf { it != selectedIndex }
            },
            segments = segments,
        ) {
            when (donutChartUM) {
                is DonutChartUM.Loaded -> {
                    Text(
                        text = donutChartUM.totalAmount,
                        color = TangemTheme.colors3.text.primary,
                        style = TangemTheme.typography3.body.medium,
                        maxLines = 1,
                        autoSize = TextAutoSize.StepBased(
                            minFontSize = 8.sp,
                            maxFontSize = TangemTheme.typography3.body.medium.fontSize,
                        ),
                    )
                    Text(
                        text = stringResourceSafe(R.string.market_chart_bubble_total_value),
                        color = TangemTheme.colors3.text.secondary,
                        style = TangemTheme.typography3.caption.medium,
                        maxLines = 1,
                        autoSize = TextAutoSize.StepBased(
                            maxFontSize = TangemTheme.typography3.caption.medium.fontSize,
                        ),
                    )
                }
                is DonutChartUM.NoData -> {
                    Text(
                        text = donutChartUM.title.resolveReference(),
                        color = TangemTheme.colors3.text.secondary,
                        style = TangemTheme.typography3.body.medium,
                        maxLines = 2,
                        textAlign = TextAlign.Center,
                        autoSize = TextAutoSize.StepBased(
                            maxFontSize = TangemTheme.typography3.caption.medium.fontSize,
                        ),
                    )
                }
            }
        }

        DonutSegmentTooltipBlock(
            selectedIndex = selectedIndex,
            segments = segments,
            chartSize = chartSize,
            chartWindowOffset = chartWindowOffset,
            cardBoundsInWindow = cardBoundsInWindow,
            onDismissRequest = {
                dismissJob?.cancel()
                dismissJob = scope.launch {
                    withFrameNanos { }
                    selectedIndex = null
                }
            },
        )
    }
}

@Suppress("LongParameterList")
@Composable
private fun DonutSegmentTooltipBlock(
    selectedIndex: Int?,
    segments: List<DonutSegmentUM>,
    chartSize: IntSize,
    chartWindowOffset: Offset,
    cardBoundsInWindow: Rect,
    onDismissRequest: () -> Unit,
) {
    val density = LocalDensity.current
    val gapPx = with(density) { 8.dp.roundToPx() }
    val strokePx = with(density) { DonutStrokeWidth.toPx() }
    var shownIndex by remember { mutableStateOf<Int?>(null) }
    if (selectedIndex != null) shownIndex = selectedIndex

    val isExpanded = selectedIndex?.let(segments::getOrNull) != null
    val shownSegment = shownIndex?.let(segments::getOrNull) ?: return
    val positionProvider = remember(
        shownIndex,
        segments,
        chartSize,
        chartWindowOffset,
        cardBoundsInWindow,
        strokePx,
        gapPx,
    ) {
        segmentTooltipPositionProvider(
            selectedIndex = shownIndex,
            segments = segments,
            chartSize = chartSize,
            chartWindowOffset = chartWindowOffset,
            strokePx = strokePx,
            startAngle = DonutStartAngle,
            cardBoundsInWindow = cardBoundsInWindow,
            gapPx = gapPx,
        )
    }

    DonutSegmentTooltip(
        expanded = isExpanded,
        positionProvider = positionProvider,
        title = shownSegment.title,
        fiatValue = shownSegment.fiatValue,
        percent = shownSegment.weight.format { percent() },
        onDismissRequest = onDismissRequest,
    )
}

private val DonutStrokeWidth = 28.dp
private val DonutStartAngle = -90f

@Composable
private fun ColumnScope.TopHoldingBlock(assetCount: Int, topHoldingPercent: TextReference) {
    Text(
        modifier = Modifier.padding(horizontal = 16.dp),
        text = pluralStringResourceSafe(R.plurals.market_chart_assets_android, assetCount, assetCount),
        color = TangemTheme.colors3.text.secondary,
        style = TangemTheme.typography3.heading.small,
    )

    Text(
        modifier = Modifier.padding(horizontal = 16.dp),
        text = topHoldingPercent.resolveReference(),
        color = TangemTheme.colors3.text.primary,
        style = TangemTheme.typography3.heading.small,
    )
}

@Composable
private fun ColumnScope.CantLoadDataBlock(text: TextReference) {
    Text(
        modifier = Modifier.padding(horizontal = 16.dp),
        text = text.resolveReference(),
        color = TangemTheme.colors3.text.secondary,
        style = TangemTheme.typography3.heading.small,
    )
}

// region Previews

private enum class MarketChartPreviewScenario { DISPLAYED, ASK_AI, NO_AI, NO_DATA }

private class MarketChartPreviewProvider : PreviewParameterProvider<MarketChartPreviewScenario> {
    override val values: Sequence<MarketChartPreviewScenario>
        get() = MarketChartPreviewScenario.entries.asSequence()
}

@Preview(name = "MarketChart • Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "MarketChart • Light", showBackground = true)
@Composable
private fun MarketChart_Preview(
    @PreviewParameter(MarketChartPreviewProvider::class) scenario: MarketChartPreviewScenario,
) {
    TangemThemePreviewRedesign {
        Box(
            modifier = Modifier
                .background(TangemTheme.colors3.bg.primary)
                .padding(16.dp),
        ) {
            MarketChart(marketChart = previewMarketChartState(scenario))
        }
    }
}

/** Maps a [scenario] to the state shown in the preview. */
@Suppress("MagicNumber")
@Composable
private fun previewMarketChartState(scenario: MarketChartPreviewScenario): MarketChartUM = when (scenario) {
    MarketChartPreviewScenario.DISPLAYED -> MarketChartUM.Loaded(
        topHoldingPercent = stringReference("Top holding 41%"),
        aiInsight = AiInsightUM.Displayed(
            "Your portfolio leans on a single asset – BTC is 42% of holdings. Stablecoins add 23% " +
                "buffer. Consider trimming concentration for a smoother ride",
        ),
        donutChart = previewLoadedDonut(),
    )
    MarketChartPreviewScenario.ASK_AI -> MarketChartUM.Loaded(
        topHoldingPercent = stringReference("Top holding 41%"),
        aiInsight = AiInsightUM.AskAiInsight(askAiInsightClick = {}),
        donutChart = previewLoadedDonut(),
    )
    MarketChartPreviewScenario.NO_AI -> MarketChartUM.Loaded(
        topHoldingPercent = stringReference("Top holding 41%"),
        aiInsight = AiInsightUM.Hide,
        donutChart = DonutChartUM.Loaded(
            totalAmount = "$10,12345678912.1333",
            donutSegmentList = persistentListOf(
                DonutSegmentUM(
                    weight = BigDecimal(0.55),
                    color = DonutSegmentColor.Brand,
                    title = stringReference("Ethereum"),
                    fiatValue = stringReference("$5,720.22"),
                ),
                DonutSegmentUM(
                    weight = BigDecimal(0.45),
                    color = DonutSegmentColor.Green,
                    title = stringReference("Solana"),
                    fiatValue = stringReference("$728.30"),
                ),
            ),
        ),
    )
    MarketChartPreviewScenario.NO_DATA -> MarketChartUM.NoData(
        title = resourceReference(R.string.market_chart_can_not_load_data),
        donutText = resourceReference(R.string.market_chart_bubble_no_data),
    )
}

@Suppress("MagicNumber")
@Composable
private fun previewLoadedDonut(): DonutChartUM.Loaded = DonutChartUM.Loaded(
    totalAmount = "$10,123456.1333",
    donutSegmentList = persistentListOf(
        DonutSegmentUM(
            weight = BigDecimal(0.90),
            color = DonutSegmentColor.Brand,
            title = stringReference("Ethereum"),
            fiatValue = stringReference("$5,720.22"),
        ),
        DonutSegmentUM(
            weight = BigDecimal(0.03),
            color = DonutSegmentColor.Violet,
            title = stringReference("Solana"),
            fiatValue = stringReference("$728.30"),
        ),
        DonutSegmentUM(
            weight = BigDecimal(0.03),
            color = DonutSegmentColor.Red,
            title = stringReference("Polkadot"),
            fiatValue = stringReference("$624.26"),
        ),
        DonutSegmentUM(
            weight = BigDecimal(0.02),
            color = DonutSegmentColor.Green,
            title = stringReference("Tether"),
            fiatValue = stringReference("$520.18"),
        ),
    ),
)

// endregion