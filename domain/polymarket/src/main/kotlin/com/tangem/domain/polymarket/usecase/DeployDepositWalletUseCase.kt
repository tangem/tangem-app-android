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
 */
class DeployDepositWalletUseCase(
    private val polymarketRepository: PolymarketRepository,
) {

    suspend operator fun invoke(
        addresses: PolymarketAddresses,
    ): Either<PolymarketOnboardingError, PolymarketWalletStatus> = polymarketRepository
        .deployWallet(
            ownerAddress = addresses.ownerAddress,
            userWalletId = addresses.userWalletId,
            depositWalletAddress = addresses.depositWalletAddress,
        )
        .mapLeft { it.toOnboardingError() }
}