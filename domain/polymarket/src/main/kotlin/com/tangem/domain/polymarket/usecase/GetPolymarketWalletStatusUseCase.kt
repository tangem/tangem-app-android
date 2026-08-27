package com.tangem.domain.polymarket.usecase

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.left
import arrow.core.right
import com.tangem.domain.polymarket.PolymarketRepository
import com.tangem.domain.polymarket.model.PolymarketAddresses
import com.tangem.domain.polymarket.model.PolymarketOnboardingError
import com.tangem.domain.polymarket.model.PolymarketWalletState

/**
 * Reads the backend's view of the owner's deposit wallet and refuses to continue when that wallet is not
 * the one derived locally: the next steps sign approvals for this address and route funds to it.
 */
class GetPolymarketWalletStatusUseCase(
    private val polymarketRepository: PolymarketRepository,
) {

    suspend operator fun invoke(
        addresses: PolymarketAddresses,
    ): Either<PolymarketOnboardingError, PolymarketWalletState> = polymarketRepository
        .getWalletStatus(ownerAddress = addresses.ownerAddress)
        .mapLeft { it.toOnboardingError() }
        .flatMap { state -> state.ensureDepositWalletMatches(expected = addresses.depositWalletAddress) }

    private fun PolymarketWalletState.ensureDepositWalletMatches(
        expected: String,
    ): Either<PolymarketOnboardingError, PolymarketWalletState> {
        val actual = depositWalletAddress ?: return right()

        return if (actual.equals(expected, ignoreCase = true)) {
            right()
        } else {
            PolymarketOnboardingError.AddressMismatch(expected = expected, actual = actual).left()
        }
    }
}