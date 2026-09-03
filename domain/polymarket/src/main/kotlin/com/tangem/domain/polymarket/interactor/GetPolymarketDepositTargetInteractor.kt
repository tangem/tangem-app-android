package com.tangem.domain.polymarket.interactor

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensureNotNull
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.PolymarketBridgeRepository
import com.tangem.domain.polymarket.model.PolymarketBridgeError
import com.tangem.domain.polymarket.model.PolymarketDepositTarget
import com.tangem.domain.polymarket.usecase.DerivePolymarketAddressesUseCase

class GetPolymarketDepositTargetInteractor(
    private val bridgeRepository: PolymarketBridgeRepository,
    private val deriveAddresses: DerivePolymarketAddressesUseCase,
) {

    suspend operator fun invoke(userWalletId: UserWalletId): Either<PolymarketBridgeError, PolymarketDepositTarget> =
        either {
            val assets = bridgeRepository.getSupportedAssets().bind()

            val asset = ensureNotNull(
                assets.firstOrNull { it.chainId == POLYGON_CHAIN_ID && it.tokenSymbol == COLLATERAL_SYMBOL },
            ) { PolymarketBridgeError.Unavailable }

            val addresses = ensureNotNull(deriveAddresses.stored(userWalletId = userWalletId)) {
                PolymarketBridgeError.Unavailable
            }

            val bridgeAddresses = bridgeRepository.createDepositAddresses(
                walletId = userWalletId.stringValue,
                depositWalletAddress = addresses.depositWalletAddress,
            ).bind()

            PolymarketDepositTarget(addresses = bridgeAddresses, asset = asset)
        }

    private companion object {
        const val POLYGON_CHAIN_ID = 137L
        const val COLLATERAL_SYMBOL = "USDC"
    }
}