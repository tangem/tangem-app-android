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
import com.tangem.common.ui.charts.state.MarketChartLook
import com.tangem.common.ui.charts.state.rememberMarketChartState
import com.tangem.core.ui.res.LocalMainBottomSheetColor
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.markets.details.impl.ui.entity.MarketsTokenDetailsUM
import com.tangem.features.markets.tokenlist.impl.ui.components.UnableToLoadData

@Composable
fun MarketTokenDetailsChart(state: MarketsTokenDetailsUM.ChartState, modifier: Modifier = Modifier) {
    val growingColor = TangemTheme.colors.icon.accent
    val fallingColor = TangemTheme.colors.icon.warning

    val chartState = rememberMarketChartState(
        dataProducer = state.dataProducer,
        colorMapper = {
            when (it) {
                MarketChartLook.Type.Growing -> growingColor
                MarketChartLook.Type.Falling -> fallingColor
            }
        },
        onMarkerShown = state.onMarkerPointSelected,
    )

    val backgroundColor = LocalMainBottomSheetColor.current.value

    Box(modifier) {
        MarketChart(
            modifier = Modifier.fillMaxWidth(),
            state = chartState,
        )

        if (state.status != MarketsTokenDetailsUM.ChartState.Status.DATA) {
            Box(
                Modifier
                    .drawBehind { drawRect(backgroundColor) }
                    .matchParentSize(),
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