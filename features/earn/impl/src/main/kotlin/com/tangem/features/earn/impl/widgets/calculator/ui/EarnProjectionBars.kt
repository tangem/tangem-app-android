package com.tangem.features.earn.impl.widgets.calculator.ui

import android.content.res.Configuration
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.haze.hazeSourceTangem
import com.tangem.core.ui.ds2.surface.TangemSurface
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.haptic.HapticManager
import com.tangem.core.ui.haptic.TangemHapticEffect
import com.tangem.core.ui.res.LocalHapticManager
import com.tangem.core.ui.res.LocalHazeState
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.features.earn.impl.ui.EARN_CHART_DIMMED_ALPHA
import com.tangem.features.earn.impl.widgets.calculator.ui.state.EarnProjectionBarUM
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.rememberHazeState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlin.math.roundToInt

@Composable
internal fun EarnProjectionBars(
    bars: ImmutableList<EarnProjectionBarUM>,
    axisLabels: ImmutableList<TextReference>,
    selectedBar: Int?,
    onBarSelect: (Int?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val currentOnBarSelect by rememberUpdatedState(onBarSelect)
    val haptic = LocalHapticManager.current
    val tooltipBar = selectedBar?.let { index -> bars.getOrNull(index) }
    val chartHazeState = rememberChartHazeState()

    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(CHART_HEIGHT)
                .pointerInput(bars, haptic) {
                    detectBarSelection(
                        bars = bars,
                        haptic = haptic,
                        onBarSelect = { index -> currentOnBarSelect(index) },
                    )
                },
        ) {
            BarsCanvas(
                bars = bars,
                selectedBar = selectedBar,
                modifier = Modifier
                    .fillMaxSize()
                    .hazeSourceTangem(state = chartHazeState),
            )
            if (selectedBar != null && tooltipBar != null) {
                CompositionLocalProvider(LocalHazeState provides chartHazeState) {
                    BarTooltip(
                        bar = tooltipBar,
                        barIndex = selectedBar,
                        barCount = bars.size,
                        modifier = Modifier.matchParentSize(),
                    )
                }
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
        ) {
            axisLabels.forEach { label ->
                Text(
                    text = label.resolveReference(),
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.End,
                    style = TangemTheme.typography3.caption.medium,
                    color = TangemTheme.colors3.text.secondary,
                )
            }
        }
    }
}

// Haze draws nothing for an effect nested in a source of its own state, and both hosts wrap the chart in one
@Composable
private fun rememberChartHazeState(): HazeState = rememberHazeState(blurEnabled = LocalHazeState.current.blurEnabled)

@Composable
private fun BarsCanvas(bars: ImmutableList<EarnProjectionBarUM>, selectedBar: Int?, modifier: Modifier = Modifier) {
    val barColor = TangemTheme.colors3.icon.brand
    val dimmedColor = barColor.copy(alpha = EARN_CHART_DIMMED_ALPHA)

    Canvas(modifier = modifier) {
        val geometry = EarnBarsGeometry(
            width = size.width,
            barCount = bars.size,
            maxBarWidth = MAX_BAR_WIDTH.toPx(),
        )
        val cornerRadius = CornerRadius(geometry.barWidth / 2)

        bars.forEachIndexed { index, bar ->
            val barHeight = bar.height * size.height
            if (barHeight > 0f) {
                drawRoundRect(
                    color = if (selectedBar == null || index == selectedBar) barColor else dimmedColor,
                    topLeft = Offset(x = geometry.barLeft(index), y = size.height - barHeight),
                    size = Size(width = geometry.barWidth, height = barHeight),
                    cornerRadius = cornerRadius,
                )
            }
        }
    }
}

@Composable
private fun BarTooltip(bar: EarnProjectionBarUM, barIndex: Int, barCount: Int, modifier: Modifier = Modifier) {
    Layout(
        content = {
            TangemSurface(
                isMaterial = true,
                shape = RoundedCornerShape(percent = 50),
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Text(
                        text = bar.balance.resolveReference(),
                        style = TangemTheme.typography3.caption.medium,
                        color = TangemTheme.colors3.text.primary,
                        maxLines = 1,
                    )
                    Text(
                        text = bar.earned.resolveReference(),
                        style = TangemTheme.typography3.caption.medium,
                        color = TangemTheme.colors3.text.brand,
                        maxLines = 1,
                    )
                }
            }
        },
        modifier = modifier,
    ) { measurables, constraints ->
        val tooltip = measurables.single().measure(Constraints())
        val chartWidth = constraints.maxWidth.toFloat()
        val geometry = EarnBarsGeometry(
            width = chartWidth,
            barCount = barCount,
            maxBarWidth = MAX_BAR_WIDTH.toPx(),
        )
        val position = earnTooltipPosition(
            barCenter = geometry.barCenter(barIndex),
            barWidth = geometry.barWidth,
            barTop = constraints.maxHeight * (1f - bar.height),
            tooltipWidth = tooltip.width.toFloat(),
            tooltipHeight = tooltip.height.toFloat(),
            chartWidth = chartWidth,
            spacing = TOOLTIP_SPACING.toPx(),
        )

        layout(width = constraints.maxWidth, height = constraints.maxHeight) {
            tooltip.place(x = position.x.roundToInt(), y = position.y.roundToInt())
        }
    }
}

private suspend fun PointerInputScope.detectBarSelection(
    bars: List<EarnProjectionBarUM>,
    haptic: HapticManager,
    onBarSelect: (Int?) -> Unit,
) {
    awaitEachGesture {
        val down = awaitFirstDown()
        val geometry = EarnBarsGeometry(
            width = size.width.toFloat(),
            barCount = bars.size,
            maxBarWidth = MAX_BAR_WIDTH.toPx(),
        )
        var current: Int? = null

        try {
            current = selectBar(
                index = geometry.touchedBar(x = down.position.x, bars = bars),
                previous = null,
                haptic = haptic,
                onBarSelect = onBarSelect,
            )
            while (true) {
                // Events stay unconsumed so a scrolling parent keeps its drag; once it consumes one, the selection ends
                val change = awaitPointerEvent(PointerEventPass.Final).changes.firstOrNull { it.id == down.id }
                    ?: break
                if (!change.pressed || change.isConsumed) break

                current = selectBar(
                    index = geometry.touchedBar(x = change.position.x, bars = bars),
                    previous = current,
                    haptic = haptic,
                    onBarSelect = onBarSelect,
                )
            }
        } finally {
            selectBar(index = null, previous = current, haptic = haptic, onBarSelect = onBarSelect)
        }
    }
}

internal fun EarnBarsGeometry.touchedBar(x: Float, bars: List<EarnProjectionBarUM>): Int? =
    if (bars.any { it.height > 0f }) barAt(x) else null

internal fun selectBar(index: Int?, previous: Int?, haptic: HapticManager, onBarSelect: (Int?) -> Unit): Int? {
    if (index == previous) return previous

    onBarSelect(index)
    if (index != null) {
        haptic.perform(
            if (previous == null) TangemHapticEffect.View.ContextClick else TangemHapticEffect.View.TextHandleMove,
        )
    }

    return index
}

private val CHART_HEIGHT = 120.dp
private val MAX_BAR_WIDTH = 6.dp
private val TOOLTIP_SPACING = 4.dp

@Preview(name = "Light")
@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun EarnProjectionBarsPreview() {
    PreviewBars(selectedBar = null)
}

@Preview(name = "Light")
@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun EarnProjectionBarsSelectedPreview() {
    PreviewBars(selectedBar = PREVIEW_SELECTED_BAR)
}

@Preview(name = "Light")
@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun EarnProjectionBarsLastSelectedPreview() {
    PreviewBars(selectedBar = PREVIEW_BARS - 1)
}

@Composable
private fun PreviewBars(selectedBar: Int?) {
    TangemThemePreviewRedesign {
        EarnProjectionBars(
            bars = List(PREVIEW_BARS) { index ->
                val step = (index + 1).toFloat() / PREVIEW_BARS
                EarnProjectionBarUM(
                    height = maxOf(step * step, PREVIEW_MIN_SHARE),
                    balance = stringReference("$12,345"),
                    earned = stringReference("+$1,234"),
                )
            }.toImmutableList(),
            axisLabels = listOf(stringReference("1Y"), stringReference("2Y"), stringReference("3Y")).toImmutableList(),
            selectedBar = selectedBar,
            onBarSelect = {},
            modifier = Modifier
                .background(TangemTheme.colors3.bg.primary)
                .padding(16.dp),
        )
    }
}

private const val PREVIEW_BARS = 24
private const val PREVIEW_SELECTED_BAR = 15
private const val PREVIEW_MIN_SHARE = 0.05f