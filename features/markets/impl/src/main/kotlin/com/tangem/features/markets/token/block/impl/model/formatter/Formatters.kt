package com.tangem.features.markets.token.block.impl.model.formatter

import com.tangem.common.ui.charts.state.MarketChartLook
import com.tangem.core.ui.components.marketprice.PriceChangeType

internal fun PriceChangeType.toChartType(): MarketChartLook.Type {
    return when (this) {
        PriceChangeType.UP -> MarketChartLook.Type.Growing
        PriceChangeType.DOWN -> MarketChartLook.Type.Falling
        PriceChangeType.NEUTRAL -> MarketChartLook.Type.Neutral
    }
}