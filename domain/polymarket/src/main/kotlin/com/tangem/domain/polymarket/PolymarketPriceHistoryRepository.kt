package com.tangem.domain.polymarket

import arrow.core.Either
import com.tangem.domain.polymarket.model.PolymarketPriceHistoryError
import com.tangem.domain.polymarket.model.PolymarketPriceHistoryQuery
import com.tangem.domain.polymarket.model.PolymarketPricePoint

interface PolymarketPriceHistoryRepository {

    /** No shared cache: the event's chart session owns period caching and stale-request rejection. */
    suspend fun getPriceHistory(
        assetId: String,
        query: PolymarketPriceHistoryQuery,
    ): Either<PolymarketPriceHistoryError, List<PolymarketPricePoint>>
}