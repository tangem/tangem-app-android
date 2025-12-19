package com.tangem.domain.markets

import com.tangem.domain.markets.repositories.MarketsTokenRepository

class GetTopFiveMarketTokenUseCase(
    private val marketsTokenRepository: MarketsTokenRepository,
) {
    operator fun invoke(
        batchingContext: TokenListBatchingContext,
        order: TokenMarketListConfig.Order,
    ): TokenListBatchFlow {
        return marketsTokenRepository.getTokenListFlow(
            batchingContext = batchingContext,
            firstBatchSize = DEFAULT_BATCH_SIZE,
            nextBatchSize = NEXT_BATCH_SIZE,
        )
    }

    companion object {
        private const val DEFAULT_BATCH_SIZE = 5
        private const val NEXT_BATCH_SIZE = 0
    }
}