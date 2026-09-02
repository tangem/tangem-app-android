package com.tangem.domain.polymarket.usecase

import arrow.core.Either
import arrow.core.left
import com.tangem.domain.polymarket.PolymarketRepository
import com.tangem.domain.polymarket.model.PolymarketAddresses
import com.tangem.domain.polymarket.model.PredictionPlaceOrderError
import com.tangem.domain.polymarket.model.PredictionPlaceOrderResult
import com.tangem.domain.polymarket.model.PredictionSignedOrder

class PlacePredictionOrderUseCase(
    private val polymarketRepository: PolymarketRepository,
    private val getApiCredentials: GetPolymarketApiCredentialsUseCase,
) {

    suspend operator fun invoke(
        addresses: PolymarketAddresses,
        signedOrder: PredictionSignedOrder,
        isNegRisk: Boolean,
    ): Either<PredictionPlaceOrderError, PredictionPlaceOrderResult> {
        val credentials = getApiCredentials(userWalletId = addresses.userWalletId)
            ?: return PredictionPlaceOrderError.Unauthorized(detail = "No stored CLOB credentials").left()

        return polymarketRepository.placeOrder(
            ownerAddress = addresses.ownerAddress,
            credentials = credentials,
            signedOrder = signedOrder,
            isNegRisk = isNegRisk,
        )
    }
}