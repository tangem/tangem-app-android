package com.tangem.domain.polymarket.usecase

import arrow.core.Either
import com.tangem.domain.polymarket.PolymarketRepository
import com.tangem.domain.polymarket.model.PolymarketEvent
import com.tangem.domain.polymarket.model.PolymarketEventError

/**
 * Serves the details of a single prediction event, carrying all of its markets
 * (unlike the Discovery feed, which carries only the top active ones).
 */
class GetPolymarketEventUseCase(
    private val polymarketRepository: PolymarketRepository,
) {

    suspend operator fun invoke(eventId: String): Either<PolymarketEventError, PolymarketEvent> {
        return polymarketRepository.getEvent(eventId = eventId)
    }
}