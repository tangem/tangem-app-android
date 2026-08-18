package com.tangem.domain.polymarket.interactor

import arrow.core.Either
import arrow.core.left
import com.tangem.domain.polymarket.PolymarketRepository
import com.tangem.domain.polymarket.model.PolymarketAddresses
import com.tangem.domain.polymarket.model.PolymarketAuthError
import com.tangem.domain.polymarket.model.PolymarketBalanceAllowance
import com.tangem.domain.polymarket.usecase.GetPolymarketApiCredentialsUseCase

/**
 * Reads the CLOB's collateral balance and allowance of [addresses]'s deposit wallet.
 *
 * The credentials come from the local store, so a wallet that never finished onboarding fails with
 * [PolymarketAuthError.KeyNotFound] without reaching the network.
 *
 * The read is preceded by a refresh, because the CLOB serves a cached view that it only recomputes when
 * asked: without this, a wallet funded after onboarding would report the balance it had at onboarding — zero
 * — for as long as it exists. The refresh is best-effort, so a failure to recompute falls back to reporting
 * the cached figure rather than turning a stale number into an error.
 */
class GetPolymarketBalanceInteractor(
    private val polymarketRepository: PolymarketRepository,
    private val getApiCredentials: GetPolymarketApiCredentialsUseCase,
) {

    suspend operator fun invoke(
        addresses: PolymarketAddresses,
    ): Either<PolymarketAuthError, PolymarketBalanceAllowance> {
        val credentials = getApiCredentials(userWalletId = addresses.userWalletId)
            ?: return PolymarketAuthError.KeyNotFound.left()

        polymarketRepository.syncBalanceAllowance(
            ownerAddress = addresses.ownerAddress,
            credentials = credentials,
        )

        return polymarketRepository.getBalanceAllowance(
            ownerAddress = addresses.ownerAddress,
            credentials = credentials,
        )
    }
}