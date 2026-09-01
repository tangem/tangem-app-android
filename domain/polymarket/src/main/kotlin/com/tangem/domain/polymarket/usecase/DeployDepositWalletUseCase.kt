package com.tangem.domain.polymarket.usecase

import arrow.core.Either
import com.tangem.domain.polymarket.PolymarketRepository
import com.tangem.domain.polymarket.model.PolymarketAddresses
import com.tangem.domain.polymarket.model.PolymarketOnboardingError
import com.tangem.domain.polymarket.model.PolymarketWalletStatus

/**
 * Asks the backend to deploy the deposit wallet, passing the address derived locally so the backend can
 * cross-check it against its own derivation. The returned status is the starting point, not the outcome —
 * deployment completes asynchronously.
 *
 * `walletId` is the Tangem wallet id, sent exactly as stored. It correlates the deposit wallet with the
 * wallet across Tangem's applications and is bound to the owner on first deploy; it is not an input to any
 * derivation, and the backend computes the deposit wallet from the owner alone.
 */
class DeployDepositWalletUseCase(
    private val polymarketRepository: PolymarketRepository,
) {

    suspend operator fun invoke(
        addresses: PolymarketAddresses,
    ): Either<PolymarketOnboardingError, PolymarketWalletStatus> = polymarketRepository
        .deployWallet(
            ownerAddress = addresses.ownerAddress,
            walletId = addresses.userWalletId.stringValue,
            depositWalletAddress = addresses.depositWalletAddress,
        )
        .mapLeft { it.toOnboardingError() }
}