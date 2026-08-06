package com.tangem.features.feed.ui.market.detailed.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.tangem.common.ui.charts.MarketChart
import com.tangem.common.ui.charts.getMarketChartBottomAxisHeight
import com.tangem.common.ui.charts.state.MarketChartLook
import com.tangem.common.ui.charts.state.rememberMarketChartState
import com.tangem.core.ui.components.UnableToLoadData
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.feed.ui.market.detailed.state.MarketsTokenDetailsUM

@Composable
internal fun MarketTokenDetailsChart(
    state: MarketsTokenDetailsUM.ChartState,
    backgroundColor: Color,
    modifier: Modifier = Modifier,
) {
    val growingColor = TangemTheme.colors3.icon.accent.blue
    val fallingColor = TangemTheme.colors3.icon.accent.red
    val neutralColor = TangemTheme.colors3.icon.secondary

    val chartState = rememberMarketChartState(
        dataProducer = state.dataProducer,
        colorMapper = { type ->
            when (type) {
                MarketChartLook.Type.Growing -> growingColor
                MarketChartLook.Type.Falling -> fallingColor
                MarketChartLook.Type.Neutral -> neutralColor
            }
        },
        onMarkerShown = state.onMarkerPointSelected,
    )

    val bottomChartAxisHeight = getMarketChartBottomAxisHeight()

    Box(modifier) {
        MarketChart(
            modifier = Modifier.fillMaxWidth(),
            state = chartState,
        )

        if (state.status != MarketsTokenDetailsUM.ChartState.Status.DATA) {
            Box(
                Modifier
                    .drawBehind { drawRect(backgroundColor) }
                    .matchParentSize()
                    .padding(bottom = bottomChartAxisHeight),
            ) {
                when (state.status) {
                    MarketsTokenDetailsUM.ChartState.Status.LOADING -> {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .size(16.dp)
                                .align(Alignment.Center),
                            color = TangemTheme.colors3.icon.accent.neutral,
                            strokeWidth = 2.dp,
                        )
                    }
                    MarketsTokenDetailsUM.ChartState.Status.ERROR -> {
                        UnableToLoadData(
                            modifier = Modifier
                                .padding(
                                    horizontal = 16.dp,
                                    vertical = 12.dp,
                                )
                                .align(Alignment.Center),
                            onRetryClick = state.onLoadRetryClick,
                        )
                    }
                    else -> {}
                }
            }
        }
    }
}