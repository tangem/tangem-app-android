package com.tangem.core.ui.ds2.for_you_temp

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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.PopupPositionProvider
import com.tangem.core.ui.components.haze.hazeSourceTangem
import com.tangem.core.ui.ds.button.SecondaryTangemButton
import com.tangem.core.ui.ds.button.TangemButtonSize
import com.tangem.core.ui.ds2.for_you_temp.models.AiInsightState
import com.tangem.core.ui.ds2.for_you_temp.models.DonutChartState
import com.tangem.core.ui.ds2.for_you_temp.models.DonutSegment
import com.tangem.core.ui.ds2.for_you_temp.models.MarketChartState
import com.tangem.core.ui.ds2.surface.TangemSurface
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.LocalHazeState
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.Int
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

@Composable
fun MarketChart(marketChartState: MarketChartState, modifier: Modifier = Modifier) {
    val hazeState = LocalHazeState.current
    // Card bounds in window px — gates the tooltip's "flip to the side" fallback (see DonutChartBlock).
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
                .onGloballyPositioned {
                    chartSize = it.size
                    chartWindowOffset = it.localToWindow(Offset.Zero)
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
                    text = "Total value",
                    color = TangemTheme.colors3.text.secondary,
                    style = TangemTheme.typography3.caption.medium,
                    maxLines = 1,
                    autoSize = TextAutoSize.StepBased(
                        maxFontSize = TangemTheme.typography3.caption.medium.fontSize,
                    ),
                )
            } else {
                Text(
                    text = "No data",
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
            }
        )

    }
}

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
    val gapPx = with(density) { TooltipGap.roundToPx() }
    val strokePx = with(density) { DonutStrokeWidth.toPx() }

    val selectedSegment = selectedIndex?.let(segments::getOrNull)
    val positionProvider = remember(
        selectedIndex, segments, chartSize, chartWindowOffset, cardBoundsInWindow, strokePx, gapPx,
    ) {
        segmentTooltipPositionProvider(
            selectedIndex = selectedIndex,
            segments = segments,
            chartSize = chartSize,
            chartWindowOffset = chartWindowOffset,
            strokePx = strokePx,
            cardBoundsInWindow = cardBoundsInWindow,
            gapPx = gapPx,
        )
    }

    DonutSegmentTooltip(
        expanded = selectedSegment != null,
        positionProvider = positionProvider,
        title = selectedSegment?.title.orEmpty(),
        fiatValue = selectedSegment?.fiatValue.orEmpty(),
        percent = selectedSegment?.let { formatSegmentPercent(it.weight) }.orEmpty(),
        onDismissRequest = onDismissRequest
    )
}

@Suppress("MagicNumber")
private fun formatSegmentPercent(weight: Float): String {
    val percent = weight.coerceIn(0f, 1f) * 100
    return if (percent % 1f == 0f) "${percent.toInt()}%" else "%.2f%%".format(percent)
}

private val DonutStrokeWidth = 28.dp
private val DonutStartAngle = -90f
private val TooltipGap = 8.dp

/**
 * Builds the tooltip position provider anchored to the end of the selected slice. Returns the centered
 * fallback while the chart hasn't been measured yet or nothing is selected.
 */
@Suppress("MagicNumber")
private fun segmentTooltipPositionProvider(
    selectedIndex: Int?,
    segments: List<DonutSegment>,
    chartSize: IntSize,
    chartWindowOffset: Offset,
    strokePx: Float,
    cardBoundsInWindow: Rect,
    gapPx: Int,
): PopupPositionProvider {
    if (selectedIndex == null || selectedIndex !in segments.indices ||
        chartSize.width == 0 || chartSize.height == 0
    ) {
        // Not shown in this state (selectedIndex is null / chart not measured) — position is irrelevant.
        return SegmentTooltipPositionProvider(Offset.Zero, Rect.Zero, gapPx)
    }
    val diameter = min(chartSize.width, chartSize.height).toFloat()
    val centerX = chartSize.width / 2f
    val centerY = chartSize.height / 2f
    val innerRadius = diameter / 2f - strokePx / 2
    // End angle of the selected slice (before its round cap) — same layout as DonutChart's drawing pass.
    val sweeps = segments.map { it.weight.coerceIn(0f, 1f) * 360f }
    val endAngleDeg = DonutStartAngle + sweeps.take(selectedIndex + 1).sum()
    val endAngleRad = Math.toRadians(endAngleDeg.toDouble())
    val anchorLocal = Offset(
        x = centerX + innerRadius * cos(endAngleRad).toFloat(),
        y = centerY + innerRadius * sin(endAngleRad).toFloat() - strokePx / 2,
    )

    val anchorInWindow = chartWindowOffset + anchorLocal

    return SegmentTooltipPositionProvider(
        anchorInWindow = anchorInWindow,
        cardBoundsInWindow = cardBoundsInWindow,
        gapPx = gapPx,
        strokePx = strokePx.toInt(),
    )
}

@Composable
private fun ColumnScope.TopHoldingBlock(assetCount: Int, topHoldingPercent: Float) {
    Text(
        modifier = Modifier.padding(horizontal = 16.dp),
        text = "$assetCount assets",
        color = TangemTheme.colors3.text.secondary,
        style = TangemTheme.typography3.heading.small,
    )

    Text(
        modifier = Modifier.padding(horizontal = 16.dp),
        text = "Top holding: ${formatSegmentPercent(topHoldingPercent)}",
        color = TangemTheme.colors3.text.primary,
        style = TangemTheme.typography3.heading.small,
    )
}

@Composable
private fun ColumnScope.CantLoadDataBlock() {
    Text(
        modifier = Modifier.padding(horizontal = 16.dp),
        text = "Can't load data",
        color = TangemTheme.colors3.text.secondary,
        style = TangemTheme.typography3.heading.small,
    )
}

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
                    text = stringReference("Ask for AI summary")
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
                            ) { append("Al Total: ") } // TODO add localization
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

@Preview(name = "MarketChart • Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "MarketChart • Light", showBackground = true)
@Composable
private fun PreviewMarketChart() {
    TangemThemePreviewRedesign {
        Box(
            modifier = Modifier
                .background(TangemTheme.colors3.bg.primary)
                .padding(16.dp),
        ) {
            MarketChart(
                MarketChartState.Loaded(
                    topHoldingPercent = 0.41f,
                    aiInsightState = AiInsightState.Displayed(
                        "Your portfolio leans on a single asset – BTC is 42% of holdings. Stablecoins add 23% " +
                            "buffer. Consider trimmng concentration for a smoother ride",
                    ),
                    donutChartState = DonutChartState.Loaded(
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
                    ),
                ),

                )
        }
    }
}

@Preview(name = "MarketChart • Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "MarketChart • Light", showBackground = true)
@Composable
private fun PreviewMarketChartAskAI() {
    TangemThemePreviewRedesign {
        Box(
            modifier = Modifier
                .background(TangemTheme.colors3.bg.primary)
                .padding(16.dp),
        ) {
            MarketChart(
                MarketChartState.Loaded(
                    topHoldingPercent = 0.41f,
                    aiInsightState = AiInsightState.AskAiInsight(askAiInsightClick = {}),
                    donutChartState = DonutChartState.Loaded(
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
                    ),
                ),

                )
        }
    }
}

@Preview(name = "MarketChart • Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "MarketChart • Light", showBackground = true)
@Composable
private fun PreviewMarketChartNoAi() {
    TangemThemePreviewRedesign {
        Box(
            modifier = Modifier
                .background(TangemTheme.colors3.bg.primary)
                .padding(16.dp),
        ) {
            MarketChart(
                MarketChartState.Loaded(
                    topHoldingPercent = 0.41f,
                    aiInsightState = AiInsightState.Hide,
                    donutChartState = DonutChartState.Loaded(
                        totalAmount = "$10,12345678912.1333",
                        donutSegmentList = listOf(
                            DonutSegment(weight = 0.55f, color = TangemTheme.colors3.border.brand),
                            DonutSegment(weight = 0.45f, color = TangemTheme.colors3.border.accent.green),
                        ),
                    ),
                ),

                )
        }
    }
}

@Preview(name = "MarketChart • Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "MarketChart • Light", showBackground = true)
@Composable
private fun PreviewMarketChartNoData() {
    TangemThemePreviewRedesign {
        Box(
            modifier = Modifier
                .background(TangemTheme.colors3.bg.primary)
                .padding(16.dp),
        ) {
            MarketChart(
                MarketChartState.NoData,
            )
        }
    }
}

// endregion