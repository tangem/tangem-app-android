package com.tangem.domain.polymarket

import arrow.core.Either
import com.tangem.domain.core.error.DataError
import com.tangem.domain.polymarket.model.PolymarketEvent

interface PolymarketRepository {

    /**
     * Fetch the Discovery feed of prediction events (each with its top active markets).
     */
    suspend fun getEvents(): Either<DataError, List<PolymarketEvent>>
}