package com.tangem.features.markets.token.block.impl.ui.state

import com.tangem.common.ui.charts.state.MarketChartRawData
import com.tangem.core.ui.components.marketprice.PriceChangeType
import com.tangem.core.ui.extensions.TextReference
import java.math.BigDecimal

internal data class TokenMarketBlockUM(
    val currencySymbol: String,
    val currentPrice: String?,
    val currentPriceValue: BigDecimal?,
    val priceAnnotated: TextReference?,
    val h24Percent: String?,
    val priceChangeType: PriceChangeType,
    val chartData: MarketChartRawData?,
    val onClick: () -> Unit,
)