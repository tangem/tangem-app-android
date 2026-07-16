package com.tangem.domain.polymarket.usecase

import arrow.core.Either
import com.tangem.domain.core.error.DataError
import com.tangem.domain.polymarket.PolymarketRepository
import com.tangem.domain.polymarket.model.PolymarketEvent

class GetPolymarketEventsUseCase(
    private val polymarketRepository: PolymarketRepository,
) {

    suspend operator fun invoke(): Either<DataError, List<PolymarketEvent>> {
        return polymarketRepository.getEvents()
    }
}