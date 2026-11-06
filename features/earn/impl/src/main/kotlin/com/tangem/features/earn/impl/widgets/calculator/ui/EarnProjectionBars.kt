package com.tangem.features.earn.impl.widgets.calculator.ui

import android.content.res.Configuration
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.features.earn.impl.widgets.calculator.ui.state.EarnProjectionBarUM
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

@Composable
internal fun EarnProjectionBars(
    bars: ImmutableList<EarnProjectionBarUM>,
    axisLabels: ImmutableList<TextReference>,
    modifier: Modifier = Modifier,
) {
    val initialColor = TangemTheme.colors3.icon.tertiary
    val totalColor = TangemTheme.colors3.icon.brand
    val topUpColor = totalColor.copy(alpha = TOP_UP_ALPHA)

    Column(modifier = modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(CHART_HEIGHT),
        ) {
            if (bars.isEmpty()) return@Canvas

            val barWidth = minOf(size.width / (bars.size * 2 - 1), MAX_BAR_WIDTH.toPx())
            val gap = if (bars.size > 1) (size.width - barWidth * bars.size) / (bars.size - 1) else 0f
            val cornerRadius = CornerRadius(barWidth / 2)
            val layerGap = LAYER_GAP.toPx()
            val layersHeight = size.height - layerGap * (LAYERS_COUNT - 1)

            bars.forEachIndexed { index, bar ->
                var bottom = size.height
                val layers = listOf(bar.initial to initialColor, bar.total to totalColor, bar.topUp to topUpColor)

                layers.forEach { (share, color) ->
                    val layerHeight = share * layersHeight
                    if (layerHeight > 0f) {
                        drawRoundRect(
                            color = color,
                            topLeft = Offset(x = index * (barWidth + gap), y = bottom - layerHeight),
                            size = Size(width = barWidth, height = layerHeight),
                            cornerRadius = cornerRadius,
                        )
                        bottom -= layerHeight + layerGap
                    }
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

private const val TOP_UP_ALPHA = 0.3f
private const val LAYERS_COUNT = 3
private val CHART_HEIGHT = 120.dp
private val MAX_BAR_WIDTH = 6.dp
private val LAYER_GAP = 2.dp

@Preview(name = "Light")
@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun EarnProjectionBarsPreview() {
    TangemThemePreviewRedesign {
        EarnProjectionBars(
            bars = List(PREVIEW_BARS) { index ->
                val step = (index + 1).toFloat() / PREVIEW_BARS
                EarnProjectionBarUM(initial = 0.1f, total = 0.8f * step, topUp = 0.1f)
            }.toImmutableList(),
            axisLabels = listOf(stringReference("1Y"), stringReference("2Y"), stringReference("3Y")).toImmutableList(),
            modifier = Modifier
                .background(TangemTheme.colors3.bg.primary)
                .padding(16.dp),
        )
    }
}

private const val PREVIEW_BARS = 24