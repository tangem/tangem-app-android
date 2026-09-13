package com.tangem.domain.polymarket.usecase

import arrow.core.Either
import com.tangem.domain.polymarket.PolymarketRepository
import com.tangem.domain.polymarket.model.PredictionOrderQuote
import com.tangem.domain.polymarket.model.PredictionOrderQuoteError
import com.tangem.domain.polymarket.model.PredictionOrderQuoteRequest

/** Preview what a prediction order would cost and fill, without placing anything. */
class GetPredictionOrderQuoteUseCase(private val polymarketRepository: PolymarketRepository) {

    suspend operator fun invoke(
        request: PredictionOrderQuoteRequest,
    ): Either<PredictionOrderQuoteError, PredictionOrderQuote> = polymarketRepository.getOrderQuote(request = request)
}