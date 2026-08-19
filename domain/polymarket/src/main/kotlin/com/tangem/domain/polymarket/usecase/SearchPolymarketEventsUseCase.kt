package com.tangem.domain.polymarket.usecase

import com.tangem.domain.polymarket.PolymarketRepository
import com.tangem.domain.polymarket.model.PolymarketSearchBatchFlow
import com.tangem.domain.polymarket.model.PolymarketSearchBatchingContext

/**
 * Serves full-text search over discoverable events as a paginated batch flow: the first page arrives on
 * [com.tangem.pagination.BatchAction.Reload] carrying the query, the following ones on
 * [com.tangem.pagination.BatchAction.LoadMore].
 */
class SearchPolymarketEventsUseCase(
    private val polymarketRepository: PolymarketRepository,
) {

    operator fun invoke(
        context: PolymarketSearchBatchingContext,
        batchSize: Int = DEFAULT_BATCH_SIZE,
    ): PolymarketSearchBatchFlow {
        return polymarketRepository.searchEventsBatchFlow(context = context, batchSize = batchSize)
    }

    private companion object {
        const val DEFAULT_BATCH_SIZE = 20
    }
}