package com.tangem.domain.markets

import com.tangem.domain.markets.repositories.MarketsTokenRepository
import com.tangem.pagination.BatchFlow
import com.tangem.pagination.BatchingContext

typealias TokenListBatchingContext = BatchingContext<Int, TokenMarketListConfig, TokenMarketUpdateRequest>
typealias TokenListBatchFlow = BatchFlow<Int, List<TokenMarket>, TokenMarketUpdateRequest>

class GetMarketsTokenListFlowUseCase(
    private val marketsTokenRepository: MarketsTokenRepository,
) {
    operator fun invoke(batchingContext: TokenListBatchingContext): TokenListBatchFlow {
        return marketsTokenRepository.getTokenListFlow(batchingContext)
    }
}
