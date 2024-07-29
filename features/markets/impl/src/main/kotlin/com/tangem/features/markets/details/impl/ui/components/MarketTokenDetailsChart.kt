package com.tangem.features.markets.details.impl.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.tangem.common.ui.charts.MarketChart
import com.tangem.common.ui.charts.state.rememberMarketChartState
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.markets.details.impl.ui.entity.MarketsTokenDetailsUM
import com.tangem.features.markets.tokenlist.impl.ui.components.UnableToLoadData

@Composable
fun MarketTokenDetailsChart(state: MarketsTokenDetailsUM.ChartState, modifier: Modifier = Modifier) {
    val chartState = rememberMarketChartState(state.dataProducer)

    MarketChart(
        modifier = modifier,
        state = chartState,
        noChartContent = {
            UnableToLoadData(
                modifier = Modifier
                    .padding(horizontal = TangemTheme.dimens.spacing16, vertical = TangemTheme.dimens.spacing12)
                    .align(Alignment.Center),
                onRetryClick = state.onLoadRetryClick,
            )
        },
    )
}
