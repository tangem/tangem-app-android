package com.tangem.domain.markets.repositories

import com.tangem.domain.markets.*

interface MarketsTokenRepository {

    fun getTokenListFlow(batchingContext: TokenListBatchingContext): TokenListBatchFlow
}