package com.tangem.features.feed.model.search.state.transformers

import com.tangem.common.ui.charts.state.MarketChartRawData
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.features.feed.ui.search.state.SearchContentUM
import com.tangem.features.feed.ui.search.state.SearchUM
import kotlinx.collections.immutable.toImmutableList

internal class UpdateRecentTokenChartTransformer(
    private val tokenId: CryptoCurrency.RawID,
    private val chartData: MarketChartRawData,
) : SearchUMTransformer {

    override fun transform(prevState: SearchUM): SearchUM {
        val content = prevState.content
        if (content !is SearchContentUM.History) return prevState

        val updatedTokens = content.recentTokens.map { token ->
            if (token.id == tokenId) {
                token.copy(chartData = chartData)
            } else {
                token
            }
        }.toImmutableList()

        return prevState.copy(
            content = content.copy(recentTokens = updatedTokens),
        )
    }
}