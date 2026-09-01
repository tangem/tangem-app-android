package com.tangem.domain.polymarket.interactor

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensureNotNull
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.PolymarketBridgeRepository
import com.tangem.domain.polymarket.model.PolymarketBridgeError
import com.tangem.domain.polymarket.model.PolymarketDepositTarget
import com.tangem.domain.polymarket.usecase.DerivePolymarketAddressesUseCase

/**
 * Resolves where [userWalletId] sends funds to top up its Polymarket balance.
 *
 * The asset is checked against what the bridge accepts before an address is issued: a combination the bridge
 * did not list can lose the funds irrecoverably, and the minimum has to come from the same answer rather than
 * from a table this app carries.
 *
 * The deposit wallet is read from what this device already derived, so opening the screen asks the user for
 * nothing. A wallet onboarded elsewhere reports [PolymarketBridgeError.Unavailable] instead of prompting for
 * the card.
 */
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

            val bridgeAddress = bridgeRepository.createDepositAddress(
                walletId = userWalletId.stringValue,
                depositWalletAddress = addresses.depositWalletAddress,
            ).bind()

            PolymarketDepositTarget(bridgeAddress = bridgeAddress, asset = asset)
        }

    private companion object {
        const val POLYGON_CHAIN_ID = 137L
        const val COLLATERAL_SYMBOL = "USDC"
    }
}