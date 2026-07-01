package com.tangem.features.foryou.impl.components

import android.content.res.Configuration
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tangem.core.ui.components.haze.hazeSourceTangem
import com.tangem.core.ui.ds.button.SecondaryTangemButton
import com.tangem.core.ui.ds.button.TangemButtonSize
import com.tangem.core.ui.ds2.surface.TangemSurface
import com.tangem.core.ui.extensions.pluralStringResourceSafe
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.LocalHazeState
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.features.foryou.impl.components.state.AiInsightState
import com.tangem.features.foryou.impl.components.state.DonutChartState
import com.tangem.features.foryou.impl.components.state.DonutSegment
import com.tangem.features.foryou.impl.components.state.MarketChartState
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.Int
import com.tangem.features.foryou.impl.R

@Composable
internal fun MarketChart(marketChartState: MarketChartState, modifier: Modifier = Modifier) {
    val hazeState = LocalHazeState.current
    var cardBoundsInWindow by remember { mutableStateOf(Rect.Zero) }

    TangemSurface(
        modifier = modifier
            .hazeSourceTangem(hazeState)
            .onGloballyPositioned { cardBoundsInWindow = it.boundsInWindow() },
        color = TangemTheme.colors3.bg.secondary,
    ) {
        Column {
            DonutChartBlock(marketChartState.donutChartState, cardBoundsInWindow)
            Spacer(modifier = Modifier.height(16.dp))
            if (marketChartState is MarketChartState.Loaded) {
                TopHoldingBlock(
                    assetCount = marketChartState.assetCount,
                    topHoldingPercent = marketChartState.topHoldingPercent,
                )
            } else {
                CantLoadDataBlock()
            }

            Spacer(modifier = Modifier.height(16.dp))
            AiInsightContent(marketChartState.aiInsightState)
        }
    }
}

@Suppress("LongMethod")
@Composable
private fun ColumnScope.DonutChartBlock(donutChartState: DonutChartState, cardBoundsInWindow: Rect) {
    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    val segments = donutChartState.donutSegmentList
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
            if (donutChartState is DonutChartState.Loaded) {
                Text(
                    text = donutChartState.totalAmount,
                    color = TangemTheme.colors3.text.primary,
                    style = TangemTheme.typography3.body.medium,
                    maxLines = 1,
                    autoSize = TextAutoSize.StepBased(
                        minFontSize = 8.sp,
                        maxFontSize = TangemTheme.typography3.body.medium.fontSize,
                    ),
                )
                Text(
                    text = stringResourceSafe(R.string.market_chart_buble_total_value),
                    color = TangemTheme.colors3.text.secondary,
                    style = TangemTheme.typography3.caption.medium,
                    maxLines = 1,
                    autoSize = TextAutoSize.StepBased(
                        maxFontSize = TangemTheme.typography3.caption.medium.fontSize,
                    ),
                )
            } else {
                Text(
                    text = stringResourceSafe(R.string.market_chart_buble_no_data),
                    color = TangemTheme.colors3.text.secondary,
                    style = TangemTheme.typography3.body.medium,
                    maxLines = 1,
                    autoSize = TextAutoSize.StepBased(
                        maxFontSize = TangemTheme.typography3.caption.medium.fontSize,
                    ),
                )
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
    segments: List<DonutSegment>,
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
    val shownSegment = shownIndex?.let(segments::getOrNull)
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
        title = shownSegment?.title.orEmpty(),
        fiatValue = shownSegment?.fiatValue.orEmpty(),
        percent = shownSegment?.let { formatSegmentPercent(it.weight) }.orEmpty(),
        onDismissRequest = onDismissRequest,
    )
}

@Suppress("MagicNumber")
private fun formatSegmentPercent(weight: Float): String {
    val percent = weight.coerceIn(0f, 1f) * 100
    return if (percent % 1f == 0f) "${percent.toInt()}" else "%.2f%".format(percent)
}

private val DonutStrokeWidth = 28.dp
private val DonutStartAngle = -90f

@Composable
private fun ColumnScope.TopHoldingBlock(assetCount: Int, topHoldingPercent: Float) {
    Text(
        modifier = Modifier.padding(horizontal = 16.dp),
        text = pluralStringResourceSafe(R.plurals.market_chart_assets_android, assetCount, assetCount),
        color = TangemTheme.colors3.text.secondary,
        style = TangemTheme.typography3.heading.small,
    )

    Text(
        modifier = Modifier.padding(horizontal = 16.dp),
        text = stringResourceSafe(R.string.market_chart_top_holding, formatSegmentPercent(topHoldingPercent)),
        color = TangemTheme.colors3.text.primary,
        style = TangemTheme.typography3.heading.small,
    )
}

@Composable
private fun ColumnScope.CantLoadDataBlock() {
    Text(
        modifier = Modifier.padding(horizontal = 16.dp),
        text = stringResourceSafe(R.string.market_chart_can_not_load_data),
        color = TangemTheme.colors3.text.secondary,
        style = TangemTheme.typography3.heading.small,
    )
}

// IntrinsicSize.Min lets the gradient divider match the AI text height — heightIn wouldn't achieve that.
@Suppress("ModifierHeightWithText")
@Composable
private fun AiInsightContent(aiInsightState: AiInsightState) {
    AnimatedContent(
        targetState = aiInsightState,
        transitionSpec = { fadeIn().togetherWith(fadeOut()) },
    ) { currentState ->
        when (currentState) {
            is AiInsightState.AskAiInsight -> {
                SecondaryTangemButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                    onClick = currentState.askAiInsightClick,
                    size = TangemButtonSize.X9,
                    text = stringReference("Ask for AI summary"),
                )
            }
            is AiInsightState.Displayed -> {
                Row(
                    modifier = Modifier
                        .height(IntrinsicSize.Min)
                        .padding(top = 8.dp, start = 16.dp, end = 16.dp, bottom = 16.dp),
                ) {
                    CanvasGradientDivider(
                        modifier = Modifier
                            .fillMaxHeight()
                            .padding(vertical = 2.dp),
                    )
                    Text(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 12.dp),
                        text = buildAnnotatedString {
                            withStyle(
                                SpanStyle(
                                    brush = Brush.horizontalGradient(
                                        listOf(
                                            TangemTheme.colors3.icon.accent.violet,
                                            TangemTheme.colors3.icon.accent.blue,
                                        ),
                                    ),
                                    alpha = 1f,
                                ),
                            ) { append(stringResourceSafe(R.string.market_chart_ai_total)) }
                            append(" ")
                            append(currentState.text)
                        },
                        color = TangemTheme.colors3.text.secondary,
                        style = TangemTheme.typography3.caption.medium,
                    )
                }
            }
            AiInsightState.Hide -> {}
        }
    }
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
            MarketChart(marketChartState = previewMarketChartState(scenario))
        }
    }
}

/**
 * Maps a [scenario] to the state shown. Built inside a `@Composable` (not the [PreviewParameterProvider])
 * because the segment colors come from [TangemTheme.colors3], which can only be read in composition.
 */
@Suppress("MagicNumber")
@Composable
private fun previewMarketChartState(scenario: MarketChartPreviewScenario): MarketChartState = when (scenario) {
    MarketChartPreviewScenario.DISPLAYED -> MarketChartState.Loaded(
        topHoldingPercent = 0.41f,
        aiInsightState = AiInsightState.Displayed(
            "Your portfolio leans on a single asset – BTC is 42% of holdings. Stablecoins add 23% " +
                "buffer. Consider trimmng concentration for a smoother ride",
        ),
        donutChartState = previewLoadedDonut(),
    )
    MarketChartPreviewScenario.ASK_AI -> MarketChartState.Loaded(
        topHoldingPercent = 0.41f,
        aiInsightState = AiInsightState.AskAiInsight(askAiInsightClick = {}),
        donutChartState = previewLoadedDonut(),
    )
    MarketChartPreviewScenario.NO_AI -> MarketChartState.Loaded(
        topHoldingPercent = 0.41f,
        aiInsightState = AiInsightState.Hide,
        donutChartState = DonutChartState.Loaded(
            totalAmount = "$10,12345678912.1333",
            donutSegmentList = listOf(
                DonutSegment(weight = 0.55f, color = TangemTheme.colors3.border.brand),
                DonutSegment(weight = 0.45f, color = TangemTheme.colors3.border.accent.green),
            ),
        ),
    )
    MarketChartPreviewScenario.NO_DATA -> MarketChartState.NoData
}

@Suppress("MagicNumber")
@Composable
private fun previewLoadedDonut(): DonutChartState.Loaded = DonutChartState.Loaded(
    totalAmount = "$10,123456.1333",
    donutSegmentList = listOf(
        DonutSegment(
            weight = 0.55f,
            color = TangemTheme.colors3.border.brand,
            title = "Ethereum",
            fiatValue = "$5,720.22",
        ),
        DonutSegment(
            weight = 0.07f,
            color = TangemTheme.colors3.border.accent.violet,
            title = "Solana",
            fiatValue = "$728.30",
        ),
        DonutSegment(
            weight = 0.06f,
            color = TangemTheme.colors3.border.accent.red,
            title = "Polkadot",
            fiatValue = "$624.26",
        ),
        DonutSegment(
            weight = 0.05f,
            color = TangemTheme.colors3.border.accent.green,
            title = "Tether",
            fiatValue = "$520.18",
        ),
    ),
)

// endregion