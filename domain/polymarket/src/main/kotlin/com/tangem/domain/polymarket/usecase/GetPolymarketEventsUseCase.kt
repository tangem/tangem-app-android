package com.tangem.domain.polymarket.usecase

import arrow.core.Either
import arrow.core.right
import com.tangem.domain.core.error.DataError
import com.tangem.domain.polymarket.model.PolymarketEvent

/**
 * Serves the Discovery feed.
 *
 * The feed is built against static fixtures ([PolymarketMockData]) so the UI can be developed without depending on
 * the shared [com.tangem.domain.polymarket.PolymarketRepository] binding — the BFF Discovery endpoint is not
 * deployed yet, and keeping the mock here avoids touching the data layer that other teams evolve in parallel.
 */
class GetPolymarketEventsUseCase {

    suspend operator fun invoke(): Either<DataError, List<PolymarketEvent>> {
        // TODO([REDACTED_TASK_KEY]): serve events from PolymarketRepository.getEvents() once the BFF Discovery endpoint is live
        return PolymarketMockData.events.right()
    }
}