package com.tangem.domain.markets

import com.tangem.domain.markets.repositories.MarketsTokenRepository
import com.tangem.pagination.BatchFlow
import com.tangem.pagination.BatchingContext

typealias TokenListBatchingContext = BatchingContext<Int, TokenMarketListConfig, TokenMarketUpdateRequest>
typealias TokenListBatchFlow = BatchFlow<Int, List<TokenMarket>, TokenMarketUpdateRequest>

class GetMarketsTokenListFlowUseCase(
    private val marketsTokenRepository: MarketsTokenRepository,
) {
    operator fun invoke(batchingContext: TokenListBatchingContext, batchFlowType: BatchFlowType): TokenListBatchFlow {
        return marketsTokenRepository.getTokenListFlow(
            batchingContext = batchingContext,
            firstBatchSize = batchFlowType.firstBatchSize,
            nextBatchSize = batchFlowType.nextBatchSize,
        )
// [REDACTED_TODO_COMMENT]
    }

    enum class BatchFlowType(
        val firstBatchSize: Int,
        val nextBatchSize: Int,
    ) {
        Main(firstBatchSize = 150, nextBatchSize = 100),
        Search(firstBatchSize = 50, nextBatchSize = 50),
    }
}
