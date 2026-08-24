package com.tangem.domain.polymarket.usecase

import com.tangem.domain.polymarket.PolymarketRepository
import com.tangem.domain.polymarket.model.PolymarketEventsBatchFlow
import com.tangem.domain.polymarket.model.PolymarketEventsBatchingContext

/**
 * Serves the Discovery feed as a paginated batch flow: the first page arrives on
 * [com.tangem.pagination.BatchAction.Reload], the following ones on
 * [com.tangem.pagination.BatchAction.LoadMore].
 */
class GetPolymarketEventsBatchFlowUseCase(
    private val polymarketRepository: PolymarketRepository,
) {

    operator fun invoke(
        context: PolymarketEventsBatchingContext,
        batchSize: Int = DEFAULT_BATCH_SIZE,
    ): PolymarketEventsBatchFlow {
        return polymarketRepository.getEventsBatchFlow(context = context, batchSize = batchSize)
    }

    private companion object {
        const val DEFAULT_BATCH_SIZE = 20
    }
}