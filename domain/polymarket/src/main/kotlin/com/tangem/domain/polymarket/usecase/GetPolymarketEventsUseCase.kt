package com.tangem.domain.polymarket.usecase

import arrow.core.Either
import com.tangem.domain.core.error.DataError
import com.tangem.domain.polymarket.PolymarketRepository
import com.tangem.domain.polymarket.model.PolymarketEvent

/**
 * Serves the Discovery feed of prediction events (each with its top active markets) from the BFF.
 */
class GetPolymarketEventsUseCase(
    private val polymarketRepository: PolymarketRepository,
) {

    /**
     * @param category optional category id to filter by; `null` for the default (Trending) feed.
     */
    suspend operator fun invoke(category: Int? = null): Either<DataError, List<PolymarketEvent>> {
        return polymarketRepository.getEvents(category = category)
    }
}