package com.tangem.domain.polymarket.usecase

import arrow.core.Either
import com.tangem.domain.polymarket.PolymarketRepository
import com.tangem.domain.polymarket.model.PredictionOrderSide
import com.tangem.domain.polymarket.model.PredictionPriceError
import java.math.BigDecimal

/** Read the current best price on one side of an asset's book, without quoting an order. */
class GetPredictionPriceUseCase(private val polymarketRepository: PolymarketRepository) {

    suspend operator fun invoke(assetId: String, side: PredictionOrderSide): Either<PredictionPriceError, BigDecimal> =
        polymarketRepository.getPrice(assetId = assetId, side = side)
}