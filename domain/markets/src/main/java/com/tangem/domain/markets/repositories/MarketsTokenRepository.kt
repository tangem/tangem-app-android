package com.tangem.domain.markets.repositories

import com.tangem.domain.markets.*

interface MarketsTokenRepository {

    fun getTokenListFlow(
        batchingContext: TokenListBatchingContext,
        firstBatchSize: Int,
        nextBatchSize: Int,
    ): TokenListBatchFlow

    suspend fun getChart(interval: PriceChangeInterval, tokenId: String): TokenChart
}
