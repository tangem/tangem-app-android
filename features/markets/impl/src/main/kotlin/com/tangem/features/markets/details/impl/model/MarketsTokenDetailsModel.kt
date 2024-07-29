package com.tangem.features.markets.details.impl.model

import com.tangem.common.ui.charts.state.MarketChartData
import com.tangem.common.ui.charts.state.MarketChartDataProducer
import com.tangem.common.ui.charts.state.MarketChartLook
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.ui.components.marketprice.PriceChangeType
import com.tangem.domain.markets.GetTokenPriceChartUseCase
import com.tangem.domain.markets.PriceChangeInterval
import com.tangem.features.markets.details.api.MarketsTokenDetailsComponent
import com.tangem.features.markets.details.impl.ui.entity.MarketsTokenDetailsUM
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.math.BigDecimal
import javax.inject.Inject

internal class MarketsTokenDetailsModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    private val getTokenPriceChartUseCase: GetTokenPriceChartUseCase,
) : Model() {

    val params = paramsContainer.require<MarketsTokenDetailsComponent.Params>()

    private val chartDataProducer = MarketChartDataProducer.build(dispatcher = dispatchers.default) {
        chartData = MarketChartData.NoData.Loading
    }

    val state = MutableStateFlow(
        MarketsTokenDetailsUM(
            tokenName = params.token.name,
            dateTimeText = "datetime",
            priceChangePercentText = params.token.tokenQuotes.h24Percent.toString(),
            priceChangeType = if (params.token.tokenQuotes.h24Percent < BigDecimal.ZERO) {
                PriceChangeType.DOWN
            } else {
                PriceChangeType.UP
            },
            chartState = MarketsTokenDetailsUM.ChartState(
                dataProducer = chartDataProducer,
                chartLook = MarketChartLook(),
                onLoadRetryClick = {},
            ),
        ),
    )

    init {
        modelScope.launch {
            val chart = getTokenPriceChartUseCase.invoke(PriceChangeInterval.H24, params.token.id)

            chart.onRight {
                chartDataProducer.runTransactionSuspend {
                    chartData = MarketChartData.Data()
                }
            }
        }
    }
}