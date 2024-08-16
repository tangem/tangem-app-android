package com.tangem.features.markets.details.impl.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import com.tangem.common.ui.charts.MarketChart
import com.tangem.common.ui.charts.getMarketChartBottomAxisHeight
import com.tangem.common.ui.charts.state.MarketChartLook
import com.tangem.common.ui.charts.state.rememberMarketChartState
import com.tangem.core.ui.res.LocalMainBottomSheetColor
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.markets.details.impl.ui.state.MarketsTokenDetailsUM
import com.tangem.features.markets.tokenlist.impl.ui.components.UnableToLoadData

@Composable
internal fun MarketTokenDetailsChart(state: MarketsTokenDetailsUM.ChartState, modifier: Modifier = Modifier) {
    val growingColor = TangemTheme.colors.icon.accent
    val fallingColor = TangemTheme.colors.icon.warning
    val neutralColor = TangemTheme.colors.icon.informative

    val chartState = rememberMarketChartState(
        dataProducer = state.dataProducer,
        colorMapper = {
            when (it) {
                MarketChartLook.Type.Growing -> growingColor
                MarketChartLook.Type.Falling -> fallingColor
                MarketChartLook.Type.Neutral -> neutralColor
            }
        },
        onMarkerShown = state.onMarkerPointSelected,
    )

    val backgroundColor = LocalMainBottomSheetColor.current.value
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
                                .size(TangemTheme.dimens.size16)
                                .align(Alignment.Center),
                            color = TangemTheme.colors.text.accent,
                            strokeWidth = TangemTheme.dimens.size2,
                        )
                    }
                    MarketsTokenDetailsUM.ChartState.Status.ERROR -> {
                        UnableToLoadData(
                            modifier = Modifier
                                .padding(
                                    horizontal = TangemTheme.dimens.spacing16,
                                    vertical = TangemTheme.dimens.spacing12,
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