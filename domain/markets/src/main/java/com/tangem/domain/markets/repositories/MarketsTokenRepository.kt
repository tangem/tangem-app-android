package com.tangem.domain.markets.repositories

import com.tangem.domain.markets.*

interface MarketsTokenRepository {

    fun getTokenListFlow(
        batchingContext: TokenListBatchingContext,
        firstBatchSize: Int,
        nextBatchSize: Int,
    ): TokenListBatchFlow

    suspend fun getChart(fiatCurrencyCode: String, interval: PriceChangeInterval, tokenId: String): TokenChart

    suspend fun getTokenInfo(fiatCurrencyCode: String, tokenId: String): TokenMarketInfo
}
