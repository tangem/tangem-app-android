package com.tangem.features.feed.model.search.state.transformers

import com.tangem.features.feed.ui.feed.state.MarketChartUM
import com.tangem.features.feed.ui.search.state.SearchUM

internal class SetTopMarketsTransformer(private val topMarkets: MarketChartUM) : SearchUMTransformer {

    override fun transform(prevState: SearchUM): SearchUM {
        return prevState.copy(topMarkets = topMarkets)
    }
}