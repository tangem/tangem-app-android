package com.tangem.domain.polymarket.usecase

import arrow.core.Either
import com.tangem.domain.polymarket.PolymarketRepository
import com.tangem.domain.polymarket.model.PredictionOrderSide
import com.tangem.domain.polymarket.model.PredictionPriceError
import java.math.BigDecimal

class GetPredictionPricesUseCase(private val polymarketRepository: PolymarketRepository) {

    suspend operator fun invoke(
        assetIds: List<String>,
        side: PredictionOrderSide,
    ): Either<PredictionPriceError, Map<String, BigDecimal?>> = polymarketRepository.getPrices(assetIds, side)
}