package com.tangem.domain.markets.repositories

import com.tangem.domain.markets.TokenMarket
import com.tangem.domain.markets.TokenMarketListConfig
import com.tangem.domain.markets.TokenMarketUpdateRequest
import com.tangem.pagination.BatchFlow
import com.tangem.pagination.BatchingContext

interface MarketsTokenRepository {

    suspend fun getTokenListFlow(
        batchingContext: BatchingContext<Int, TokenMarketListConfig, TokenMarketUpdateRequest>,
    ): BatchFlow<Int, List<TokenMarket>, TokenMarketUpdateRequest>
}
