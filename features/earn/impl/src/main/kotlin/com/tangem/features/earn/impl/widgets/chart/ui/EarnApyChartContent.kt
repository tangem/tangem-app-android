package com.tangem.features.earn.impl.widgets.chart.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.ds2.segmentedcontrol.TangemSegmentedControl
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_calculator_20
import com.tangem.core.ui.res.generated.icons.ic_triangle_up_12
import com.tangem.features.earn.impl.widgets.chart.EarnApyChartTexts
import com.tangem.features.earn.impl.widgets.chart.model.EarnApyChartAxis
import com.tangem.features.earn.impl.widgets.chart.ui.state.EarnApyAverageUM
import com.tangem.features.earn.impl.widgets.chart.ui.state.EarnApyBarUM
import com.tangem.features.earn.impl.widgets.chart.ui.state.EarnApyChartUM
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlin.math.abs
import kotlin.math.sin

@Composable
internal fun EarnApyChartContent(state: EarnApyChartUM, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TangemSegmentedControl(items = state.periods, modifier = Modifier.weight(1f))
            state.onCalculatorClick?.let { onClick ->
                TangemButton(
                    variant = TangemButton.Variant.Secondary,
                    size = TangemButton.Size.X10,
                    iconStart = TangemIconUM.Icon(Icons.ic_calculator_20),
                    contentDescription = EarnApyChartTexts.calculator.resolveReference(),
                    onClick = onClick,
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        RateHeader(state = state)
        Spacer(modifier = Modifier.height(16.dp))
        EarnApyPlot(state = state)
    }
}

/** The two blocks share the width equally, so the current rate stays put while the mean appears beside it. */
@Composable
private fun RateHeader(state: EarnApyChartUM, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Column(modifier = Modifier.weight(1f)) {
            RateTitle(title = state.rateTitle)
            RateValue(
                text = state.currentRate,
                color = TangemTheme.colors3.text.accent.blue,
                marker = {
                    Icon(
                        imageVector = Icons.ic_triangle_up_12,
                        contentDescription = null,
                        tint = TangemTheme.colors3.text.accent.blue,
                    )
                },
            )
        }

        val average = state.average
        if (average != null) {
            Column(modifier = Modifier.weight(1f)) {
                RateTitle(title = average.title)
                RateValue(
                    text = average.rate,
                    color = TangemTheme.colors3.text.secondary,
                    marker = { Dot(color = TangemTheme.colors3.text.secondary) },
                )
            }
        } else {
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun RateTitle(title: TextReference) {
    Text(
        text = title.resolveReference(),
        style = TangemTheme.typography3.body.medium,
        color = TangemTheme.colors3.text.primary,
    )
}

@Composable
private fun RateValue(text: TextReference, color: Color, marker: @Composable () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        marker()
        Text(
            text = text.resolveReference(),
            style = TangemTheme.typography3.heading.medium,
            color = color,
        )
    }
}

/** Takes the width of the triangle that marks the current rate, so both values start at the same x. */
@Composable
private fun Dot(color: Color) {
    Box(modifier = Modifier.size(12.dp), contentAlignment = Alignment.Center) {
        Box(modifier = Modifier.size(4.dp).background(color = color, shape = CircleShape))
    }
}

@Preview(name = "Light", widthDp = 360)
@Preview(name = "Dark", widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun EarnApyChartContentPreview() {
    PreviewHost(state = previewState())
}

@Preview(name = "Selected light", widthDp = 360)
@Preview(name = "Selected dark", widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun EarnApyChartContentSelectedPreview() {
    PreviewHost(state = previewState(selectedBar = 4))
}

@Preview(name = "Loading", widthDp = 360)
@Composable
private fun EarnApyChartContentLoadingPreview() {
    PreviewHost(state = previewState(isLoading = true))
}

@Preview(name = "No data", widthDp = 360)
@Composable
private fun EarnApyChartContentNoDataPreview() {
    PreviewHost(state = previewState(barsCount = 0))
}

@Preview(name = "No calculator button", widthDp = 360)
@Composable
private fun EarnApyChartContentNoCalculatorPreview() {
    PreviewHost(state = previewState(hasCalculator = false))
}

@Composable
private fun PreviewHost(state: EarnApyChartUM) {
    TangemThemePreviewRedesign {
        EarnApyChartContent(
            state = state,
            modifier = Modifier
                .background(TangemTheme.colors3.bg.primary)
                .padding(16.dp),
        )
    }
}

@Suppress("MagicNumber")
private fun previewState(
    barsCount: Int = 30,
    selectedBar: Int? = null,
    isLoading: Boolean = false,
    hasCalculator: Boolean = true,
): EarnApyChartUM {
    val values = if (isLoading) emptyList() else List(barsCount) { 1.5 + 7.0 * abs(sin(it * 0.5)) }

    return EarnApyChartUM(
        periods = listOf("1M", "3M", "6M")
            .map { label ->
                TangemSegmentedControl.Item(
                    id = label,
                    label = stringReference(label),
                    isSelected = label == "6M",
                    onClick = {},
                )
            }
            .toImmutableList(),
        rateTitle = EarnApyChartTexts.currentApy,
        currentRate = stringReference("4.49%"),
        bars = values.map { EarnApyBarUM(value = it, tooltip = stringReference("%.2f%% · Mar 12, 2026".format(it))) }
            .toImmutableList(),
        scale = EarnApyChartAxis.scaleOf(tallestColumn = values.maxOrNull() ?: 0.0),
        average = EarnApyAverageUM(
            value = 5.12,
            text = stringReference("Avg 5.12%"),
            title = EarnApyChartTexts.averageApy,
            rate = stringReference("5.12%"),
        ).takeIf { values.isNotEmpty() },
        xLabels = persistentListOf("Jan", "Feb", "Mar", "Apr").takeIf { values.isNotEmpty() } ?: persistentListOf(),
        selectedBar = selectedBar,
        isLoading = isLoading,
        onBarSelected = {},
        onCalculatorClick = {}.takeIf { hasCalculator },
    )
}