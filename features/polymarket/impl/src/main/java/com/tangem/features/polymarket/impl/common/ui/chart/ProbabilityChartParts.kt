@file:Suppress("MagicNumber")

package com.tangem.features.polymarket.impl.common.ui.chart

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.currentStateAsState
import com.tangem.core.ui.ds2.shimmers.ProvideTangemShimmer
import com.tangem.core.ui.ds2.shimmers.TangemShimmer
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_chart_line_vertical_20
import com.tangem.features.polymarket.impl.common.formatPolymarketPercent
import com.tangem.features.polymarket.impl.common.ui.chart.PolymarketProbabilityChart.SeriesColor
import com.tangem.features.polymarket.impl.common.ui.chart.PolymarketProbabilityChart.State.Content
import com.tangem.utils.StringsSigns.DASH_SIGN
import kotlin.math.roundToInt

@Composable
@ReadOnlyComposable
internal fun SeriesColor.lineColor(): Color = when (this) {
    SeriesColor.Blue -> TangemTheme.colors3.border.accent.blue
    SeriesColor.Red -> TangemTheme.colors3.border.accent.red
    SeriesColor.Green -> TangemTheme.colors3.border.accent.green
    SeriesColor.Yellow -> TangemTheme.colors3.border.accent.yellow
    SeriesColor.Neutral -> TangemTheme.colors3.border.accent.neutral
}

internal fun Double?.probabilityText(): String =
    this?.let { (it * 100).roundToInt().formatPolymarketPercent() } ?: DASH_SIGN

@Composable
internal fun ProbabilityChartLegend(content: Content?, isLoading: Boolean, lineCount: Int, selectedTime: Long?) {
    val lineHeight = with(LocalDensity.current) { TangemTheme.typography3.caption.medium.lineHeight.toDp() }
    if (isLoading) {
        ProvideTangemShimmer {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                repeat(lineCount.coerceAtLeast(1)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TangemShimmer(Modifier.size(120.dp, lineHeight), radius = 24.dp)
                        TangemShimmer(Modifier.size(96.dp, lineHeight), radius = 24.dp)
                    }
                }
            }
        }
    } else if (content == null) {
        Spacer(Modifier.height(24.dp))
    } else {
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            content.series.forEach { series ->
                val probability = if (selectedTime == null) {
                    series.points.lastOrNull()?.probability
                } else {
                    series.points.probabilityAt(selectedTime)
                }
                Row(Modifier.semantics(mergeDescendants = true) {}, verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(12.dp).clearAndSetSemantics {}, contentAlignment = Alignment.Center) {
                        Spacer(Modifier.size(4.dp).background(series.color.lineColor(), CircleShape))
                    }
                    Text(
                        text = probability.probabilityText(),
                        color = TangemTheme.colors3.text.primary,
                        style = TangemTheme.typography3.caption.medium,
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = series.title,
                        color = TangemTheme.colors3.text.secondary,
                        style = TangemTheme.typography3.caption.medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
internal fun ProbabilityChartEmpty(title: TextReference, description: TextReference) {
    Column(
        modifier = Modifier.background(
            TangemTheme.colors3.bg.primary,
            RoundedCornerShape(24.dp),
        ).padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(Icons.ic_chart_line_vertical_20, null, tint = TangemTheme.colors3.icon.secondary)
        Text(
            text = title.resolveReference(),
            color = TangemTheme.colors3.text.primary,
            style = TangemTheme.typography3.subheading.medium,
            textAlign = TextAlign.Center,
        )
        Text(
            text = description.resolveReference(),
            color = TangemTheme.colors3.text.secondary,
            style = TangemTheme.typography3.caption.medium,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
internal fun ProbabilityChartDates(
    scale: ProbabilityChartScale?,
    selectedTime: Long?,
    formatTime: (Long) -> String,
    isLoading: Boolean,
    modifier: Modifier = Modifier,
) {
    val height = with(LocalDensity.current) { TangemTheme.typography3.caption.medium.lineHeight.toDp() }
    BoxWithConstraints(modifier.heightIn(min = height)) {
        when {
            isLoading -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                repeat(3) { TangemShimmer(Modifier.size(55.dp, height), radius = 24.dp) }
            }
            selectedTime != null -> Text(
                text = formatTime(selectedTime),
                modifier = Modifier.fillMaxWidth(),
                color = TangemTheme.colors3.text.primary,
                style = TangemTheme.typography3.caption.medium,
                textAlign = TextAlign.Center,
            )
            scale != null -> {
                val labels = listOf(scale.start, scale.start + (scale.end - scale.start) / 2, scale.end).map(formatTime)
                val measurer = rememberTextMeasurer()
                val style = TangemTheme.typography3.caption.medium
                val requiredWidth = labels.sumOf { measurer.measure(it, style).size.width }
                val availableWidth = with(LocalDensity.current) { (maxWidth - 16.dp).toPx() }
                val visible = if (requiredWidth <= availableWidth) labels else listOf(labels.first(), labels.last())
                Row(Modifier.fillMaxWidth()) {
                    visible.forEachIndexed { index, label ->
                        Text(
                            text = label,
                            modifier = Modifier.weight(1f),
                            color = TangemTheme.colors3.text.secondary,
                            style = style,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = when (index) {
                                0 -> TextAlign.Start
                                visible.lastIndex -> TextAlign.End
                                else -> TextAlign.Center
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun rememberChartPulse(isEnabled: Boolean): State<Float> {
    val lifecycle by LocalLifecycleOwner.current.lifecycle.currentStateAsState()
    return if (isEnabled && lifecycle.isAtLeast(Lifecycle.State.STARTED)) {
        rememberInfiniteTransition(label = "Probability pulse").animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(1400, easing = LinearOutSlowInEasing)),
            label = "Probability pulse radius",
        )
    } else {
        rememberUpdatedState(0f)
    }
}