@file:Suppress("MagicNumber")

package com.tangem.features.polymarket.impl.common.ui.cryptochart

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.haze.ProvideHaze
import com.tangem.core.ui.components.haze.hazeSourceTangem
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.polymarket.impl.common.ui.cryptochart.PolymarketCryptoChart.Snapshot

@Composable
@Suppress("LongParameterList", "LongMethod")
internal fun CryptoPricePlot(
    snapshot: Snapshot,
    targetLabel: TextReference,
    emptyLabel: TextReference,
    formatPrice: (Double, Int) -> String,
    formatTime: (Long) -> String,
    isCompact: Boolean,
    modifier: Modifier = Modifier,
    liveFrame: State<CryptoChartFrame>? = null,
) {
    val initial = remember(snapshot) { CryptoChartFrame.from(snapshot.points, snapshot.range, snapshot.targetPrice) }
    val frame = liveFrame ?: rememberUpdatedState(initial)
    val isEmpty by remember(frame) { derivedStateOf { frame.value.points.isEmpty() } }
    val tickCount = if (isCompact) 5 else 11
    val style = TangemTheme.typography3.caption.medium.copy(color = TangemTheme.colors3.text.secondary)
    val measurer = rememberTextMeasurer(cacheSize = tickCount)
    val labels = remember(frame, tickCount, formatPrice, style, measurer) {
        derivedStateOf {
            val scale = frame.value.scale
            scale.ticks(tickCount).map { measurer.measure(formatPrice(it, scale.fractionDigits(tickCount)), style) }
        }
    }
    val labelSize by remember(labels) {
        derivedStateOf {
            IntSize(labels.value.maxOf { it.size.width }, labels.value.maxOf { it.size.height })
        }
    }
    val density = LocalDensity.current
    val labelHeight = with(density) { labelSize.height.toDp() }
    val axisWidth = with(density) { labelSize.width.toDp() }
    val plotHeight = if (isCompact) 124.dp else maxOf(364.dp, labelHeight * (tickCount - 1) * 1.5f)
    val lineColor = TangemTheme.colors3.border.accent.yellow
    val gridColor = TangemTheme.colors3.border.primary
    val background = if (isCompact) TangemTheme.colors3.bg.secondary else TangemTheme.colors3.bg.primary
    ProvideHaze {
        Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    Modifier.weight(1f).height(plotHeight).semantics {
                        initial.points.lastOrNull()?.let { point ->
                            contentDescription = listOf(
                                formatPrice(point.price, initial.scale.fractionDigits(tickCount)),
                                formatTime(point.timestampMillis),
                            ).filter(String::isNotEmpty).joinToString()
                        }
                    },
                ) {
                    Canvas(
                        Modifier.matchParentSize().hazeSourceTangem().background(background)
                            .padding(vertical = labelHeight / 2).clearAndSetSemantics {},
                    ) {
                        val dash = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 3.dp.toPx()))
                        val current = frame.value
                        current.scale.ticks(tickCount).forEach { value ->
                            val y = current.scale.y(value) * size.height
                            drawLine(
                                color = gridColor,
                                start = Offset(0f, y),
                                end = Offset(size.width, y),
                                strokeWidth = 1.dp.toPx(),
                                pathEffect = dash,
                            )
                        }
                        drawCryptoPrice(
                            points = current.points,
                            range = current.range,
                            scale = current.scale,
                            color = lineColor,
                        )
                    }
                    if (isEmpty) {
                        Text(
                            text = emptyLabel.resolveReference(),
                            modifier = Modifier.align(Alignment.Center),
                            color = TangemTheme.colors3.text.secondary,
                            style = TangemTheme.typography3.caption.medium,
                        )
                    } else if (snapshot.targetPrice != null) {
                        CryptoChartTarget(
                            price = snapshot.targetPrice,
                            label = targetLabel,
                            frame = frame,
                            labelHeight = labelHeight,
                        )
                    }
                    if (liveFrame != null) {
                        CryptoChartScrubber(
                            frame = frame,
                            formatPrice = formatPrice,
                            formatTime = formatTime,
                            modifier = Modifier.matchParentSize().padding(vertical = labelHeight / 2),
                        )
                    }
                }
                if (!isCompact) {
                    Canvas(Modifier.width(axisWidth).height(plotHeight).clearAndSetSemantics {}) {
                        labels.value.forEachIndexed { index, label ->
                            drawText(
                                label,
                                topLeft = Offset(0f, index * (size.height - labelHeight.toPx()) / (tickCount - 1)),
                            )
                        }
                    }
                }
            }
            if (!isCompact) {
                CryptoChartDates(
                    frame = frame,
                    formatTime = formatTime,
                    modifier = Modifier.fillMaxWidth().padding(end = axisWidth + 8.dp),
                )
            }
        }
    }
}

@Composable
private fun CryptoChartDates(
    frame: State<CryptoChartFrame>,
    formatTime: (Long) -> String,
    modifier: Modifier = Modifier,
) {
    val style = TangemTheme.typography3.caption.medium.copy(color = TangemTheme.colors3.text.secondary)
    val measurer = rememberTextMeasurer()
    val height = with(LocalDensity.current) { style.lineHeight.toDp() }
    Canvas(modifier.height(height).clearAndSetSemantics {}) {
        val range = frame.value.range
        val labels = listOf(
            range.startMillis,
            range.startMillis + (range.endMillis - range.startMillis) / 2,
            range.endMillis,
        )
            .map { measurer.measure(formatTime(it), style) }
        val isStartVisible = labels.first().size.width + labels.last().size.width + 8.dp.toPx() <= size.width
        val isMiddleVisible = labels.sumOf { it.size.width } + 16.dp.toPx() <= size.width
        labels.forEachIndexed { index, label ->
            val isVisible = when (index) {
                0 -> isStartVisible
                1 -> isMiddleVisible
                else -> true
            }
            if (isVisible) drawText(label, topLeft = Offset((size.width - label.size.width) * index / 2, 0f))
        }
    }
}