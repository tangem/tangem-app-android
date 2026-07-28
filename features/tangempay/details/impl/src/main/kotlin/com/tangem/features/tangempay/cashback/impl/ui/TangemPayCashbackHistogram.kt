package com.tangem.features.tangempay.cashback.impl.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.features.tangempay.cashback.impl.ui.state.TangemPayCashbackHistogramUM
import com.tangem.features.tangempay.cashback.impl.ui.state.TangemPayCashbackHistogramUM.Style
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Composable
internal fun TangemPayCashbackHistogram(state: TangemPayCashbackHistogramUM, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .padding(16.dp),
        ) {
            Text(
                text = state.title.resolveReference(),
                style = TangemTheme.typography3.heading.small,
                color = TangemTheme.colors3.text.primary,
            )
        }
        Chart(bars = state.bars)
    }
}

@Composable
private fun Chart(bars: ImmutableList<TangemPayCashbackHistogramUM.Bar>, modifier: Modifier = Modifier) {
    val maxValue = bars.maxOfOrNull { it.amountValue }?.coerceAtLeast(0f) ?: 0f
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            bars.forEach { bar ->
                BarColumn(bar = bar, maxValue = maxValue, modifier = Modifier.weight(1f))
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(TangemTheme.colors3.border.primary),
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            bars.forEach { bar ->
                Text(
                    text = bar.month.resolveReference(),
                    style = TangemTheme.typography3.caption.medium,
                    color = monthColor(bar.style),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun BarColumn(bar: TangemPayCashbackHistogramUM.Bar, maxValue: Float, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        if (bar.style != Style.Regular || bar.amountValue != 0f) {
            Text(
                text = bar.amount.resolveReference(),
                style = TangemTheme.typography3.caption.medium,
                color = amountColor(bar.style),
                textAlign = TextAlign.Center,
                maxLines = 1,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(4.dp))
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(barHeight(value = bar.amountValue, maxValue = maxValue))
                .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                .background(barColor(bar.style)),
        )
    }
}

private fun barHeight(value: Float, maxValue: Float): Dp {
    if (maxValue <= 0f) return 3.dp
    val fraction = (value / maxValue).coerceIn(0f, 1f)
    return maxOf(3.dp, 115.dp * fraction)
}

@Composable
private fun barColor(style: Style): Color = when (style) {
    Style.Regular -> TangemTheme.colors3.bg.opaque.secondary
    Style.Highlighted -> TangemTheme.colors3.bg.brand
    Style.HighlightedNegative -> TangemTheme.colors3.bg.status.error
}

@Composable
private fun amountColor(style: Style): Color = when (style) {
    Style.Regular -> TangemTheme.colors3.text.tertiary
    else -> TangemTheme.colors3.text.primary
}

@Composable
private fun monthColor(style: Style): Color = when (style) {
    Style.Regular -> TangemTheme.colors3.text.tertiary
    else -> TangemTheme.colors3.text.secondary
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun TangemPayCashbackHistogramPreview(
    @PreviewParameter(TangemPayCashbackHistogramUMProvider::class) state: TangemPayCashbackHistogramUM,
) {
    TangemThemePreviewRedesign {
        TangemPayCashbackHistogram(
            state = state,
            modifier = Modifier
                .fillMaxWidth()
                .background(TangemTheme.colors3.bg.primary),
        )
    }
}

@Suppress("MagicNumber")
private class TangemPayCashbackHistogramUMProvider : CollectionPreviewParameterProvider<TangemPayCashbackHistogramUM>(
    collection = listOf(
        // Populated
        TangemPayCashbackHistogramUM(
            title = stringReference("$132.15 earned in total"),
            bars = persistentListOf(
                bar(month = "Feb", amount = "$12.02", value = 12.02f, style = Style.Regular),
                bar(month = "Mar", amount = "$44.22", value = 44.22f, style = Style.Regular),
                bar(month = "Apr", amount = "$38.52", value = 38.52f, style = Style.Regular),
                bar(month = "May", amount = "$26.10", value = 26.10f, style = Style.Regular),
                bar(month = "Jun", amount = "$32.15", value = 32.15f, style = Style.Highlighted),
            ),
        ),
        // Negative current month
        TangemPayCashbackHistogramUM(
            title = stringReference("$132.15 earned in total"),
            bars = persistentListOf(
                bar(month = "Feb", amount = "$12.02", value = 12.02f, style = Style.Regular),
                bar(month = "Mar", amount = "$44.22", value = 44.22f, style = Style.Regular),
                bar(month = "Apr", amount = "$38.52", value = 38.52f, style = Style.Regular),
                bar(month = "May", amount = "$26.10", value = 26.10f, style = Style.Regular),
                bar(month = "Jun", amount = "-$2.15", value = -2.15f, style = Style.HighlightedNegative),
            ),
        ),
        // Empty
        TangemPayCashbackHistogramUM(
            title = stringReference("$0 earned in total"),
            bars = persistentListOf(
                bar(month = "Feb", amount = "$0.00", value = 0f, style = Style.Regular),
                bar(month = "Mar", amount = "$0.00", value = 0f, style = Style.Regular),
                bar(month = "Apr", amount = "$0.00", value = 0f, style = Style.Regular),
                bar(month = "May", amount = "$0.00", value = 0f, style = Style.Regular),
                bar(month = "Jun", amount = "$0.00", value = 0f, style = Style.Highlighted),
            ),
        ),
    ),
)

private fun bar(month: String, amount: String, value: Float, style: Style) = TangemPayCashbackHistogramUM.Bar(
    month = stringReference(month),
    amount = stringReference(amount),
    amountValue = value,
    style = style,
)