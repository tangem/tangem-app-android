package com.tangem.domain.markets

import com.tangem.domain.markets.repositories.MarketsTokenRepository
import com.tangem.pagination.BatchFlow
import com.tangem.pagination.BatchingContext

typealias MarketCategoryListBatchingContext = BatchingContext<Int, MarketCategoryListConfig, Nothing>
typealias MarketCategoryListBatchFlow = BatchFlow<Int, List<MarketCategory>, Nothing>

/**
 * Get a paginated flow of market categories
 *
 * Categories carry no live-updating data, so the flow has no update channel — `TUpdate` is [Nothing].
 *
 * @property marketsTokenRepository markets token repository
 */
class GetMarketCategoryListFlowUseCase(
    private val marketsTokenRepository: MarketsTokenRepository,
) {

    operator fun invoke(
        batchingContext: MarketCategoryListBatchingContext,
        batchFlowType: BatchFlowType = BatchFlowType.Main,
    ): MarketCategoryListBatchFlow {
        return marketsTokenRepository.getMarketCategoryListFlow(
            batchingContext = batchingContext,
            firstBatchSize = batchFlowType.firstBatchSize,
            nextBatchSize = batchFlowType.nextBatchSize,
        )
    }

    enum class BatchFlowType(
        val firstBatchSize: Int,
        val nextBatchSize: Int,
    ) {
        Main(firstBatchSize = 100, nextBatchSize = 100),
    }
}