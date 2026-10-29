package com.tangem.domain.polymarket.usecase

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.left
import arrow.core.right
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.PolymarketRepository
import com.tangem.domain.polymarket.model.PolymarketAddresses
import com.tangem.domain.polymarket.model.PolymarketOnboardingError
import com.tangem.domain.polymarket.model.PolymarketWalletError
import com.tangem.domain.polymarket.model.PolymarketWalletState
import com.tangem.domain.polymarket.model.PolymarketWalletStatus
import com.tangem.utils.logging.TangemLogger

/**
 * Reads the backend's view of the owner's deposit wallet and refuses to continue when that wallet is not
 * the one derived locally: the next steps sign approvals for this address and route funds to it.
 */
class GetPolymarketWalletStatusUseCase(
    private val polymarketRepository: PolymarketRepository,
) {

    suspend operator fun invoke(
        addresses: PolymarketAddresses,
    ): Either<PolymarketOnboardingError, PolymarketWalletState> = readByWalletIdFallingBackToOwner(addresses)
        .mapLeft { it.toOnboardingError() }
        .flatMap { state -> state.ensureDepositWalletMatches(expected = addresses.depositWalletAddress) }

    suspend fun byWalletId(userWalletId: UserWalletId): Either<PolymarketOnboardingError, PolymarketWalletState> =
        polymarketRepository
            .getWalletStatusByWalletId(walletId = userWalletId.stringValue)
            .mapLeft { it.toOnboardingError() }

    private suspend fun readByWalletIdFallingBackToOwner(
        addresses: PolymarketAddresses,
    ): Either<PolymarketWalletError, PolymarketWalletState> {
        val byWalletId = polymarketRepository.getWalletStatusByWalletId(walletId = addresses.userWalletId.stringValue)
        if (byWalletId.isRight { it.status != PolymarketWalletStatus.NOT_CREATED }) return byWalletId

        val byOwner = polymarketRepository.getWalletStatus(
            userWalletId = addresses.userWalletId,
            ownerAddress = addresses.ownerAddress,
        )
        if (byOwner.isRight { it.status != PolymarketWalletStatus.NOT_CREATED }) {
            TangemLogger.i("Polymarket wallet not read by wallet id, found by owner address")
        }
        return byOwner
    }

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