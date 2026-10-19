package com.tangem.domain.polymarket.usecase

import arrow.core.Either
import arrow.core.left
import com.tangem.domain.polymarket.PolymarketCredentialsLock
import com.tangem.domain.polymarket.PolymarketRepository
import com.tangem.domain.polymarket.model.PolymarketAddresses
import com.tangem.domain.polymarket.model.PolymarketApiCredentials
import com.tangem.domain.polymarket.model.PolymarketClobAsset
import com.tangem.domain.polymarket.model.PolymarketMarketVersion
import com.tangem.domain.polymarket.model.PredictionOrderSide
import com.tangem.domain.polymarket.model.PredictionPlaceOrderError
import com.tangem.domain.polymarket.model.PredictionPlaceOrderResult
import com.tangem.domain.polymarket.model.PredictionSignedOrder
import com.tangem.utils.logging.TangemLogger

/**
 * Sends a signed order to the CLOB.
 *
 * A SELL first refreshes the CLOB's cached balance of the shares it spends, under the asset type of the market's
 * [PolymarketMarketVersion]: the book otherwise answers from a stale cache and refuses the order for a balance
 * the wallet does hold. The refresh is best-effort — the order is sent whether it succeeded or not, and a `401`
 * on it drops nothing; only the order's own `401` proves the credentials dead.
 */
class PlacePredictionOrderUseCase(
    private val polymarketRepository: PolymarketRepository,
    private val getApiCredentials: GetPolymarketApiCredentialsUseCase,
    private val invalidateCredentials: InvalidatePolymarketCredentialsUseCase,
    private val credentialsLock: PolymarketCredentialsLock,
) {

    suspend operator fun invoke(
        addresses: PolymarketAddresses,
        signedOrder: PredictionSignedOrder,
        version: PolymarketMarketVersion,
    ): Either<PredictionPlaceOrderError, PredictionPlaceOrderResult> =
        credentialsLock.withLock(addresses.userWalletId) {
            place(addresses = addresses, signedOrder = signedOrder, version = version)
        }

    private suspend fun place(
        addresses: PolymarketAddresses,
        signedOrder: PredictionSignedOrder,
        version: PolymarketMarketVersion,
    ): Either<PredictionPlaceOrderError, PredictionPlaceOrderResult> {
        val credentials = getApiCredentials(userWalletId = addresses.userWalletId)
            ?: return PredictionPlaceOrderError.Unauthorized(detail = "No stored CLOB credentials").left()

        if (signedOrder.order.side == PredictionOrderSide.SELL) {
            syncSharesBalance(
                addresses = addresses,
                credentials = credentials,
                signedOrder = signedOrder,
                version = version,
            )
        }

        return polymarketRepository.placeOrder(
            ownerAddress = addresses.ownerAddress,
            credentials = credentials,
            signedOrder = signedOrder,
        ).onLeft { error ->
            if (error is PredictionPlaceOrderError.Unauthorized) {
                invalidateCredentials(userWalletId = addresses.userWalletId)
            }
        }
    }

    private suspend fun syncSharesBalance(
        addresses: PolymarketAddresses,
        credentials: PolymarketApiCredentials,
        signedOrder: PredictionSignedOrder,
        version: PolymarketMarketVersion,
    ) {
        polymarketRepository.syncBalanceAllowance(
            ownerAddress = addresses.ownerAddress,
            credentials = credentials,
            asset = PolymarketClobAsset.Shares(tokenId = signedOrder.order.tokenId.toString(), version = version),
        ).onLeft { error ->
            TangemLogger.e("Polymarket shares balance sync before SELL failed, sending the order anyway: $error")
        }
    }
}