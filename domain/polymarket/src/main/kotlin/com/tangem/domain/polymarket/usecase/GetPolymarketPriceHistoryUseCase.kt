package com.tangem.domain.polymarket.usecase

import arrow.core.Either
import arrow.core.left
import com.tangem.domain.polymarket.PolymarketPriceHistoryRepository
import com.tangem.domain.polymarket.model.PolymarketEvent
import com.tangem.domain.polymarket.model.PolymarketHistoryToken
import com.tangem.domain.polymarket.model.PolymarketPriceHistoryError
import com.tangem.domain.polymarket.model.PolymarketPriceHistoryQuery
import com.tangem.domain.polymarket.model.PolymarketPricePoint
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

class GetPolymarketPriceHistoryUseCase(private val repository: PolymarketPriceHistoryRepository) {

    suspend operator fun invoke(
        event: PolymarketEvent,
        tokens: Set<PolymarketHistoryToken>,
        query: PolymarketPriceHistoryQuery,
    ): Map<PolymarketHistoryToken, Either<PolymarketPriceHistoryError, List<PolymarketPricePoint>>> = coroutineScope {
        tokens.map { token ->
            async {
                val isEventToken = event.markets.any { market ->
                    market.eventId == event.id && market.id == token.marketId && market.version == token.version &&
                        market.outcomes.any { it.assetId == token.assetId }
                }
                token to if (token.assetId.isNotBlank() && isEventToken) {
                    repository.getPriceHistory(token.assetId, query)
                } else {
                    PolymarketPriceHistoryError.InvalidToken.left()
                }
            }
        }.awaitAll().toMap()
    }
}